package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TeleguardSettings::class, ActivityLog::class], version = 1, exportSchema = false)
abstract class TeleguardDatabase : RoomDatabase() {
    abstract val teleguardDao: TeleguardDao

    companion object {
        @Volatile
        private var INSTANCE: TeleguardDatabase? = null

        fun getInstance(context: Context): TeleguardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeleguardDatabase::class.java,
                    "teleguard_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
