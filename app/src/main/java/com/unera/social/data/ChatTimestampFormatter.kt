package com.unera.social.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * Formatter for chat and chatlist timestamps.
 * Strictly adheres to: "in chat and chatlist do not show date instead show just now, 1hour and time"
 */
object ChatTimestampFormatter {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val isoFormat1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoFormat3 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val isoFormat4 = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun format(raw: String?): String {
        if (raw.isNullOrBlank()) return "Just now"
        val trimmed = raw.trim()

        if (trimmed.equals("Just now", ignoreCase = true) || trimmed.equals("1hour", ignoreCase = true)) {
            return trimmed
        }
        if (trimmed.matches(Regex("^[0-1]?[0-9]:[0-5][0-9]\\s*(AM|PM|am|pm)$"))) {
            return trimmed
        }

        val millis = trimmed.toLongOrNull()
        val date: Date? = if (millis != null && millis > 100000000000L) {
            Date(millis)
        } else if (millis != null && millis > 1000000000L) {
            Date(millis * 1000L)
        } else {
            tryParseDate(trimmed)
        }

        if (date == null) {
            val timeMatch = Regex("([0-2]?[0-9]:[0-5][0-9]\\s*(?:AM|PM|am|pm)?)").find(trimmed)
            if (timeMatch != null) {
                return timeMatch.value
            }
            return "Just now"
        }

        val now = System.currentTimeMillis()
        val diffMs = now - date.time

        if (diffMs < 0) {
            return "Just now"
        }

        val diffMinutes = diffMs / (60 * 1000)
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m"
            diffMinutes < 120 -> "1hour"
            else -> timeFormat.format(date) // Strictly formatted as time (e.g. "3:45 PM"), never date
        }
    }

    private fun tryParseDate(str: String): Date? {
        return try {
            isoFormat1.parse(str)
        } catch (_: Exception) {
            try {
                isoFormat2.parse(str)
            } catch (_: Exception) {
                try {
                    isoFormat3.parse(str)
                } catch (_: Exception) {
                    try {
                        isoFormat4.parse(str)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }
}
