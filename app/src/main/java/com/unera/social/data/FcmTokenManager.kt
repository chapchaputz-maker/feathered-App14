package com.unera.social.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private const val DEVICES_ENDPOINT = "https://featheredsocial.site/api/devices"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) Feathered/1.0"

    const val NOTIFICATION_CHANNEL_ID = "feathered_social"
    private const val NOTIFICATION_CHANNEL_NAME = "Feathered Social"
    private const val NOTIFICATION_CHANNEL_DESCRIPTION = "Likes, comments, follows, messages, and group updates"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Ensures the high-importance notification channel "feathered_social" exists on Android 8.0+.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = NOTIFICATION_CHANNEL_DESCRIPTION
                        enableLights(true)
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(channel)
                    Log.d(TAG, "Created notification channel: $NOTIFICATION_CHANNEL_ID")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
            }
        }
    }

    /**
     * Awaits FirebaseMessaging.getInstance().token without blocking or crashing on failure.
     */
    suspend fun awaitFcmToken(): String? = suspendCancellableCoroutine { continuation ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        val token = task.result
                        continuation.resume(token?.takeIf { it.isNotBlank() })
                    } else {
                        Log.e(TAG, "Failed to fetch FCM token", task.exception)
                        continuation.resume(null)
                    }
                }
        } catch (t: Throwable) {
            Log.e(TAG, "Exception requesting FCM token", t)
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    /**
     * Registers the device's FCM token with the Feathered Social backend.
     * Additive and silent: logs errors without throwing or blocking user flows.
     */
    suspend fun registerFcmTokenWithBackend(
        currentUserId: String,
        tokenOverride: String? = null
    ) = withContext(Dispatchers.IO) {
        val cleanUserId = currentUserId.trim()
        val numericId = cleanUserId.toLongOrNull() ?: 0L
        if (cleanUserId.isBlank() || numericId <= 0L) {
            Log.d(TAG, "Skipping FCM token registration: no authenticated user ID")
            return@withContext
        }

        try {
            val fcmToken = tokenOverride?.takeIf { it.isNotBlank() } ?: awaitFcmToken()
            if (fcmToken.isNullOrBlank()) {
                Log.w(TAG, "Skipping FCM backend registration: FCM token is unavailable")
                return@withContext
            }

            val payload = JSONObject().apply {
                put("token", fcmToken)
                put("platform", "android")
            }.toString()

            val url = URL(DEVICES_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", cleanUserId)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val statusCode = conn.responseCode
            val responseBody = try {
                val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
                if (stream != null) BufferedReader(InputStreamReader(stream)).use { it.readText() } else ""
            } catch (_: Exception) {
                ""
            }
            conn.disconnect()

            if (statusCode in 200..299) {
                Log.i(TAG, "FCM token registered with backend for user $cleanUserId (HTTP $statusCode)")
            } else {
                Log.e(TAG, "Failed to register FCM token for user $cleanUserId (HTTP $statusCode): $responseBody")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error registering FCM token with backend for user $cleanUserId", t)
        }
    }

    /**
     * Fire-and-forget helper for non-suspend contexts (e.g., FirebaseMessagingService.onNewToken).
     */
    fun registerFcmTokenInBackground(currentUserId: String, tokenOverride: String? = null) {
        backgroundScope.launch {
            registerFcmTokenWithBackend(currentUserId, tokenOverride)
        }
    }
}
