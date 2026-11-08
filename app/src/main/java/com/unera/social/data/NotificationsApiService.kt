package com.unera.social.data

import com.unera.social.model.Group
import com.unera.social.model.Notification
import com.unera.social.model.NotificationType
import com.unera.social.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * NotificationsApiService
 *
 * Dedicated live network service for notifications matching Feathered web:
 * - GET /api/notifications (Fetch all notifications, polling every 20s, load more)
 * - PATCH /api/notifications (Mark all as read)
 * - PATCH /api/notifications?id={id} (Mark single notification as read)
 * - DELETE /api/notifications?id={id} (Remove notification)
 * - PUT /api/group-invites?id={inviteId}&group_id={groupId} (Accept / decline group invites)
 */
object NotificationsApiService {

    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) Feathered/1.0"

    /**
     * GET /api/notifications
     * Headers:
     *   x-user-id: <userId>
     *   Authorization: Bearer <token> (if available)
     */
    suspend fun fetchNotifications(
        userId: Long,
        token: String? = null,
        knownUsers: List<User> = emptyList(),
        knownGroups: List<Group> = emptyList()
    ): List<Notification> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Notification>()
        if (userId <= 0L) return@withContext emptyList()

        try {
            val url = URL("$BASE_URL/api/notifications")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                conn.disconnect()

                val usersMap = knownUsers.associateBy { it.id }
                val groupsMap = knownGroups.associateBy { it.id }

                if (responseText.startsWith("[")) {
                    val arr = JSONArray(responseText)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseNotificationJson(obj, usersMap, groupsMap)?.let { result.add(it) }
                    }
                } else if (responseText.startsWith("{")) {
                    val root = JSONObject(responseText)
                    val arr = root.optJSONArray("notifications") ?: root.optJSONArray("data")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            parseNotificationJson(obj, usersMap, groupsMap)?.let { result.add(it) }
                        }
                    }
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        result
    }

    /**
     * PATCH /api/notifications
     * Marks all notifications as read for current user.
     */
    suspend fun markAllAsRead(userId: Long, token: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (userId <= 0L) return@withContext false
        try {
            val url = URL("$BASE_URL/api/notifications")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val code = conn.responseCode
            val isSuccess = code in 200..299
            conn.disconnect()
            return@withContext isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Mark a single notification as read:
     * Sends PATCH /api/notifications?id={notificationId} (and fallback POST /api/notifications/{id}/read)
     */
    suspend fun markOneAsRead(notificationId: String, userId: Long, token: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (notificationId.isBlank() || userId <= 0L) return@withContext false
        try {
            // 1. Primary: PATCH /api/notifications?id=<id>
            val url = URL("$BASE_URL/api/notifications?id=$notificationId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val payload = JSONObject().apply {
                put("id", notificationId.toLongOrNull() ?: notificationId)
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                return@withContext true
            }

            // Fallback: POST /api/notifications/{id}/read
            try {
                val postUrl = URL("$BASE_URL/api/notifications/$notificationId/read")
                val postConn = (postUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("x-user-id", userId.toString())
                    if (!token.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                val postCode = postConn.responseCode
                postConn.disconnect()
                return@withContext postCode in 200..299
            } catch (_: Exception) {
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * DELETE /api/notifications?id={notificationId}
     */
    suspend fun deleteNotification(notificationId: String, userId: Long, token: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (notificationId.isBlank() || userId <= 0L) return@withContext false
        try {
            val url = URL("$BASE_URL/api/notifications?id=$notificationId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val code = conn.responseCode
            conn.disconnect()
            return@withContext code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * PUT /api/group-invites?id={inviteId}&group_id={groupId}
     * Body: { status: "accepted" | "declined", user_id: <userId>, group_id: <groupId> }
     */
    suspend fun respondGroupInvite(
        inviteId: Long,
        groupId: Long,
        userId: Long,
        status: String,
        token: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val urlStr = buildString {
                append("$BASE_URL/api/group-invites?id=$inviteId")
                if (groupId > 0) append("&group_id=$groupId")
            }
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("x-user-id", userId.toString())
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val payload = JSONObject().apply {
                put("status", status)
                put("user_id", userId)
                if (groupId > 0) put("group_id", groupId)
                if (inviteId > 0) put("id", inviteId)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                return@withContext true
            }

            // Fallback for group join if groupId is set and status == "accepted"
            if (groupId > 0 && status == "accepted") {
                try {
                    val joinUrl = URL("$BASE_URL/api/groups/$groupId/join")
                    val joinConn = (joinUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 6000
                        readTimeout = 6000
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("x-user-id", userId.toString())
                        if (!token.isNullOrBlank()) {
                            setRequestProperty("Authorization", "Bearer $token")
                        }
                    }
                    val joinPayload = JSONObject().apply {
                        put("user_id", userId)
                        put("group_id", groupId)
                    }
                    OutputStreamWriter(joinConn.outputStream, "UTF-8").use {
                        it.write(joinPayload.toString())
                        it.flush()
                    }
                    val joinCode = joinConn.responseCode
                    joinConn.disconnect()
                    return@withContext joinCode in 200..299
                } catch (_: Exception) {}
            }

            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // ==========================================
    // PARSER
    // ==========================================

    private fun parseNotificationJson(
        obj: JSONObject,
        usersMap: Map<Long, User>,
        groupsMap: Map<Long, Group>
    ): Notification? {
        val idVal = obj.opt("id") ?: return null
        val notifId = idVal.toString()
        if (notifId.isBlank()) return null

        val actorId = obj.optLong("actor_id", obj.optLong("recipient_id", 0L))
        val rawType = obj.optString("type", "like").trim().lowercase(Locale.ROOT)
        val entityType = obj.optString("entity_type", obj.optString("target_type", "")).trim().lowercase(Locale.ROOT)

        val rawEntityId = obj.opt("entity_id")?.toString()?.toLongOrNull()
            ?: obj.opt("target_id")?.toString()?.toLongOrNull()
            ?: obj.optLong("entity_id", 0L).takeIf { it > 0 }

        val rawGroupId = obj.opt("group_id")?.toString()?.toLongOrNull()
            ?: if (entityType == "group" || entityType == "group_post" || entityType == "group_invite") rawEntityId else null

        val rawInviteId = obj.opt("invite_id")?.toString()?.toLongOrNull()
            ?: obj.opt("parent_id")?.toString()?.toLongOrNull()

        val isRead = obj.optInt("is_read", 0) == 1 || obj.optBoolean("is_read", false)
        val createdAtStr = obj.optString("created_at", "")
        val timestamp = parseCreatedAtToMillis(createdAtStr)

        // Resolve or construct Actor User
        val existingUser = if (actorId > 0) usersMap[actorId] else null
        val actorName = obj.optString("actor_name").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("name").takeIf { it.isNotBlank() && it != "null" }
            ?: existingUser?.name
            ?: if (actorId > 0) "User $actorId" else "Feathered Member"

        val actorUsername = obj.optString("username").takeIf { it.isNotBlank() && it != "null" }
            ?: existingUser?.username
            ?: "@${actorName.lowercase().replace(" ", "")}"

        val actorAvatar = obj.optString("actor_image").takeIf { it.isNotBlank() && it != "null" }
            ?: obj.optString("profile_image_url").takeIf { it.isNotBlank() && it != "null" }
            ?: existingUser?.profileImageUrl
            ?: ""

        val user = User(
            id = if (actorId > 0) actorId else existingUser?.id ?: 0L,
            name = actorName,
            username = actorUsername,
            profileImageUrl = actorAvatar,
            isVerified = existingUser?.isVerified ?: (obj.optInt("is_verified", 0) == 1 || obj.optBoolean("is_verified", false))
        )

        // Map notification type enum
        val mappedType = when (rawType) {
            "discuss", "comment" -> NotificationType.COMMENT
            "reply" -> NotificationType.COMMENT
            "react", "reaction" -> NotificationType.REACTION
            "follow" -> NotificationType.FOLLOW
            "share" -> NotificationType.SHARE
            "group_invite", "group_request", "invite" -> NotificationType.GROUP_INVITE
            "event", "going", "interested" -> NotificationType.EVENT
            "birthday" -> NotificationType.BIRTHDAY
            "song", "music" -> NotificationType.MUSIC
            "message" -> NotificationType.MESSAGE
            else -> NotificationType.LIKE
        }

        // Detect reaction type if applicable
        var reactionType = obj.optString("reaction_type", "").trim().lowercase(Locale.ROOT)
        val rawMessage = obj.optString("message", "").trim().let { if (it.equals("null", ignoreCase = true)) "" else it }

        if (reactionType.isBlank()) {
            val lowerMsg = rawMessage.lowercase(Locale.ROOT)
            reactionType = when {
                lowerMsg.contains("love") || lowerMsg.contains("heart") -> "love"
                lowerMsg.contains("haha") || lowerMsg.contains("laugh") -> "haha"
                lowerMsg.contains("wow") -> "wow"
                lowerMsg.contains("sad") -> "sad"
                lowerMsg.contains("angry") -> "angry"
                lowerMsg.contains("fire") -> "fire"
                lowerMsg.contains("party") -> "party"
                lowerMsg.contains("clap") -> "clap"
                lowerMsg.contains("like") -> "like"
                mappedType == NotificationType.REACTION -> "like"
                else -> ""
            }
        }

        // Construct descriptive content if message is blank
        val cleanContent = if (rawMessage.isNotBlank()) {
            rawMessage
        } else {
            val entityLabel = when (entityType) {
                "group_post" -> "group post"
                "group_invite" -> "group invitation"
                "event" -> "event"
                "song" -> "song"
                "product" -> "product"
                "story" -> "story"
                "reel" -> "reel"
                "profile" -> "profile"
                else -> "post"
            }
            when (rawType) {
                "discuss", "comment" -> "commented on your $entityLabel"
                "reply" -> "replied to your comment"
                "react", "reaction" -> {
                    when (reactionType) {
                        "love" -> "loved your $entityLabel"
                        "haha" -> "laughed at your $entityLabel"
                        "wow" -> "was amazed by your $entityLabel"
                        "sad" -> "reacted sadly to your $entityLabel"
                        "angry" -> "reacted angrily to your $entityLabel"
                        "fire" -> "found your $entityLabel fire"
                        else -> "reacted to your $entityLabel"
                    }
                }
                "follow" -> "followed you"
                "share" -> "shared your $entityLabel"
                "group_invite", "invite" -> "invited you to join a group"
                "group_request" -> "requested to join your group"
                "going" -> "is going to your event"
                "interested" -> "is interested in your event"
                "birthday" -> "has a birthday today"
                else -> "interacted with your $entityLabel"
            }
        }

        // Resolve group name if available
        val groupName = obj.optString("group_name").takeIf { it.isNotBlank() && it != "null" }
            ?: (if (rawGroupId != null) groupsMap[rawGroupId]?.name else null)

        val previewText = obj.optString("preview_text").takeIf { it.isNotBlank() && it != "null" }
        val previewImage = obj.optString("preview_image").takeIf { it.isNotBlank() && it != "null" }

        // Multi-actors parsing
        val actorsCount = obj.optInt("actors_count", 1)
        val extraActors = mutableListOf<User>()
        val rawActorsJson = obj.opt("actors_json")
        if (rawActorsJson != null) {
            val str = rawActorsJson.toString().trim()
            if (str.startsWith("[")) {
                try {
                    val arr = JSONArray(str)
                    for (i in 0 until arr.length()) {
                        val item = arr.opt(i)
                        if (item is Number) {
                            val uid = item.toLong()
                            val u = usersMap[uid]
                            if (u != null) extraActors.add(u)
                        } else if (item is JSONObject) {
                            val uid = item.optLong("id")
                            val u = usersMap[uid] ?: User(
                                id = uid,
                                name = item.optString("name", "User $uid"),
                                username = item.optString("username", "@user$uid"),
                                profileImageUrl = item.optString("profile_image_url", "")
                            )
                            extraActors.add(u)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return Notification(
            id = notifId,
            user = user,
            content = cleanContent,
            timeAgo = formatRelativeTime(timestamp),
            type = mappedType,
            isRead = isRead,
            rawType = rawType,
            reactionType = reactionType,
            entityType = entityType,
            entityId = rawEntityId,
            groupId = rawGroupId,
            groupName = groupName,
            inviteId = rawInviteId,
            previewText = previewText,
            previewImage = previewImage,
            actorsCount = maxOf(actorsCount, 1 + extraActors.size),
            actors = extraActors,
            timestamp = timestamp
        )
    }

    private fun parseCreatedAtToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        )
        for (sdf in formats) {
            try {
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return dateStr.toLongOrNull() ?: System.currentTimeMillis()
    }

    private fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0) return "Just now"
        val diff = System.currentTimeMillis() - timestamp
        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "Just now"
            diff < hour -> "${maxOf(1L, diff / minute)}m ago"
            diff < day -> "${diff / hour}h ago"
            diff < 7 * day -> "${diff / day}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
}
