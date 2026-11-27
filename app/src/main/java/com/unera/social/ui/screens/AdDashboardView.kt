package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.AdCampaign
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import java.text.NumberFormat
import java.util.Locale

/**
 * AdDashboardView - Matches web Dashboard.tsx for Ad Analytics exactly.
 * Includes:
 * - Header: "Ad Analytics", "Track your campaign performance and analytics."
 * - Stats Grid (4 cards): Total Impressions, Total Clicks, Total Views, Total Spend ($0 free or calculated)
 *   with trend indicators (up/down %, vs last week)
 * - Second Row Stats: Click-Through Rate (CTR) gauge with progress bar (Industry avg: 1.5-2.5%) & Active Campaigns count card
 * - Weekly Performance Chart: Interactive bar chart for Mon-Sun showing impressions and clicks with time range dropdown
 * - Top Performing Campaigns:
 *   - Mobile view: Dedicated cards with prominent CTR badge (emerald pill), thumbnail, status badge, 3-stat row (Impressions, Views, Clicks)
 *   - Wide/Tablet view: Horizontal row layout with all metrics and status pill
 */
@Composable
fun AdDashboardView(
    campaigns: List<AdCampaign>,
    onBack: (() -> Unit)? = null,
    onNavigateToCampaigns: (() -> Unit)? = null,
    isEmbedded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTimeRange by remember { mutableStateOf("Last 7 days") }
    var showTimeDropdown by remember { mutableStateOf(false) }

    // Aggregate statistics
    val totalImpressions = remember(campaigns) {
        campaigns.sumOf { it.impressions.toLong() }
    }
    val totalClicks = remember(campaigns) {
        campaigns.sumOf { it.clicks.toLong() }
    }
    val totalViews = remember(campaigns) {
        campaigns.sumOf { it.views.toLong() }
    }
    val totalSpend = remember(campaigns) {
        // Free promo or sum of budgets
        0L
    }

    val activeCampaigns = remember(campaigns) {
        campaigns.count { it.status.equals("active", ignoreCase = true) }
    }

    val overallCtr = remember(totalImpressions, totalClicks) {
        if (totalImpressions > 0L) {
            String.format(Locale.US, "%.2f", (totalClicks.toDouble() / totalImpressions.toDouble()) * 100.0)
        } else {
            "0.00"
        }
    }

    // Weekly Mock Data matching web Dashboard.tsx
    val weeklyData = remember {
        listOf(
            WeeklyDayData("Mon", 1200, 45),
            WeeklyDayData("Tue", 1350, 52),
            WeeklyDayData("Wed", 1100, 38),
            WeeklyDayData("Thu", 1800, 72),
            WeeklyDayData("Fri", 2100, 88),
            WeeklyDayData("Sat", 1900, 76),
            WeeklyDayData("Sun", 1600, 61)
        )
    }

    val maxWeeklyImpression = 2100f

    val emeraldText = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)
    val emeraldSub = if (AppColors.isLight) Color(0xFF047857) else Color(0xFF6EE7B7)
    val blueText = if (AppColors.isLight) Color(0xFF2563EB) else Color(0xFF60A5FA)
    val skyText = if (AppColors.isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)
    val amberText = if (AppColors.isLight) Color(0xFFD97706) else Color(0xFFFBBF24)

    val topCampaigns = remember(campaigns) {
        campaigns.distinctBy { it.id }.sortedByDescending { it.impressions }.take(5)
    }

    val headerSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.bgChip)
                        .testTag("dashboard_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ad Analytics",
                    fontSize = 24.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Track your campaign performance and analytics.",
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (onNavigateToCampaigns != null) {
                Button(
                    onClick = onNavigateToCampaigns,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.bgCard,
                        contentColor = AppColors.textPrimary
                    ),
                    border = BorderStroke(1.dp, AppColors.border),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = skyText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Campaigns",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textPrimary
                    )
                }
            }
        }
    }

    val statsGridSection: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "Total Impressions",
                    value = formatNumber(totalImpressions),
                    icon = Icons.Default.Visibility,
                    trend = "up",
                    trendValue = "12.5%",
                    iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    iconTint = blueText,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Total Clicks",
                    value = formatNumber(totalClicks),
                    icon = Icons.Default.TouchApp,
                    trend = "up",
                    trendValue = "8.2%",
                    iconBgColor = Color(0xFF10B981).copy(alpha = 0.15f),
                    iconTint = emeraldText,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "Total Views",
                    value = formatNumber(totalViews),
                    icon = Icons.Default.PlayArrow,
                    trend = "down",
                    trendValue = "3.1%",
                    iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    iconTint = amberText,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Total Spend",
                    value = "$${totalSpend}",
                    icon = Icons.Default.AttachMoney,
                    trend = "up",
                    trendValue = "15.3%",
                    iconBgColor = Color(0xFFF43F5E).copy(alpha = 0.15f),
                    iconTint = Color(0xFFE11D48),
                    subtitleNote = "100% Free Promo",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val secondRowStatsSection: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Click-Through Rate Card with custom Progress Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                border = BorderStroke(1.dp, AppColors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Click-Through Rate (CTR)",
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                color = AppColors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$overallCtr%",
                                fontSize = 32.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "CTR Chart",
                                tint = blueText,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val ctrNum = overallCtr.toFloatOrNull() ?: 0f
                    val progressFraction = (ctrNum / 10f).coerceIn(0.05f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.bgChip)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                                    )
                                )
                        )
                    }

                    Text(
                        text = "Industry average: 1.5 - 2.5%",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.textMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            // Active Campaigns Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                border = BorderStroke(1.dp, AppColors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Campaigns",
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            color = AppColors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$activeCampaigns",
                            fontSize = 32.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "out of ${campaigns.size} total",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Active Campaigns",
                            tint = emeraldText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    val weeklyChartSection: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
            border = BorderStroke(1.dp, AppColors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Performance",
                        fontSize = 17.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )

                    Box {
                        Surface(
                            onClick = { showTimeDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.bgInput,
                            border = BorderStroke(1.dp, AppColors.borderStrong)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTimeRange,
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textBody,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTimeDropdown,
                            onDismissRequest = { showTimeDropdown = false },
                            modifier = Modifier.background(AppColors.bgCard)
                        ) {
                            listOf("Last 7 days", "Last 30 days", "Last 90 days").forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            opt,
                                            color = AppColors.textPrimary,
                                            fontSize = 13.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        selectedTimeRange = opt
                                        showTimeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                var selectedDayTooltip by remember { mutableStateOf<WeeklyDayData?>(null) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyData.forEach { day ->
                        val imprFraction = (day.impressions / maxWeeklyImpression).coerceIn(0.1f, 1f)
                        val clickFraction = ((day.clicks * 20f) / maxWeeklyImpression).coerceIn(0.05f, 0.9f)
                        val isSelected = selectedDayTooltip?.day == day.day

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedDayTooltip = if (isSelected) null else day
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AppColors.bgChip.copy(alpha = 0.65f))
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction = imprFraction)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) Color(0xFF3B82F6)
                                            else Color(0xFF3B82F6).copy(alpha = 0.8f)
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction = clickFraction)
                                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                        .background(
                                            if (isSelected) Color(0xFF10B981)
                                            else Color(0xFF10B981).copy(alpha = 0.85f)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = day.day,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                color = if (isSelected) AppColors.textPrimary else AppColors.textMuted,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                selectedDayTooltip?.let { day ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.bgInput,
                        border = BorderStroke(1.dp, AppColors.borderStrong)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${day.day} Breakdown",
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = "Impressions: ${day.impressions} • Clicks: ${day.clicks}",
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = skyText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppColors.border, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF3B82F6))
                        )
                        Text(text = "Impressions", fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF10B981))
                        )
                        Text(text = "Clicks", fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary)
                    }
                }
            }
        }
    }

    val topPerformingHeaderSection: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Performing Campaigns",
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = "Campaigns ranked by engagement and impressions",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (campaigns.isNotEmpty()) {
                    Text(
                        text = "${topCampaigns.size} shown",
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.textMuted
                    )
                }
            }
        }
    }

    val emptyCampaignsSection: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
            border = BorderStroke(1.dp, AppColors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.bgChip),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = AppColors.textMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "No campaigns yet",
                    fontSize = 15.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "Create your first ad to see performance data",
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.textSecondary
                )
            }
        }
    }

    if (isEmbedded) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(AppColors.bgScreen)
                .padding(bottom = 16.dp)
                .testTag("ad_dashboard_view"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            headerSection()
            statsGridSection()
            secondRowStatsSection()
            weeklyChartSection()
            topPerformingHeaderSection()
            if (topCampaigns.isEmpty()) {
                emptyCampaignsSection()
            } else {
                topCampaigns.forEach { ad ->
                    TopCampaignPerformanceCard(ad = ad)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.bgScreen)
                .testTag("ad_dashboard_view"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "dashboard_header") { headerSection() }
            item(key = "stats_grid") { statsGridSection() }
            item(key = "second_row_stats") { secondRowStatsSection() }
            item(key = "weekly_performance_chart") { weeklyChartSection() }
            item(key = "top_performing_header") { topPerformingHeaderSection() }
            if (topCampaigns.isEmpty()) {
                item(key = "top_performing_empty") { emptyCampaignsSection() }
            } else {
                items(topCampaigns, key = { "top_ad_${it.id}" }) { ad ->
                    TopCampaignPerformanceCard(ad = ad)
                }
            }
        }
    }
}

