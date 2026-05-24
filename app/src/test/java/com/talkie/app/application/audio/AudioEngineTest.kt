package com.talkie.app.application.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioEngineTest {

    @Test
    fun `sample rate must be 44100 Hz per spec`() {
        assertEquals("Sample rate must be 44100 Hz", 44100, AudioEngine.SAMPLE_RATE)
    }

    @Test
    fun `packet size must be 1024 bytes per spec`() {
        assertEquals("Packet size must be 1024 bytes", 1024, AudioEngine.PACKET_SIZE)
    }

    @Test
    fun `audio format must be PCM 16-bit`() {
        assertEquals(
            "Audio format must be PCM 16-bit",
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            AudioEngine.AUDIO_FORMAT
        )
    }

    @Test
    fun `channel in must be Mono`() {
        assertEquals(
            "Input channel must be MONO",
            android.media.AudioFormat.CHANNEL_IN_MONO,
            AudioEngine.CHANNEL_CONFIG_IN
        )
    }

    @Test
    fun `channel out must be Mono`() {
        assertEquals(
            "Output channel must be MONO",
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            AudioEngine.CHANNEL_CONFIG_OUT
        )
    }

    @Test
    fun `stopRecording is idempotent when called before startRecording`() {
        // Should not throw even if called when nothing is recording
        val engine = AudioEngine()
        engine.stopRecording()
        engine.stopRecording()
    }

    @Test
    fun `release is idempotent when nothing is active`() {
        val engine = AudioEngine()
        engine.release()
        engine.release()
    }
}
