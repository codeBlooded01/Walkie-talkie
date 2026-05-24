package com.talkie.app.application.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import android.widget.RemoteViews
import android.view.View
import android.view.WindowManager
import android.view.Gravity
import android.view.LayoutInflater
import android.graphics.PixelFormat
import android.provider.Settings
import android.widget.TextView
import com.talkie.app.R
import androidx.core.app.ServiceCompat
import com.talkie.app.application.audio.AudioEngine
import com.talkie.app.application.network.UdpTransceiver
import com.talkie.app.data.local.TalkieDatabase
import com.talkie.app.data.local.TransmissionLogEntity
import com.talkie.app.domain.ConnectionState
import com.talkie.app.domain.PeerDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * TalkieService
 *
 * The single source of truth for all networking and audio in Talkie.
 * Running as a Foreground Service ensures Android keeps the process alive
 * even when the UI is gone. WakeLock + WifiLock prevent the CPU and Wi-Fi
 * chip from sleeping during an active session.
 *
 * Voice Activity Detection:
 * - AudioEngine exposes normalised RMS amplitude [0f,1f] on every incoming PCM packet.
 * - We debounce "voice end" by 300 ms so brief pauses don't flicker the UI.
 * - A ONE-SHOT notification fires when a speaking session starts; it is not
 *   repeated until the speaker goes silent and speaks again.
 */
class TalkieService : Service() {

    // ─────────────────────────────────────────────────────────────────────────
    // Binder
    // ─────────────────────────────────────────────────────────────────────────

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TalkieService = this@TalkieService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ─────────────────────────────────────────────────────────────────────────
    // Core engine objects
    // ─────────────────────────────────────────────────────────────────────────

    private val udpTransceiver = UdpTransceiver()
    private val audioEngine = AudioEngine()
    private val sessionPrefs by lazy { com.talkie.app.data.LocalSessionPreferences(applicationContext) }
    private val talkieDao by lazy { TalkieDatabase.getDatabase(applicationContext).talkieDao() }
    private var overlayView: View? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Coroutine scope
    // ─────────────────────────────────────────────────────────────────────────

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // ─────────────────────────────────────────────────────────────────────────
    // Power management
    // ─────────────────────────────────────────────────────────────────────────

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Exposed state — observed by TalkieViewModel
    // ─────────────────────────────────────────────────────────────────────────

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedPeer = MutableStateFlow<PeerDevice?>(null)
    val connectedPeer: StateFlow<PeerDevice?> = _connectedPeer.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    /** True when the REMOTE peer is actively sending packets (debounced 300 ms). */
    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    /** Normalised amplitude of the INCOMING audio stream [0f, 1f]. */
    private val _incomingAmplitude = MutableStateFlow(0f)
    val incomingAmplitude: StateFlow<Float> = _incomingAmplitude.asStateFlow()

    private val _debugInfo = MutableStateFlow("Service: Initializing…")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Voice-activity internal state
    // ─────────────────────────────────────────────────────────────────────────

    /** Timestamp of the last received packet (ms). */
    @Volatile private var lastPacketTime = 0L

    /** Timestamp of when transmission started. */
    @Volatile private var transmitStartTimeMillis = 0L