/**
 * TopCampaignPerformanceCard - Matches web Mobile View & Tablet View cards
 * Features:
 * - Media Thumbnail (or fallback gradient)
 * - Name, location, days
 * - Status Pill badge (active=emerald, paused=amber, completed=zinc)
 * - Prominent CTR Badge (Emerald Pill with large % and CTR label)
 * - 3-Stat Bottom Row (Impressions, Views, Clicks)
 */
@Composable
private fun TopCampaignPerformanceCard(ad: AdCampaign) {
    val ctr = remember(ad.impressions, ad.clicks) {
        if (ad.impressions > 0) {
            String.format(Locale.US, "%.1f", (ad.clicks.toDouble() / ad.impressions.toDouble()) * 100.0)
        } else {
            "0.0"
        }
    }
    val emeraldText = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)
    val emeraldSub = if (AppColors.isLight) Color(0xFF047857) else Color(0xFF6EE7B7)
    val amberText = if (AppColors.isLight) Color(0xFFD97706) else Color(0xFFFBBF24)
    val blueText = if (AppColors.isLight) Color(0xFF2563EB) else Color(0xFF60A5FA)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        border = BorderStroke(1.dp, AppColors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Thumbnail + Info + Prominent CTR Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Media Thumbnail
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.bgChip)
                            .border(1.dp, AppColors.borderStrong, RoundedCornerShape(10.dp))
                    ) {
                        if (!ad.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ad.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF059669))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Title + location + status badge
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ad.name.ifBlank { "Campaign #${ad.id}" },
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${ad.location.ifBlank { "Global" }} • ${ad.days}d",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )

                        // Status pill
                        val (statusBg, statusBorder, statusColor) = when (ad.status.lowercase()) {
                            "active" -> Triple(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981).copy(alpha = 0.35f), emeraldText)
                            "paused" -> Triple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B).copy(alpha = 0.35f), amberText)
                            else -> Triple(AppColors.bgChip, AppColors.borderStrong, AppColors.textSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(statusBg)
                                .border(1.dp, statusBorder, RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ad.status.uppercase(),
                                fontSize = 9.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Prominently Positioned CTR Badge matching web mobile & desktop
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$ctr%",
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = emeraldText,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "CTR",
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = emeraldSub,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Stats 3-column row matching web mobile
            HorizontalDivider(color = AppColors.border, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatBox(
                    label = "Impressions",
                    value = formatNumber(ad.impressions.toLong()),
                    valueColor = AppColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                CompactStatBox(
                    label = "Views",
                    value = formatNumber(ad.views.toLong()),
                    valueColor = blueText,
                    modifier = Modifier.weight(1f)
                )
                CompactStatBox(
                    label = "Clicks",
                    value = formatNumber(ad.clicks.toLong()),
                    valueColor = AppColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactStatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.bgInput)
            .border(1.dp, AppColors.border, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                color = AppColors.textSecondary,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

/**
 * DashboardStatCard - Component for primary 4 stats
 */
@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    trend: String? = null,
    trendValue: String? = null,
    iconBgColor: Color,
    iconTint: Color,
    subtitleNote: String? = null,
    modifier: Modifier = Modifier
) {
    val emeraldText = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)
    val redText = if (AppColors.isLight) Color(0xFFDC2626) else Color(0xFFF87171)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        border = BorderStroke(1.dp, AppColors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        fontSize = 22.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (trend != null && trendValue != null) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (trend == "up") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (trend == "up") emeraldText else redText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = trendValue,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = if (trend == "up") emeraldText else redText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "vs last week",
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.textMuted
                    )
                }
            } else if (subtitleNote != null) {
                Text(
                    text = subtitleNote,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = emeraldText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private data class WeeklyDayData(
    val day: String,
    val impressions: Int,
    val clicks: Int
)

private fun formatNumber(num: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(num)
}
