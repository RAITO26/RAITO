package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TeleguardDao {
    @Query("SELECT * FROM teleguard_settings WHERE id = 0")
    fun getSettings(): Flow<TeleguardSettings?>

    @Query("SELECT * FROM teleguard_settings WHERE id = 0")
    suspend fun getSettingsDirect(): TeleguardSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: TeleguardSettings)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