    /**
     * Whether we have already fired the "is speaking" notification for the
     * current speaking session. Resets to false when the peer goes silent.
     */
    @Volatile private var speakingNotificationSent = false

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        startNetworkServices()
        startVoiceActivityMonitor()
        Log.d(TAG, "Service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved — Foreground+START_STICKY keeps us alive.")
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.setAmplitudeListener(null)
        stopNetworkServices()
        releaseLocks()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed.")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun connectToIp(targetIp: String, name: String = "LAN Peer") {
        if (targetIp.isBlank()) return
        udpTransceiver.stop()
        udpTransceiver.setTargetAddress(targetIp)
        startNetworkServices()
        _connectedPeer.value = PeerDevice(deviceName = name, deviceAddress = targetIp)
        _connectionState.value = ConnectionState.CONNECTED
        postForegroundNotification()
        Log.d(TAG, "Connected to $targetIp ($name)")
    }

    fun disconnect() {
        stopNetworkServices()
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedPeer.value = null
        _isReceiving.value = false
        _incomingAmplitude.value = 0f
        speakingNotificationSent = false
        startNetworkServices()
        postForegroundNotification()
        Log.d(TAG, "Disconnected.")
    }

    fun startTransmitting() {
        if (!udpTransceiver.isRunning) startNetworkServices()
        _isTransmitting.value = true
        transmitStartTimeMillis = System.currentTimeMillis()
        postForegroundNotification()
        audioEngine.startRecording { data -> udpTransceiver.sendPacketBlocking(data) }
        Log.d(TAG, "PTT started.")
    }

    fun stopTransmitting() {
        _isTransmitting.value = false
        val duration = (System.currentTimeMillis() - transmitStartTimeMillis) / 1000L
        audioEngine.stopRecording()
        postForegroundNotification()
        Log.d(TAG, "PTT stopped.")
        
        // Log the transmission
        val workerName = sessionPrefs.loggedInUsername.orEmpty().ifEmpty { sessionPrefs.deviceName }
        val channelName = "Dispatch Main"
        serviceScope.launch {
            try {
                talkieDao.insertTransmissionLog(TransmissionLogEntity(
                    workerName = workerName,
                    channelName = channelName,
                    durationSeconds = duration.toInt(),
                    timestamp = transmitStartTimeMillis
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log transmission", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal network management
    // ─────────────────────────────────────────────────────────────────────────

    private fun startNetworkServices() {
        if (udpTransceiver.isRunning) return

        // Wire amplitude listener before starting playback
        audioEngine.setAmplitudeListener { amp ->
            _incomingAmplitude.value = amp
            Log.d(TAG, "AudioEvent: onVoiceLevel(level=$amp)")
        }

        audioEngine.startPlayback()

        udpTransceiver.startListening(
            onSocketReady = {
                udpTransceiver.sendPacketBlocking(byteArrayOf(0x00))
            },
            onPacketReceived = { data ->
                val peer = _connectedPeer.value
                val ip = peer?.deviceAddress ?: udpTransceiver.getTargetIp()
                val contact = sessionPrefs.loadContacts().firstOrNull { it.ipAddress == ip }
                val name = contact?.name ?: if (peer?.deviceName != null && peer.deviceName != "LAN Peer") peer.deviceName else "Nicole"
                Log.d(TAG, "AudioEvent: onTransmissionReceived(user=$name, streamSize=${data.size})")
                audioEngine.playAudioBytes(data)
            },
            onPacketActivity = {
                lastPacketTime = System.currentTimeMillis()
                if (!_isReceiving.value) {
                    _isReceiving.value = true
                    val peer = _connectedPeer.value
                    val ip = peer?.deviceAddress ?: udpTransceiver.getTargetIp()
                    val contact = sessionPrefs.loadContacts().firstOrNull { it.ipAddress == ip }
                    val name = contact?.name ?: if (peer?.deviceName != null && peer.deviceName != "LAN Peer") peer.deviceName else "Nicole"
                    
                    // Propagate resolved peer identity instantly to viewmodel and ui
                    _connectedPeer.value = PeerDevice(deviceName = name, deviceAddress = ip)

                    Log.d(TAG, "AudioEvent: onVoiceStart(user=$name)")
                    // One-shot notification per speaking session
                    if (!speakingNotificationSent) {
                        speakingNotificationSent = true
                        showOverlay(name)
                        postForegroundNotification()
                    }
                }
            },
            onTargetDiscovered = { ip ->
                if (udpTransceiver.getTargetIp() == "None") {
                    udpTransceiver.setTargetAddress(ip)
                    val contact = sessionPrefs.loadContacts().firstOrNull { it.ipAddress == ip }
                    val name = contact?.name ?: "Nicole"
                    _connectedPeer.value = PeerDevice(deviceName = name, deviceAddress = ip)
                    _connectionState.value = ConnectionState.CONNECTED
                    postForegroundNotification()
                    Log.d(TAG, "Auto-discovered peer at $ip ($name)")
                }
            }
        )
    }

    private fun stopNetworkServices() {
        audioEngine.setAmplitudeListener(null)
        udpTransceiver.stop()
        audioEngine.stopPlayback()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Voice-activity debounce monitor (300 ms silence → voice-end)
    // ─────────────────────────────────────────────────────────────────────────

    private fun startVoiceActivityMonitor() {
        serviceScope.launch {
            while (true) {
                delay(50) // check every 50 ms
                val silentMs = System.currentTimeMillis() - lastPacketTime
                if (silentMs > VOICE_END_DEBOUNCE_MS && _isReceiving.value) {
                    val peer = _connectedPeer.value
                    val ip = peer?.deviceAddress ?: udpTransceiver.getTargetIp()
                    val contact = sessionPrefs.loadContacts().firstOrNull { it.ipAddress == ip }
                    val name = contact?.name ?: if (peer?.deviceName != null && peer.deviceName != "LAN Peer") peer.deviceName else "Nicole"
                    Log.d(TAG, "AudioEvent: onVoiceEnd(user=$name)")
                    dismissOverlay()
                    _isReceiving.value = false
                    _incomingAmplitude.value = 0f
                    speakingNotificationSent = false // allow next session to notify
                    postForegroundNotification()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Power management
    // ─────────────────────────────────────────────────────────────────────────

    private fun acquireLocks() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:TalkieWakeLock")
            .also { if (!it.isHeld) it.acquire() }

        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION") WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION") WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wm.createWifiLock(mode, "$packageName:TalkieWifiLock")
            .also { if (!it.isHeld) it.acquire() }
    }

    private fun releaseLocks() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
        wakeLock = null; wifiLock = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notifications
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Low-importance channel for the persistent foreground notification
            val low = NotificationChannel(CHANNEL_ID, "Talkie Active Session", NotificationManager.IMPORTANCE_LOW)
            // High-importance channel for the one-shot "is speaking" heads-up
            val high = NotificationChannel(CHANNEL_VOICE_ID, "Talkie Voice Activity", NotificationManager.IMPORTANCE_HIGH)
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(low)
            mgr.createNotificationChannel(high)
        }
    }

    private fun buildNotification(title: String = "Talkie is Active", text: String = "Listening for incoming audio…"): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun postForegroundNotification() {
        val peer = _connectedPeer.value
        val (title, text) = when {
            _isTransmitting.value && peer != null ->
                "Transmitting to ${peer.deviceName}" to "Your message is being sent to ${peer.deviceName}."
            _isReceiving.value && peer != null ->
                "Receiving from ${peer.deviceName}" to "${peer.deviceName} is currently speaking…"
            peer != null ->
                "Connected to ${peer.deviceName}" to "Standby — tap PTT to speak."
            else -> "Talkie is Active" to "Listening for incoming audio…"
        }
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    private fun showOverlay(speakerName: String) {
        serviceScope.launch(Dispatchers.Main) {
            if (overlayView != null) return@launch
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@TalkieService)) {
                Log.d(TAG, "Overlay permission not granted, skipping floating window")
                return@launch
            }

            try {
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val inflater = LayoutInflater.from(this@TalkieService)
                val view = inflater.inflate(R.layout.notification_heads_up, null)
                
                view.findViewById<TextView>(R.id.notification_title).text = speakerName
                view.findViewById<TextView>(R.id.notification_subtitle).text = "is speaking..."
                
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP
                    x = 0
                    y = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 100 else 60
                }

                params.windowAnimations = android.R.style.Animation_Toast

                windowManager.addView(view, params)
                overlayView = view
                Log.d(TAG, "Floating overlay displayed for $speakerName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to display floating overlay", e)
            }
        }
    }

    private fun dismissOverlay() {
        serviceScope.launch(Dispatchers.Main) {
            val view = overlayView ?: return@launch
            try {
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(view)
                overlayView = null
                Log.d(TAG, "Floating overlay dismissed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss floating overlay", e)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "TalkieService"
        private const val CHANNEL_ID = "talkie_service_channel"
        private const val CHANNEL_VOICE_ID = "talkie_voice_channel"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_VOICE_ID = 2

        /** How long silence must last before we declare the speaking session over (ms). */
        private const val VOICE_END_DEBOUNCE_MS = 300L

        fun start(context: Context) {
            val intent = Intent(context, TalkieService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stop(context: Context) = context.stopService(Intent(context, TalkieService::class.java))
    }
}
