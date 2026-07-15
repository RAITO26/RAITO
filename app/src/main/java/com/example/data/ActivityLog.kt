package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "INFO", "ALERT", "NOTIF_FORWARD", "SMS_FORWARD", "BOT_COMMAND", "BOT_STATUS"
    val message: String,
    val status: String // "SUCCESS", "FAILED2", "INFO"
)
