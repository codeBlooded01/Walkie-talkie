package com.talkie.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TalkieDao {

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Channel queries
    @Query("SELECT * FROM channels WHERE isActive = 1")
    fun getActiveChannelsFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels")
    suspend fun getAllChannels(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    // Transmission Log queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransmissionLog(log: TransmissionLogEntity)

    @Query("SELECT * FROM transmission_logs ORDER BY timestamp DESC")
    fun getAllTransmissionLogsFlow(): Flow<List<TransmissionLogEntity>>

    @Query("SELECT * FROM transmission_logs ORDER BY timestamp DESC")
    suspend fun getAllTransmissionLogs(): List<TransmissionLogEntity>

    // Aggregated Metrics for Dispatcher
    @Query("SELECT COUNT(*) FROM transmission_logs")
    fun getTotalTransmissionsFlow(): Flow<Int>

    @Query("SELECT SUM(durationSeconds) FROM transmission_logs")
    fun getTotalAirtimeFlow(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM transmission_logs WHERE isIncidentFlagged = 1")
    fun getTotalFlaggedIncidentsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'FIELD_WORKER'")
    fun getRegisteredWorkersCountFlow(): Flow<Int>
}
