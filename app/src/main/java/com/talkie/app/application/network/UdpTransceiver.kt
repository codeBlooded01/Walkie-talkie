package com.talkie.app.application.network

import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * UdpTransceiver
 *
 * Sends and receives small DatagramPackets over the local P2P IP address.
 * UDP is preferred for audio due to lower latency, accepting minor packet loss.
 *
 * Fix (reuseAddress): DatagramSocket(port) binds immediately on construction,
 * making any subsequent reuseAddress = true a no-op. We now use the no-arg
 * constructor, set reuseAddress first, then manually bind — so reconnects
 * after disconnect never hit "Address already in use".
 */
class UdpTransceiver(private val listenPort: Int = 9999) {
    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null

    var packetsSent = 0
        private set
    var packetsReceived = 0
        private set

    fun getTargetIp(): String = targetAddress?.hostAddress ?: "None"

    @Volatile
    var isRunning = false
        private set

    fun startListening(
        onPacketReceived: (ByteArray) -> Unit,
        onPacketActivity: (() -> Unit)? = null,
        onTargetDiscovered: ((String) -> Unit)? = null,
        onSocketReady: (() -> Unit)? = null
    ) {
        if (isRunning) return
        
        Thread {
            Log.d("TalkieAudio", "Transceiver thread started. Target port: $listenPort")
            try {
                // Correct order: no-arg constructor → set reuseAddress → bind
                val s = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    // Use this@UdpTransceiver.listenPort to avoid shadowing by DatagramSocket.port
                    bind(InetSocketAddress(this@UdpTransceiver.listenPort))
                }
                socket = s
                isRunning = true 
                
                Log.d("TalkieAudio", "UdpTransceiver bound to port $listenPort. Starting loop.")
                onSocketReady?.invoke()
                
                val buffer = ByteArray(1024)
                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    
                    if (targetAddress == null) {
                        targetAddress = packet.address
                        val ip = packet.address.hostAddress ?: "Unknown"
                        Log.d("TalkieAudio", "Auto-discovered target IP: $ip")
                        onTargetDiscovered?.invoke(ip)
                    }
                    
                    onPacketActivity?.invoke()
                    val data = packet.data.copyOf(packet.length)
                    packetsReceived++
                    // Diagnostic log for reception
                    if (packetsReceived % 50 == 0) {
                        Log.d("TalkieAudio", "RX check: Received $packetsReceived packets total. Last from: ${packet.address.hostAddress}")
                    }
                    onPacketReceived(data)
                }
            } catch (e: Exception) {
                Log.e("TalkieAudio", "FATAL SOCKET ERROR: ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
            } finally {
                Log.d("TalkieAudio", "Transceiver thread exiting. Cleaning up.")
                isRunning = false
                socket?.close()
                socket = null
            }
        }.start()
    }

    fun setTargetAddress(ipAddress: String) {
        targetAddress = InetAddress.getByName(ipAddress)
    }

    suspend fun sendPacket(data: ByteArray) = withContext(Dispatchers.IO) {
        sendPacketBlocking(data)
    }

    fun sendPacketBlocking(data: ByteArray) {
        val target = targetAddress
        if (target == null) return
        
        val s = socket
        if (s == null || s.isClosed) return
        
        try {
            val packet = DatagramPacket(data, data.size, target, listenPort)
            s.send(packet)
            packetsSent++
        } catch (e: Exception) {
            Log.e("TalkieAudio", "Error sending packet", e)
        }
    }

    fun stop() {
        isRunning = false
        socket?.close()
        socket = null
        targetAddress = null
    }
}
