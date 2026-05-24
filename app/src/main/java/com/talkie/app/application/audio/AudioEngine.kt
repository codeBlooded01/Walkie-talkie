package com.talkie.app.application.audio

import android.util.Log
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * AudioEngine
 *
 * Responsible for recording audio input and converting it to PCM bytes,
 * as well as taking received PCM bytes and playing them back.
 *
 * Specs: 44100 Hz, 16-bit PCM, Mono, 1024 bytes per packet.
 *
 * Voice Activity Detection:
 * - Computes RMS amplitude of every incoming PCM packet (16-bit samples).
 * - Normalized to [0f, 1f] against a ceiling of 8000 (typical speech peak).
 * - Caller receives amplitude via onAmplitude callback for UI visualisation.
 */
class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val PACKET_SIZE = 1024

        // RMS ceiling for normalisation — typical conversational speech peaks here
        private const val RMS_CEILING = 6000f
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private val ioScope = CoroutineScope(Dispatchers.IO)

    @Volatile var isRecording = false
        private set
    @Volatile private var isPlaying = false

    // ── Amplitude callback (called from receive-path) ──────────────────────────
    private var amplitudeCallback: ((Float) -> Unit)? = null

    /** Register a listener for incoming audio amplitude [0f, 1f]. */
    fun setAmplitudeListener(listener: ((Float) -> Unit)?) {
        amplitudeCallback = listener
    }

    // ── RMS helper ─────────────────────────────────────────────────────────────
    private fun computeRmsNormalized(pcm: ByteArray): Float {
        if (pcm.size < 2) return 0f
        var sumSq = 0.0
        var count = 0
        var i = 0
        while (i + 1 < pcm.size) {
            // Little-endian 16-bit sample
            val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
            sumSq += sample.toDouble() * sample.toDouble()
            count++
            i += 2
        }
        val rms = if (count > 0) sqrt(sumSq / count).toFloat() else 0f
        return (rms / RMS_CEILING).coerceIn(0f, 1f)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onAudioReady: (ByteArray) -> Unit) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, PACKET_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG_IN,
            AUDIO_FORMAT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val buffer = ByteArray(PACKET_SIZE)
            Log.d("TalkieAudio", "AudioRecord thread started")
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    onAudioReady(buffer.copyOf(read))
                } else if (read < 0) {
                    Log.e("TalkieAudio", "AudioRecord read error: $read")
                }
            }
            Log.d("TalkieAudio", "AudioRecord thread stopped")
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun startPlayback() {
        if (isPlaying) return

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, PACKET_SIZE * 4)

        @Suppress("DEPRECATION")
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG_OUT,
            AUDIO_FORMAT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()
        isPlaying = true
    }

    /**
     * Play received PCM data and compute amplitude for voice-activity detection.
     * The amplitude is forwarded to any registered [amplitudeCallback].
     */
    fun playAudioBytes(data: ByteArray) {
        if (!isPlaying) startPlayback()
        audioTrack?.write(data, 0, data.size)

        // Measure amplitude on IO thread — never blocks UI
        val amp = computeRmsNormalized(data)
        amplitudeCallback?.invoke(amp)
    }

    fun stopPlayback() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        // Signal silence
        amplitudeCallback?.invoke(0f)
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
