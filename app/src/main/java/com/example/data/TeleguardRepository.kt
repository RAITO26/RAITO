package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TeleguardRepository(private val dao: TeleguardDao) {
    val settings: Flow<TeleguardSettings> = dao.getSettings().map { 
        it ?: TeleguardSettings()
    }

    suspend fun getSettingsDirect(): TeleguardSettings {
        return dao.getSettingsDirect() ?: TeleguardSettings()
    }

    suspend fun saveSettings(settings: TeleguardSettings) {
        dao.insertSettings(settings)
    }

    val allLogs: Flow<List<ActivityLog>> = dao.getAllLogs()

    suspend fun log(type: String, message: String, status: String = "INFO") {
        dao.insertLog(ActivityLog(type = type, message = message, status = status))
    }

    suspend fun clearLogs() {
        dao.clearLogs()
    }
}
