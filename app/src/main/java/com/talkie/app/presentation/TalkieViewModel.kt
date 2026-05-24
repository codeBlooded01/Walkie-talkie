package com.talkie.app.presentation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.talkie.app.BuildConfig
import com.talkie.app.application.service.TalkieService
import com.talkie.app.data.LocalSessionPreferences
import com.talkie.app.domain.ConnectionState
import com.talkie.app.domain.Contact
import com.talkie.app.domain.PeerDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * TalkieViewModel
 *
 * Client to TalkieService. Also manages the full Contact list,
 * user profile, and all UI state for the Home / PTT / Profile screens.
 *
 * Exposes:
 * - [incomingAmplitude]  normalised RMS [0f,1f] of incoming audio
 * - [isReceiving]        debounced flag — remote peer is speaking
 * - [connectedPeer]      identity of the currently connected peer
 */
class TalkieViewModel(application: Application) : AndroidViewModel(application) {

    // ─────────────────────────────────────────────────────────────────────────
    // Persisted preferences
    // ─────────────────────────────────────────────────────────────────────────

    val sessionPreferences = LocalSessionPreferences(application)

    private val wifiManager =
        application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // ─────────────────────────────────────────────────────────────────────────
    // UI-only state
    // ─────────────────────────────────────────────────────────────────────────

    private val _localIpAddress = MutableStateFlow(getLocalIp())
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    private val _deviceName = MutableStateFlow(sessionPreferences.deviceName)
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _devicePhotoUri = MutableStateFlow(sessionPreferences.devicePhotoUri)
    val devicePhotoUri: StateFlow<String?> = _devicePhotoUri.asStateFlow()

    private val _deviceBio = MutableStateFlow(sessionPreferences.deviceBio)
    val deviceBio: StateFlow<String> = _deviceBio.asStateFlow()

    private val _debugInfo = MutableStateFlow("Debug: Binding to service…")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Contact list
    // ─────────────────────────────────────────────────────────────────────────

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Mirrored state from Service
    // ─────────────────────────────────────────────────────────────────────────

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedPeer = MutableStateFlow<PeerDevice?>(null)
    val connectedPeer: StateFlow<PeerDevice?> = _connectedPeer.asStateFlow()

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    /** Debounced flag: remote peer is actively sending voice. */
    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    /** Normalised incoming audio amplitude [0f, 1f]. */
    private val _incomingAmplitude = MutableStateFlow(0f)
    val incomingAmplitude: StateFlow<Float> = _incomingAmplitude.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Service binding
    // ─────────────────────────────────────────────────────────────────────────

    private var talkieService: TalkieService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? TalkieService.LocalBinder ?: return
            talkieService = localBinder.getService()
            Log.d(TAG, "Bound to TalkieService.")
            startObservingServiceFlows()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            talkieService = null
            Log.w(TAG, "TalkieService unexpectedly disconnected.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────────

    init {
        Log.d(TAG, "TalkieViewModel initializing.")
        _contacts.value = sessionPreferences.loadContacts()

        TalkieService.start(application)
        val intent = Intent(application, TalkieService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            while (true) {
                _localIpAddress.value = getLocalIp()
                delay(3_000)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service flow observation
    // ─────────────────────────────────────────────────────────────────────────

    private fun startObservingServiceFlows() {
        val service = talkieService ?: return
        viewModelScope.launch { service.connectionState.collect { _connectionState.value = it } }
        viewModelScope.launch { service.connectedPeer.collect { _connectedPeer.value = it } }
        viewModelScope.launch { service.isTransmitting.collect { _isTransmitting.value = it } }
        viewModelScope.launch { service.isReceiving.collect { _isReceiving.value = it } }
        viewModelScope.launch { service.incomingAmplitude.collect { _incomingAmplitude.value = it } }
        if (BuildConfig.DEBUG) {
            viewModelScope.launch { service.debugInfo.collect { _debugInfo.value = it } }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public commands — PTT / network
    // ─────────────────────────────────────────────────────────────────────────

    fun connectToIp(targetIp: String, name: String = "LAN Peer") {
        talkieService?.connectToIp(targetIp, name)
            ?: Log.w(TAG, "connectToIp: service not yet bound.")
    }

    fun disconnect() {
        talkieService?.disconnect()
            ?: Log.w(TAG, "disconnect: service not yet bound.")
    }

    fun startTransmitting() {
        talkieService?.startTransmitting()
            ?: Log.w(TAG, "startTransmitting: service not yet bound.")
    }

    fun stopTransmitting() {
        talkieService?.stopTransmitting()
            ?: Log.w(TAG, "stopTransmitting: service not yet bound.")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public commands — user profile
    // ─────────────────────────────────────────────────────────────────────────

    fun updateDeviceName(name: String) {
        sessionPreferences.deviceName = name
        _deviceName.value = name
    }

    fun updateDevicePhoto(uri: String?) {
        sessionPreferences.devicePhotoUri = uri
        _devicePhotoUri.value = uri
    }

    fun updateDeviceBio(bio: String) {
        sessionPreferences.deviceBio = bio
        _deviceBio.value = bio
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public commands — contact management
    // ─────────────────────────────────────────────────────────────────────────

    fun addContact(name: String, ipAddress: String, photoUri: String?) {
        val updated = _contacts.value + Contact(
            id = UUID.randomUUID().toString(),
            name = name,
            ipAddress = ipAddress,
            photoUri = photoUri,
            isPaired = true
        )
        persist(updated)
    }

    fun updateContact(id: String, name: String, ipAddress: String, photoUri: String?) {
        val updated = _contacts.value.map { c ->
            if (c.id == id) c.copy(name = name, ipAddress = ipAddress,
                photoUri = photoUri ?: c.photoUri)
            else c
        }
        persist(updated)
    }

    fun deleteContact(id: String) {
        persist(_contacts.value.filter { it.id != id })
    }

    private fun persist(list: List<Contact>) {
        _contacts.value = list
        sessionPreferences.saveContacts(list)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "unbindService: ${e.message}")
        }
        talkieService = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun getLocalIp(): String {
        val ip = wifiManager.dhcpInfo?.ipAddress ?: 0
        if (ip == 0) return "Not connected to Wi-Fi"
        return String.format("%d.%d.%d.%d",
            ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }

    companion object {
        private const val TAG = "TalkieViewModel"
    }
}
