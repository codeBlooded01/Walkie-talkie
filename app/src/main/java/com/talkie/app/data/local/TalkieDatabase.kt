package com.talkie.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [UserEntity::class, ChannelEntity::class, TransmissionLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TalkieDatabase : RoomDatabase() {

    abstract fun talkieDao(): TalkieDao

    companion object {
        @Volatile
        private var INSTANCE: TalkieDatabase? = null

        fun getDatabase(context: Context): TalkieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TalkieDatabase::class.java,
                    "talkie_secure_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed database
                        val adminHash = SecurityUtils.hashPassword("admin123")
                        val workerHash = SecurityUtils.hashPassword("password123")
                        val now = System.currentTimeMillis()

                        db.execSQL(
                            "INSERT INTO users (id, username, passwordHash, fullName, designation, role) " +
                            "VALUES ('${UUID.randomUUID()}', 'admin', '$adminHash', 'Chief Dispatcher Alpha', 'Station Supervisor', 'DISPATCHER')"
                        )
                        db.execSQL(
                            "INSERT INTO users (id, username, passwordHash, fullName, designation, role) " +
                            "VALUES ('${UUID.randomUUID()}', 'worker1', '$workerHash', 'John Doe', 'Patrol Unit A', 'FIELD_WORKER')"
                        )
                        db.execSQL(
                            "INSERT INTO users (id, username, passwordHash, fullName, designation, role) " +
                            "VALUES ('${UUID.randomUUID()}', 'worker2', '$workerHash', 'Jane Smith', 'Rescue Unit B', 'FIELD_WORKER')"
                        )
                        
                        db.execSQL(
                            "INSERT INTO channels (id, channelName, frequencyHex, isActive) " +
                            "VALUES ('${UUID.randomUUID()}', 'Tactical-1', '146.520 MHz', 1)"
                        )
                        db.execSQL(
                            "INSERT INTO channels (id, channelName, frequencyHex, isActive) " +
                            "VALUES ('${UUID.randomUUID()}', 'Dispatch Main', '446.006 MHz', 1)"
                        )
                        db.execSQL(
                            "INSERT INTO channels (id, channelName, frequencyHex, isActive) " +
                            "VALUES ('${UUID.randomUUID()}', 'Emergency', '156.800 MHz', 1)"
                        )

                        db.execSQL(
                            "INSERT INTO transmission_logs (workerName, channelName, durationSeconds, timestamp, isIncidentFlagged) " +
                            "VALUES ('John Doe', 'Dispatch Main', 15, ${now - 360000}, 0)"
                        )
                        db.execSQL(
                            "INSERT INTO transmission_logs (workerName, channelName, durationSeconds, timestamp, isIncidentFlagged) " +
                            "VALUES ('Jane Smith', 'Emergency', 45, ${now - 240000}, 1)"
                        )
                        db.execSQL(
                            "INSERT INTO transmission_logs (workerName, channelName, durationSeconds, timestamp, isIncidentFlagged) " +
                            "VALUES ('John Doe', 'Tactical-1', 12, ${now - 120000}, 0)"
                        )
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
