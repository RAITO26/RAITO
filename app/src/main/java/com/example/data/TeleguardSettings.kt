package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teleguard_settings")
data class TeleguardSettings(
    @PrimaryKey val id: Int = 0,
    val botToken: String = "",
    val chatId: String = "",
    val isDarkTheme: Boolean = true,
    val languageCode: String = "en", // "fa" (Persian), "en" (English), "ru" (Russian), "zh" (Chinese)
    val isBotRunning: Boolean = false
)
