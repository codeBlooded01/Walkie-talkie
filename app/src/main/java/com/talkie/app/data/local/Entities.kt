package com.talkie.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val designation: String,
    val role: String // "FIELD_WORKER", "DISPATCHER"
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val channelName: String,
    val frequencyHex: String,
    val isActive: Boolean = true
)

@Entity(tableName = "transmission_logs")
data class TransmissionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerName: String,
    val channelName: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val isIncidentFlagged: Boolean = durationSeconds > 30
)
