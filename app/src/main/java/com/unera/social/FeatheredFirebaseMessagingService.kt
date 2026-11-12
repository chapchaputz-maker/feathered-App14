package com.unera.social

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unera.social.data.FcmTokenManager
import com.unera.social.data.UserSessionManager
import java.util.concurrent.atomic.AtomicInteger

class FeatheredFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FeatheredFCMService"
        private const val SUMMARY_NOTIFICATION_ID = 1001
        private val notificationIdSequence = AtomicInteger(2000)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken received: $token")

        val storedUserId = UserSessionManager.getUserId(applicationContext)
            .takeIf { it > 0L }
            ?: UserSessionManager.getSavedUser(applicationContext)?.id
            ?: UserSessionManager.getCurrentUser()?.id
            ?: 0L

        if (storedUserId > 0L) {
            FcmTokenManager.registerFcmTokenInBackground(
                currentUserId = storedUserId.toString(),
                tokenOverride = token
            )
        } else {
            Log.d(TAG, "No logged-in user ID found on token refresh; will register on login/startup.")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data

        val type = data["type"].orEmpty()
        val groupKey = data["groupKey"]?.takeIf { it.isNotBlank() }
            ?: data["group_key"]?.takeIf { it.isNotBlank() }
            ?: type.takeIf { it.isNotBlank() }?.let { "feathered_$it" }
            ?: "feathered_social_group"
        val entityType = data["entityType"] ?: data["entity_type"] ?: ""
        val entityId = data["entityId"] ?: data["entity_id"] ?: ""
        val parentId = data["parentId"] ?: data["parent_id"] ?: ""
        val actorId = data["actorId"] ?: data["actor_id"] ?: ""

        val title = data["title"]?.takeIf { it.isNotBlank() }
            ?: remoteMessage.notification?.title?.takeIf { it.isNotBlank() }
            ?: buildDefaultTitle(type)

        val body = data["body"]?.takeIf { it.isNotBlank() }
            ?: data["message"]?.takeIf { it.isNotBlank() }
            ?: remoteMessage.notification?.body?.takeIf { it.isNotBlank() }
            ?: buildDefaultBody(type, entityType)

        Log.d(
            TAG,
            "FCM message received: type=$type, groupKey=$groupKey, entityType=$entityType, entityId=$entityId, parentId=$parentId, actorId=$actorId"
        )

        showGroupedNotification(
            title = title,
            body = body,
            type = type,
            groupKey = groupKey,
            entityType = entityType,
            entityId = entityId,
            parentId = parentId,
            actorId = actorId
        )
    }

    private fun showGroupedNotification(
        title: String,
        body: String,
        type: String,
        groupKey: String,
        entityType: String,
        entityId: String,
        parentId: String,
        actorId: String
    ) {
        FcmTokenManager.createNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping notification display")
                return
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fcm_type", type)
            putExtra("fcm_groupKey", groupKey)
            putExtra("fcm_entityType", entityType)
            putExtra("fcm_entityId", entityId)
            putExtra("fcm_parentId", parentId)
            putExtra("fcm_actorId", actorId)
        }

        var notificationId = buildIndividualNotificationId(entityType, entityId)
        if (notificationId == SUMMARY_NOTIFICATION_ID) {
            notificationId += 1
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryPendingIntent = PendingIntent.getActivity(
            this,
            SUMMARY_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, FcmTokenManager.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_feather_logo)
            .setColor(0xFF1877F2.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(groupKey)

        val summaryNotification = NotificationCompat.Builder(this, FcmTokenManager.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_feather_logo)
            .setColor(0xFF1877F2.toInt())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(body)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(body)
                    .setBigContentTitle(getString(R.string.app_name))
                    .setSummaryText(formatGroupSummaryLabel(type))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(summaryPendingIntent)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(this)
            manager.notify(notificationId, notificationBuilder.build())
            manager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        } catch (se: SecurityException) {
            Log.e(TAG, "Missing notification permission when posting notification", se)
        }
    }

    private fun buildIndividualNotificationId(entityType: String, entityId: String): Int {
        val raw = if (entityType.isNotBlank() || entityId.isNotBlank()) {
            ("${entityType}_${entityId}_${notificationIdSequence.incrementAndGet()}").hashCode()
        } else {
            notificationIdSequence.incrementAndGet()
        }
        return (raw and 0x7FFFFFFF).coerceAtLeast(2000)
    }

    private fun buildDefaultTitle(type: String): String {
        return when (type.lowercase()) {
            "like", "reaction" -> "New Reaction"
            "comment", "reply" -> "New Comment"
            "follow", "friend_request" -> "New Follower"
            "message", "chat" -> "New Message"
            "group_invite", "group" -> "Group Update"
            else -> getString(R.string.app_name)
        }
    }

    private fun buildDefaultBody(type: String, entityType: String): String {
        return when (type.lowercase()) {
            "like", "reaction" -> "Someone reacted to your ${entityType.ifBlank { "post" }}."
            "comment", "reply" -> "Someone commented on your ${entityType.ifBlank { "post" }}."
            "follow" -> "Someone started following you."
            "message", "chat" -> "You received a new message."
            "group_invite" -> "You were invited to join a group."
            else -> "You have a new notification on Feathered Social."
        }
    }

    private fun formatGroupSummaryLabel(type: String): String {
        return when (type.lowercase()) {
            "message", "chat" -> "Messages"
            "comment", "reply" -> "Comments"
            "like", "reaction" -> "Reactions"
            "follow" -> "Followers"
            else -> "Feathered Social updates"
        }
    }
}
