package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.example.SyncApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SystemMessageReceiver : BroadcastReceiver() {

    private val client = OkHttpClient()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            val repository = try {
                val app = context.applicationContext as? SyncApplication
                app?.repository ?: run {
                    val db = com.example.data.TeleguardDatabase.getInstance(context.applicationContext)
                    com.example.data.TeleguardRepository(db.teleguardDao)
                }
            } catch (e: Exception) {
                val db = com.example.data.TeleguardDatabase.getInstance(context.applicationContext)
                com.example.data.TeleguardRepository(db.teleguardDao)
            }

            try {
                @Suppress("DEPRECATION")
                val pdus = bundle.get("pdus") as? Array<*> ?: return
                val format = bundle.getString("format")
                val messages = ArrayList<SmsMessage>()

                for (pdu in pdus) {
                    val smsRaw = pdu as ByteArray
                    val sms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(smsRaw, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(smsRaw)
                    }
                    if (sms != null) {
                        messages.add(sms)
                    }
                }

                if (messages.isEmpty()) return

                val sender = messages[0].displayOriginatingAddress ?: "Unknown Sender"
                val bodyBuilder = java.lang.StringBuilder()
                for (sms in messages) {
                    bodyBuilder.append(sms.displayMessageBody)
                }
                val fullBody = bodyBuilder.toString()

                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.settings.first()
                    if (!settings.isBotRunning) {
                        return@launch
                    }
                    val token = settings.botToken
                    val chat = settings.chatId

                    if (token.isEmpty() || chat.isEmpty()) {
                        return@launch
                    }

                    val escapedSender = escapeHtml(sender)
                    val escapedBody = escapeHtml(fullBody)
                    val formattedMsg = "💬 <b>RAITO SMS Alert</b>\n\n" +
                            "• <b>Sender:</b> <code>$escapedSender</code>\n" +
                            "• <b>Content:</b> $escapedBody"

                    val success = postTelegramMessage(token, chat, formattedMsg)
                    if (success) {
                        repository.log("SMS_FORWARD", "Forwarded SMS from $sender", "SUCCESS")
                    } else {
                        repository.log("SMS_FORWARD", "Failed to forward SMS from $sender (API error)", "FAILED")
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.log("SMS_FORWARD", "Exception reading incoming SMS: ${e.message}", "FAILED")
                }
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
