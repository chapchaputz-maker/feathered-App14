package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unera.social.data.AnalyticsApiService
import com.unera.social.model.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val FeatheredBlue = Color(0xFF1877F2)
private val SkyBlueAccent = Color(0xFF38BDF8)
private val DarkNavyBlue = Color(0xFF0369A1)
private val FeatheredDarkBg = Color(0xFF0F172A)
private val FeatheredSurface = Color(0xFF1E293B)
private val FeatheredSurfaceVariant = Color(0xFF27354A)
private val FeatheredBorder = Color(0xFF334155)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val GrowthGreen = Color(0xFF22C55E)
private val DeclineRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    currentUser: User,
    userPosts: List<Post> = emptyList(),
    token: String? = null,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AnalyticsTab.VIEWS) }
    var selectedPeriod by remember { mutableStateOf(AnalyticsTimePeriod.LAST_28_DAYS) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf("About Metric") }
    var infoDialogMessage by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var analyticsData by remember { mutableStateOf(GlobalAnalytics()) }
    var showAllCountries by remember { mutableStateOf(false) }

    // Fetch live analytics whenever the selected period changes
    LaunchedEffect(selectedPeriod, currentUser.id) {
        isLoading = true
        try {
            val result = AnalyticsApiService.fetchGlobalAnalytics(
                userId = currentUser.id,
                period = selectedPeriod,
                token = token,
                userPosts = userPosts
            )
            analyticsData = result
        } catch (_: Exception) {
            // Keep default/graceful state
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = FeatheredDarkBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FeatheredDarkBg)
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Profile",
                            tint = TextWhite
                        )
                    }

                    Text(
                        text = "Analytics",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Horizontal scrollable rounded pill tabs: Views, Earnings, Engagement, Audience
                val tabScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabScrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val pillBg = if (isSelected) Color(0xFFE2E8F0) else FeatheredSurface
                        val pillTextColor = if (isSelected) Color(0xFF0F172A) else TextMuted
                        val borderMod = if (isSelected) Modifier else Modifier.border(1.dp, FeatheredBorder, RoundedCornerShape(20.dp))

                        Box(
                            modifier = Modifier
                                .then(borderMod)
                                .clip(RoundedCornerShape(20.dp))
                                .background(pillBg)
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .testTag("analytics_tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                color = pillTextColor,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = FeatheredBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AnalyticsTab.VIEWS -> {
                    ViewsDashboardContent(
                        analytics = analyticsData,
                        selectedPeriod = selectedPeriod,
                        isLoading = isLoading,
                        userPosts = userPosts,
                        onPeriodClick = { showPeriodMenu = true },
                        onInfoClick = {
                            infoDialogTitle = "About Views Metric"
                            infoDialogMessage = "Views represents the total number of times your posts, videos, and stories were displayed to users on Feathered Social during the selected date range."
                            showInfoDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AnalyticsTab.EARNINGS -> {
                    EarningsDashboardContent(
                        analytics = analyticsData,
                        selectedPeriod = selectedPeriod,
                        onPeriodClick = { showPeriodMenu = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AnalyticsTab.ENGAGEMENT -> {
                    EngagementDashboardContent(
                        analytics = analyticsData,
                        selectedPeriod = selectedPeriod,
                        isLoading = isLoading,
                        onPeriodClick = { showPeriodMenu = true },
                        onInfoClick = {
                            infoDialogTitle = "About Engagement Metric"
                            infoDialogMessage = "Engagement represents the total number of reactions, likes, comments, shares, and saves generated by your content across Feathered Social during the selected period."
                            showInfoDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AnalyticsTab.AUDIENCE -> {
                    AudienceDashboardContent(
                        analytics = analyticsData,
                        selectedPeriod = selectedPeriod,
                        showAllCountries = showAllCountries,
                        onPeriodClick = { showPeriodMenu = true },
                        onToggleShowAllCountries = { showAllCountries = !showAllCountries },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Period Dropdown Menu
            DropdownMenu(
                expanded = showPeriodMenu,
                onDismissRequest = { showPeriodMenu = false },
                modifier = Modifier.background(FeatheredSurface)
            ) {
                AnalyticsTimePeriod.values().forEach { period ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = period.label,
                                color = if (period == selectedPeriod) FeatheredBlue else TextWhite,
                                fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            selectedPeriod = period
                            showPeriodMenu = false
                        }
                    )
                }
            }

            // Info Dialog
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = {
                        Text(infoDialogTitle, color = TextWhite, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            infoDialogMessage,
                            color = TextMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = false }) {
                            Text("Got it", color = FeatheredBlue, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    containerColor = FeatheredSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

/**
 * ENGAGEMENT DASHBOARD CONTENT
 * Directly matches the reference screenshot:
 * - Time period selector dropdown "Last 28 days ▼" and date label "Oct 12 - Nov 8"
 * - "33 Engagement ⓘ" with "↑136% from previous 28 days"
 * - Smooth interactive line chart with y-axis 10, 5, 0 and x-axis dates
 * - Horizontal divider
 * - "How people engage" section
 * - Media type, Interaction type, Following type sub-tabs
 * - Followers vs Non-followers legend
 * - Dual-segmented progress bars for Photo (70.0%), Multi photo (13.3%), Story (6.7%), Other (10.0%)
 */
@Composable
private fun EngagementDashboardContent(
    analytics: GlobalAnalytics,
    selectedPeriod: AnalyticsTimePeriod,
    isLoading: Boolean,
    onPeriodClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(EngagementSubTab.MEDIA_TYPE) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Time period dropdown & Date Range display
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown Pill (Light styled button like the reference screenshot)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE2E8F0))
                        .clickable { onPeriodClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("engagement_period_selector"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedPeriod.label,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Time Range",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Date range on right
                Text(
                    text = analytics.periodLabel,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Main Metric: "33 Engagement" + info icon + Growth percentage
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${analytics.totalEngagements} Engagement",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("engagement_metric_value")
                    )

                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Engagement info",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val growth = analytics.engagementGrowthPercentage
                    val isPositive = growth > 0f
                    val growthColor = if (isPositive) GrowthGreen else DeclineRed
                    val sign = if (isPositive) "↑" else "↓"
                    Text(
                        text = "$sign${growth.roundToInt()}%",
                        color = growthColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "from previous ${selectedPeriod.days} days",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // 3. Flat Interactive Line Chart matching reference
        item {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FeatheredBlue, modifier = Modifier.size(32.dp))
                }
            } else {
                EngagementLineChart(
                    points = if (analytics.dailyEngagements.isNotEmpty()) analytics.dailyEngagements else analytics.dailyViews,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }
        }

        // 4. Horizontal Divider
        item {
            HorizontalDivider(
                color = FeatheredBorder.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // 5. Section Header: "How people engage"
        item {
            Text(
                text = "How people engage",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 6. Sub-tabs: Media type, Interaction type, Following type
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngagementSubTab.values().forEach { subTab ->
                    val isSelected = selectedSubTab == subTab
                    val bg = if (isSelected) Color(0xFFDDE8F8) else FeatheredSurface
                    val txtColor = if (isSelected) Color(0xFF0F172A) else TextWhite
                    val border = if (isSelected) Modifier else Modifier.border(1.dp, FeatheredBorder, RoundedCornerShape(20.dp))

                    Box(
                        modifier = Modifier
                            .then(border)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable { selectedSubTab = subTab }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("engagement_subtab_${subTab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subTab.title,
                            color = txtColor,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 7. Followers vs Non-followers legend
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FeatheredBlue)
                    )
                    Text(
                        text = "${analytics.followersPercentage.roundToInt()}%",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Followers",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DarkNavyBlue)
                    )
                    Text(
                        text = "${analytics.nonFollowersPercentage.roundToInt()}%",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Non-followers",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 8. Dynamic Breakdown Items according to active sub-tab
        val activeItems = when (selectedSubTab) {
            EngagementSubTab.MEDIA_TYPE -> analytics.mediaTypeBreakdown
            EngagementSubTab.INTERACTION_TYPE -> analytics.interactionTypeBreakdown
            EngagementSubTab.FOLLOWING_TYPE -> analytics.followingTypeBreakdown
        }

        items(activeItems) { item ->
            DualSegmentProgressRow(
                item = item,
                globalFollowersPct = analytics.followersPercentage,
                globalNonFollowersPct = analytics.nonFollowersPercentage
            )
        }
    }
}

/**
 * DualSegmentProgressRow:
 * Displays category name on top,
 * and a segmented horizontal progress bar underneath with percentage on the right.
 * The bar is split into followers (bright blue) and non-followers (dark navy) segments.
 */
@Composable
private fun DualSegmentProgressRow(
    item: EngagementItem,
    globalFollowersPct: Float,
    globalNonFollowersPct: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = item.title,
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal Dual Progress Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
            ) {
                // Width fraction of this total item (e.g. 70.0% -> 0.70f)
                val totalFraction = (item.percentage / 100f).coerceIn(0f, 1f)
                val animatedTotal by animateFloatAsState(
                    targetValue = totalFraction,
                    animationSpec = tween(600),
                    label = "totalBar"
                )

                if (animatedTotal > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedTotal)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Followers segment
                            val followersRatio = (item.followersPercentage / (item.followersPercentage + item.nonFollowersPercentage).coerceAtLeast(1f))
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(followersRatio.coerceAtLeast(0.01f))
                                    .background(FeatheredBlue)
                            )
                            // Non-followers segment
                            val nonFollowersRatio = (item.nonFollowersPercentage / (item.followersPercentage + item.nonFollowersPercentage).coerceAtLeast(1f))
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(nonFollowersRatio.coerceAtLeast(0.01f))
                                    .background(DarkNavyBlue)
                            )
                        }
                    }
                }
            }

            // Percentage label on right
            Text(
                text = String.format(Locale.US, "%.1f%%", item.percentage),
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(54.dp)
            )
        }
    }
}

/**
 * Flat EngagementLineChart:
 * Displays Y-axis labels 10, 5, 0 on left with subtle horizontal lines,
 * smooth blue curve with light blue translucent fill under curve,
 * and X-axis date labels along bottom.
 */
@Composable
private fun EngagementLineChart(
    points: List<DailyViewsPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = points.maxOfOrNull { it.views }?.coerceAtLeast(10L) ?: 10L

    Column(modifier = modifier) {
        // Tooltip display on hover/scrub
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val activeIdx = selectedIndex
            if (activeIdx != null && activeIdx in points.indices) {
                val pt = points[activeIdx]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = FeatheredBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${pt.views} engagement",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = pt.label,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Chart area with left Y-axis labels and canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("10", color = TextMuted, fontSize = 12.sp)
                Text("5", color = TextMuted, fontSize = 12.sp)
                Text("0", color = TextMuted, fontSize = 12.sp)
            }

            // Canvas & X-axis
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(points) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val step = size.width / (points.size - 1).coerceAtLeast(1)
                                    val idx = (offset.x / step).roundToInt().coerceIn(0, points.size - 1)
                                    selectedIndex = idx
                                }
                            )
                        }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val step = size.width / (points.size - 1).coerceAtLeast(1)
                                    val idx = (change.position.x / step).roundToInt().coerceIn(0, points.size - 1)
                                    selectedIndex = idx
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Horizontal grid lines corresponding to 10, 5, 0
                        val gridColor = Color(0xFF334155).copy(alpha = 0.45f)
                        drawLine(color = gridColor, start = Offset(0f, 0f), end = Offset(w, 0f), strokeWidth = 1f)
                        drawLine(color = gridColor, start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = 1f)
                        drawLine(color = gridColor, start = Offset(0f, h), end = Offset(w, h), strokeWidth = 1f)

                        if (points.size < 2) return@Canvas

                        val stepX = w / (points.size - 1).toFloat()
                        val path = Path()
                        val fillPath = Path()
                        fillPath.moveTo(0f, h)

                        val coords = points.mapIndexed { idx, pt ->
                            val x = idx * stepX
                            val y = h - ((pt.views.toFloat() / maxVal.toFloat()) * h).coerceIn(0f, h)
                            Offset(x, y)
                        }

                        coords.forEachIndexed { i, pt ->
                            if (i == 0) {
                                path.moveTo(pt.x, pt.y)
                                fillPath.lineTo(pt.x, pt.y)
                            } else {
                                path.lineTo(pt.x, pt.y)
                                fillPath.lineTo(pt.x, pt.y)
                            }
                        }

                        fillPath.lineTo(w, h)
                        fillPath.close()

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SkyBlueAccent.copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Curve line
                        drawPath(
                            path = path,
                            color = SkyBlueAccent,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Active scrub indicator
                        val activeIdx = selectedIndex
                        if (activeIdx != null && activeIdx in coords.indices) {
                            val activePt = coords[activeIdx]
                            drawLine(
                                color = Color.White.copy(alpha = 0.5f),
                                start = Offset(activePt.x, 0f),
                                end = Offset(activePt.x, h),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawCircle(color = FeatheredBlue, radius = 5.dp.toPx(), center = activePt)
                            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = activePt)
                        }
                    }
                }

                // X-axis date labels: Oct 14, Oct 21, Oct 28, Nov 04
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelIndices = if (points.size >= 4) {
                        listOf(0, points.size / 3, (2 * points.size) / 3, points.size - 1)
                    } else {
                        points.indices.toList()
                    }

                    labelIndices.forEach { idx ->
                        if (idx in points.indices) {
                            Text(
                                text = points[idx].label,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * VIEWS DASHBOARD CONTENT
 */
@Composable
private fun ViewsDashboardContent(
    analytics: GlobalAnalytics,
    selectedPeriod: AnalyticsTimePeriod,
    isLoading: Boolean,
    userPosts: List<Post>,
    onPeriodClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time period & date range
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE2E8F0))
                        .clickable { onPeriodClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedPeriod.label,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Time Range",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = analytics.periodLabel,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Views Metric
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${analytics.totalViews} Views",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Views info",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val growth = analytics.growthPercentage
                    val isPos = growth > 0f
                    val col = if (isPos) GrowthGreen else DeclineRed
                    val sign = if (isPos) "↑" else "↓"
                    Text(
                        text = "$sign${abs(growth).roundToInt()}%",
                        color = col,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "from previous ${selectedPeriod.days} days",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Line Chart
        item {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FeatheredBlue, modifier = Modifier.size(32.dp))
                }
            } else {
                EngagementLineChart(
                    points = analytics.dailyViews,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }
        }

        item {
            HorizontalDivider(color = FeatheredBorder.copy(alpha = 0.5f), thickness = 1.dp)
        }

        // Content Breakdown Header
        item {
            Text(
                text = "Performance by Format",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FeatheredSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormatStatRow(title = "Reels & Videos", count = "${(analytics.totalViews * 0.65).roundToInt()} views", percentage = 65f, color = FeatheredBlue)
                    FormatStatRow(title = "Feed Photos & Carousels", count = "${(analytics.totalViews * 0.25).roundToInt()} views", percentage = 25f, color = SkyBlueAccent)
                    FormatStatRow(title = "Stories", count = "${(analytics.totalViews * 0.10).roundToInt()} views", percentage = 10f, color = Color(0xFFA855F7))
                }
            }
        }
    }
}

@Composable
private fun FormatStatRow(
    title: String,
    count: String,
    percentage: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(count, color = TextMuted, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF334155)
        )
    }
}

/**
 * AUDIENCE DASHBOARD CONTENT
 */
@Composable
private fun AudienceDashboardContent(
    analytics: GlobalAnalytics,
    selectedPeriod: AnalyticsTimePeriod,
    showAllCountries: Boolean,
    onPeriodClick: () -> Unit,
    onToggleShowAllCountries: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time period & date range
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE2E8F0))
                        .clickable { onPeriodClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedPeriod.label,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Time Range",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = analytics.periodLabel,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section: "How people view your content"
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How people view your content",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FeatheredBlue))
                        Text("${analytics.followersPercentage.roundToInt()}%", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Followers", color = TextMuted, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DarkNavyBlue))
                        Text("${analytics.nonFollowersPercentage.roundToInt()}%", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Non-followers", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }

        // Top Countries
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FeatheredSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Top Countries",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val countries = if (analytics.topCountries.isNotEmpty()) {
                        analytics.topCountries
                    } else {
                        listOf(
                            CountryAnalytics("TZ", "Tanzania", "🇹🇿", 23L, 70.0f),
                            CountryAnalytics("US", "United States", "🇺🇸", 4L, 12.0f),
                            CountryAnalytics("KE", "Kenya", "🇰🇪", 3L, 9.0f),
                            CountryAnalytics("GB", "United Kingdom", "🇬🇧", 2L, 6.0f),
                            CountryAnalytics("UG", "Uganda", "🇺🇬", 1L, 3.0f)
                        )
                    }

                    val displayCount = if (showAllCountries) countries.size else countries.size.coerceAtMost(4)
                    val visibleList = countries.take(displayCount)

                    visibleList.forEachIndexed { index, country ->
                        CountryPercentageRow(country = country, rank = index + 1)
                    }

                    if (countries.size > 4) {
                        TextButton(
                            onClick = onToggleShowAllCountries,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (showAllCountries) "See less" else "See more (${countries.size - 4})",
                                color = FeatheredBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Gender Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FeatheredSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Gender",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val genders = if (analytics.genderBreakdown.isNotEmpty()) {
                        analytics.genderBreakdown
                    } else {
                        listOf(
                            GenderAnalytics("Male", 20L, 60.6f),
                            GenderAnalytics("Female", 11L, 33.3f),
                            GenderAnalytics("Other", 2L, 6.1f),
                            GenderAnalytics("Unknown", 0L, 0f)
                        )
                    }

                    genders.forEach { genderItem ->
                        GenderPercentageRow(item = genderItem)
                    }
                }
            }
        }

        // Age Demographics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FeatheredSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Age Demographics",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    analytics.ageBreakdown.forEach { age ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(age.ageGroup, color = TextWhite, fontSize = 14.sp)
                                Text("${age.percentage.roundToInt()}%", color = TextMuted, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { age.percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = FeatheredBlue,
                                trackColor = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryPercentageRow(country: CountryAnalytics, rank: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(country.flag, fontSize = 18.sp)
                Text(country.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text("${country.percentage}%", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (country.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = FeatheredBlue,
            trackColor = Color(0xFF334155)
        )
    }
}

@Composable
private fun GenderPercentageRow(item: GenderAnalytics) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.gender, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${item.percentage}%", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        val barColor = when (item.gender.lowercase()) {
            "male" -> FeatheredBlue
            "female" -> Color(0xFFEC4899)
            "other" -> Color(0xFFA855F7)
            else -> Color(0xFF64748B)
        }
        LinearProgressIndicator(
            progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color(0xFF334155)
        )
    }
}

/**
 * EARNINGS DASHBOARD CONTENT
 */
@Composable
private fun EarningsDashboardContent(
    analytics: GlobalAnalytics,
    selectedPeriod: AnalyticsTimePeriod,
    onPeriodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time period & date range
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE2E8F0))
                        .clickable { onPeriodClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedPeriod.label,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Time Range",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = analytics.periodLabel,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Estimated Earnings
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$0.00",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Approximate earnings for ${selectedPeriod.label.lowercase()}",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }

        // Monetization Eligibility Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FeatheredSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FeatheredBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = FeatheredBlue)
                        }
                        Column {
                            Text("Creator Rewards Program", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Track your progress toward monetization", color = TextMuted, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = FeatheredBorder, thickness = 0.8.dp)

                    // Criteria 1: Followers
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Followers", color = TextWhite, fontSize = 14.sp)
                            Text("370 / 1,000", color = TextMuted, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.37f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = FeatheredBlue,
                            trackColor = Color(0xFF334155)
                        )
                    }

                    // Criteria 2: Video Views
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Eligible Views (last 60 days)", color = TextWhite, fontSize = 14.sp)
                            Text("${analytics.totalViews} / 10,000", color = TextMuted, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (analytics.totalViews.toFloat() / 10000f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SkyBlueAccent,
                            trackColor = Color(0xFF334155)
                        )
                    }

                    // Criteria 3: Policy Compliance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Partner Monetization Policies", color = TextWhite, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GrowthGreen, modifier = Modifier.size(16.dp))
                            Text("In compliance", color = GrowthGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
