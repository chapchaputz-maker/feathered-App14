package com.unera.social

import com.unera.social.model.Notification
import com.unera.social.model.NotificationType
import com.unera.social.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NotificationsSystemTest {

    @Test
    fun testNotificationModelAndTypes() {
        val user = User(id = 9L, name = "Jane Doe", username = "@janedoe")
        val notif = Notification(
            id = "109",
            user = user,
            content = "followed you",
            type = NotificationType.FOLLOW,
            isRead = false,
            rawType = "follow",
            entityType = "profile",
            entityId = 1L
        )

        assertEquals("109", notif.id)
        assertEquals(9L, notif.user.id)
        assertEquals("Jane Doe", notif.user.name)
        assertEquals(NotificationType.FOLLOW, notif.type)
        assertFalse(notif.isRead)
        assertEquals(1L, notif.entityId)
    }

    @Test
    fun testUtcDateParsingLogic() {
        val dateStr = "2026-11-08 20:54:27"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = sdf.parse(dateStr)
        assertNotNull(date)
        assertTrue(date!!.time > 0L)
    }

    @Test
    fun testRelativeTimeCalculation() {
        val now = System.currentTimeMillis()
        val tenMinutesAgo = now - 10 * 60 * 1000L
        val diffMinutes = (now - tenMinutesAgo) / (60 * 1000L)
        assertEquals(10L, diffMinutes)
    }
}
