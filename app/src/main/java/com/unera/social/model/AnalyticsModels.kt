package com.unera.social.model

data class DailyViewsPoint(
    val date: String,             // "2026-10-14"
    val label: String,            // "Oct 14"
    val views: Long
)

data class CountryAnalytics(
    val code: String,             // "TZ"
    val name: String,             // "Tanzania"
    val flag: String,             // "🇹🇿"
    val views: Long,
    val percentage: Float         // e.g. 70.0
)

data class GenderAnalytics(
    val gender: String,           // "Male", "Female", "Other", "Unknown"
    val views: Long,
    val percentage: Float         // e.g. 60.0
)

data class GlobalAnalytics(
    val totalViews: Long = 0L,
    val previousPeriodViews: Long = 0L,
    val growthPercentage: Float = 0f,
    val periodLabel: String = "Oct 12 - Nov 8",
    val dailyViews: List<DailyViewsPoint> = emptyList(),
    val topCountries: List<CountryAnalytics> = emptyList(),
    val genderBreakdown: List<GenderAnalytics> = emptyList(),
    val followersPercentage: Float = 37.0f,
    val nonFollowersPercentage: Float = 63.0f,
    val totalPostsTracked: Int = 0
)

enum class AnalyticsTimePeriod(val id: String, val label: String, val days: Int) {
    LAST_7_DAYS("7d", "Last 7 days", 7),
    LAST_28_DAYS("28d", "Last 28 days", 28),
    LAST_90_DAYS("90d", "Last 90 days", 90),
    ALL_TIME("all", "All time", 365)
}
