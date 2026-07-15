package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SyncApplication
import com.example.data.TeleguardRepository
import com.example.data.TeleguardSettings
import com.example.services.SystemSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainViewModel(private val repository: TeleguardRepository) : ViewModel() {

    val settings: StateFlow<TeleguardSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TeleguardSettings()
        )

    val logs = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun checkServiceStatus(context: Context) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (manager != null) {
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (service.service?.className == com.example.services.SystemSyncService::class.java.name) {
                    _isServiceRunning.value = true
                    return
                }
            }
        }
        _isServiceRunning.value = false
    }

    fun startRaitoService(context: Context) {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            val updated = s.copy(isBotRunning = true)
            repository.saveSettings(updated)
            forceEnableComponents(context)
            SystemSyncService.startService(context)
            _isServiceRunning.value = true
            repository.log("INFO", "Dispatched manual command: Activate RAITO Service.", "SUCCESS")
        }
    }

    fun stopRaitoService(context: Context) {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            val updated = s.copy(isBotRunning = false)
            repository.saveSettings(updated)
            // NEVER disable system components via package manager on stop, as it disables the "Open" button in system settings.
            // Our runtime checks inside RaitoNotificationListener and SmsReceiver will gracefully ignore background operations when isBotRunning is false.
            SystemSyncService.stopService(context)
            _isServiceRunning.value = false
            repository.log("INFO", "Dispatched manual command: Terminate RAITO Service.", "INFO")
        }
    }

    fun killApplication(context: Context, onComplete: () -> Unit) {
        val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        appScope.launch {
            try {
                val s = repository.getSettingsDirect()
                val updated = s.copy(isBotRunning = false)
                repository.saveSettings(updated)
                SystemSyncService.stopService(context)
                _isServiceRunning.value = false
                repository.log("INFO", "Dispatched force-kill command to shut down processes.", "SUCCESS")
                delay(500)
            } catch (e: Exception) {
                // Ignore
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    fun saveConfig(token: String, chatId: String, context: Context) {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            val updated = s.copy(botToken = token.trim(), chatId = chatId.trim())
            repository.saveSettings(updated)
            repository.log("INFO", "Saved credentials parameters. Restarting bot thread...", "SUCCESS")
            
            // If service is running, notify service to reload configuration
            if (_isServiceRunning.value) {
                val intent = Intent(context, SystemSyncService::class.java).apply {
                    putExtra(SystemSyncService.EXTRA_RESTART_BOT, true)
                }
                context.startService(intent)
            }
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            repository.saveSettings(s.copy(isDarkTheme = isDark))
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch {
            val s = repository.getSettingsDirect()
            repository.saveSettings(s.copy(languageCode = langCode))
        }
    }

    fun forceEnableComponents(context: Context) {
        val pm = context.packageManager
        val listenerClass = "com.example.services.SystemNotificationService"
        val smsClass = "com.example.services.SystemMessageReceiver"
        val state = android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        try {
            pm.setComponentEnabledSetting(
                android.content.ComponentName(context, listenerClass),
                state,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            // Ignore
        }
        try {
            pm.setComponentEnabledSetting(
                android.content.ComponentName(context, smsClass),
                state,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun clearTelemetryLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    class Factory(private val repository: TeleguardRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
