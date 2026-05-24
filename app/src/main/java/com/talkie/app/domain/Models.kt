package com.talkie.app.domain

import kotlinx.serialization.Serializable

/**
 * Enum defining the current connection state of the P2P network link.
 */
enum class ConnectionState {
    DISCONNECTED,
    SEARCHING,
    CONNECTING,
    CONNECTED
}

/**
 * Represents a nearby peer discovered via Wi-Fi Direct.
 */
data class PeerDevice(
    val deviceName   : String,
    val deviceAddress: String,
    val isGroupOwner : Boolean = false,
    val photoUri     : String? = null
)

/**
 * A saved contact in the user's contact list.
 * Serializable so it can be persisted as JSON in SharedPreferences.
 */
@Serializable
data class Contact(
    val id       : String,           // UUID string
    val name     : String,
    val ipAddress: String,
    val photoUri : String? = null,   // local file URI as a string
    val bio      : String  = "\"Truth is not just seen or heard—it is felt in the silence between questions.\"",
    val isPaired : Boolean = true    // true = Paired Device, false = Available Device
)
