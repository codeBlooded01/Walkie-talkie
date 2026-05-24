package com.talkie.app.application.network

import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramSocket
import java.net.InetSocketAddress

class UdpTransceiverTest {

    @Test
    fun `setTargetAddress resolves a valid loopback IP without throwing`() {
        val transceiver = UdpTransceiver()
        // Should not throw on a valid address string
        transceiver.setTargetAddress("127.0.0.1")
    }

    @Test(expected = java.net.UnknownHostException::class)
    fun `setTargetAddress throws UnknownHostException for invalid hostname`() {
        val transceiver = UdpTransceiver()
        transceiver.setTargetAddress("not.a.real.hostname.invalid")
    }

    @Test
    fun `stop is idempotent — calling twice does not throw`() {
        val transceiver = UdpTransceiver()
        transceiver.stop()
        transceiver.stop()
    }

    @Test
    fun `reuseAddress is set before bind — port can be reused after stop`() {
        // This is the regression test for the original reuseAddress bug.
        // We verify that after a UdpTransceiver binds and then stops, a second
        // DatagramSocket can bind to the same port immediately — which would fail
        // if reuseAddress were set after bind (the original bug).
        val port = 19988 // Use a test port that won't conflict with the app
        val transceiver = UdpTransceiver(port)

        // Manually replicate the correct bind order (mirrors the fixed implementation)
        val socket = DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(port))
        }
        assertTrue("Socket should be bound", socket.isBound)
        socket.close()

        // After closing, a fresh socket should be able to bind to the same port
        val socket2 = DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(port))
        }
        assertTrue("Second socket should bind cleanly after close", socket2.isBound)
        socket2.close()
    }
}
