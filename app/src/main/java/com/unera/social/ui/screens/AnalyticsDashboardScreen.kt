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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
private val FeatheredDarkBg = Color(0xFF0F172A)
private val FeatheredSurface = Color(0xFF1E293B)
private val FeatheredSurfaceVariant = Color(0xFF27354A)
private val FeatheredBorder = Color(0xFF334155)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val GrowthGreen = Color(0xFF22C55E)
private val DeclineRed = Color(0xFFEF4444)

enum class AnalyticsTab(val title: String) {
    VIEWS("Views"),
    EARNINGS("Earnings")
}

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

    var isLoading by remember { mutableStateOf(true) }
    var analyticsData by remember { mutableStateOf(GlobalAnalytics()) }
    var showAllCountries by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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

                // Rounded Pill Tabs: Views, Earnings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        showAllCountries = showAllCountries,
                        onPeriodClick = { showPeriodMenu = true },
                        onInfoClick = { showInfoDialog = true },
                        onToggleShowAllCountries = { showAllCountries = !showAllCountries },
                        modifier = Modifier.fillMaxSize()
                    )

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
                }
                AnalyticsTab.EARNINGS -> {
                    EarningsComingSoonContent(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Info Dialog
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = {
                        Text("About Views Metric", color = TextWhite, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            "This metric represents the total number of times your posts and videos have been viewed across Feathered Social during the selected date range. Each user view is tracked uniquely per session.",
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

@Composable
private fun ViewsDashboardContent(
    analytics: GlobalAnalytics,
    selectedPeriod: AnalyticsTimePeriod,
    isLoading: Boolean,
    showAllCountries: Boolean,
    onPeriodClick: () -> Unit,
    onInfoClick: () -> Unit,
    onToggleShowAllCountries: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Time Range Selector & Date Range Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FeatheredSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                    modifier = Modifier
                        .clickable { onPeriodClick() }
                        .testTag("analytics_period_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedPeriod.label,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Time Range",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Date Range Label (e.g. "Oct 12 - Nov 8")
                Text(
                    text = analytics.periodLabel,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Main Metric (e.g. "33 Views") & Growth percentage
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${analytics.totalViews} Views",
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("analytics_main_metric")
                    )

                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Views Information",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Percentage change vs previous period
                val growth = analytics.growthPercentage
                val isPositive = growth > 0f
                val isNegative = growth < 0f
                val growthColor = when {
                    isPositive -> GrowthGreen
                    isNegative -> DeclineRed
                    else -> TextMuted
                }
                val growthIcon = when {
                    isPositive -> Icons.AutoMirrored.Filled.TrendingUp
                    isNegative -> Icons.AutoMirrored.Filled.TrendingDown
                    else -> null
                }
                val absGrowth = abs(growth)
                val growthText = when {
                    isPositive -> "↑ ${String.format(Locale.US, "%.0f", absGrowth)}% from previous ${selectedPeriod.days} days"
                    isNegative -> "↓ ${String.format(Locale.US, "%.0f", absGrowth)}% from previous ${selectedPeriod.days} days"
                    analytics.totalViews > 0L -> "No prior data for comparison"
                    else -> "0% from previous ${selectedPeriod.days} days"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (growthIcon != null) {
                        Icon(
                            imageVector = growthIcon,
                            contentDescription = null,
                            tint = growthColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = growthText,
                        color = growthColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Views Interactive Line Chart
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
                        .padding(16.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = FeatheredBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        ViewsInteractiveChart(
                            points = analytics.dailyViews,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }
        }

        // 4. Section Divider
        item {
            HorizontalDivider(
                color = FeatheredBorder.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 5. Audience Analytics Header: "How people view your content"
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How people view your content",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Followers / Non-followers breakdown pills
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
                                .background(Color(0xFF0369A1))
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
        }

        // 6. TOP COUNTRIES
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

                    val countries = analytics.topCountries
                    if (countries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No location data available yet",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        val displayCount = if (showAllCountries) countries.size else countries.size.coerceAtMost(4)
                        val visibleList = countries.take(displayCount)

                        visibleList.forEachIndexed { index, country ->
                            CountryPercentageRow(
                                country = country,
                                rank = index + 1
                            )
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
        }

        // 7. GENDER BREAKDOWN
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
                            GenderAnalytics("Male", 0L, 0f),
                            GenderAnalytics("Female", 0L, 0f),
                            GenderAnalytics("Other", 0L, 0f),
                            GenderAnalytics("Unknown", 0L, 0f)
                        )
                    }

                    genders.forEach { genderItem ->
                        GenderPercentageRow(item = genderItem)
                    }
                }
            }
        }

        // Bottom space padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Interactive line chart displaying views over time with gradient fill and tap/drag tooltip.
 */
@Composable
fun ViewsInteractiveChart(
    points: List<DailyViewsPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No views data for this period", color = TextMuted)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val maxViews = remember(points) {
        val m = points.maxOfOrNull { it.views } ?: 0L
        if (m <= 0L) 10L else ((m + 4) / 5) * 5 // Round up to nearest 5
    }

    Column(modifier = modifier) {
        // Tooltip display when inspecting
        val activePoint = selectedIndex?.let { points.getOrNull(it) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (activePoint != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = FeatheredBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${activePoint.views} views",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = activePoint.label,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                Text(
                    text = "Tap or drag along the graph to inspect",
                    color = TextMuted.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Canvas Chart
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val pointSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / pointSpacing)
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = idx
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pointSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                            val idx = (offset.x / pointSpacing)
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = idx
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pointSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                            val idx = (change.position.x / pointSpacing)
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = idx
                        },
                        onDragEnd = {
                            // keep last selected point
                        }
                    )
                }
        ) {
            val chartWidth = maxWidth
            val chartHeight = maxHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw 3 horizontal grid lines (0, 50%, 100%)
                val gridColor = Color(0xFF334155).copy(alpha = 0.6f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 0f),
                    end = Offset(w, 0f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1f
                )

                if (points.size < 2) return@Canvas

                val stepX = w / (points.size - 1).toFloat()
                val path = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, h)

                val coords = points.mapIndexed { idx, pt ->
                    val x = idx * stepX
                    val y = h - ((pt.views.toFloat() / maxViews.toFloat()) * h).coerceIn(0f, h)
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

                // Subtle gradient fill under the line
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            FeatheredBlue.copy(alpha = 0.35f),
                            FeatheredBlue.copy(alpha = 0.05f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                // The solid blue line
                drawPath(
                    path = path,
                    color = FeatheredBlue,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight active selected point
                val activeIdx = selectedIndex
                if (activeIdx != null && activeIdx in coords.indices) {
                    val activePt = coords[activeIdx]
                    // Vertical indicator line
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(activePt.x, 0f),
                        end = Offset(activePt.x, h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Outer glow
                    drawCircle(
                        color = FeatheredBlue.copy(alpha = 0.4f),
                        radius = 8.dp.toPx(),
                        center = activePt
                    )
                    // Inner dot
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = activePt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Axis Date Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelIndices = when {
                points.size <= 4 -> points.indices.toList()
                else -> listOf(
                    0,
                    points.size / 3,
                    (points.size * 2) / 3,
                    points.size - 1
                )
            }

            labelIndices.distinct().forEach { idx ->
                val pt = points.getOrNull(idx) ?: return@forEach
                Text(
                    text = pt.label,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Top Country horizontal progress row with flag, name, percentage bar, and percentage label.
 */
@Composable
private fun CountryPercentageRow(
    country: CountryAnalytics,
    rank: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (country.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "country_bar"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = country.flag,
                    fontSize = 18.sp
                )
                Text(
                    text = country.name,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${String.format(Locale.US, "%.1f", country.percentage)}%",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF033B6B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FeatheredBlue)
            )
        }
    }
}

/**
 * Gender breakdown horizontal row.
 */
@Composable
private fun GenderPercentageRow(
    item: GenderAnalytics
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (item.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "gender_bar"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.gender,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${String.format(Locale.US, "%.1f", item.percentage)}%",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF033B6B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FeatheredBlue)
            )
        }
    }
}

/**
 * Earnings Coming Soon Screen in Feathered Blue & Dark Theme.
 */
@Composable
private fun EarningsComingSoonContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Blue Icon Container
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(FeatheredBlue.copy(alpha = 0.15f))
                    .border(2.dp, FeatheredBlue.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Earnings",
                    tint = FeatheredBlue,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Earnings",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Monetization is coming soon",
                color = FeatheredBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Feathered Creator Monetization is currently in development. You will soon be able to earn from your high-performing posts, video reels, and creator content.",
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Planned feature pills
            Card(
                colors = CardDefaults.cardColors(containerColor = FeatheredSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatheredBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Upcoming Monetization Programs",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    UpcomingProgramItem(
                        icon = Icons.Default.Paid,
                        title = "Ad Revenue Sharing",
                        subtitle = "Earn a share from impressions on your video feeds"
                    )

                    UpcomingProgramItem(
                        icon = Icons.Default.Stars,
                        title = "Subscriber Badges",
                        subtitle = "Offer exclusive perks and content to monthly supporters"
                    )

                    UpcomingProgramItem(
                        icon = Icons.Default.VolunteerActivism,
                        title = "Creator Tipping",
                        subtitle = "Receive direct tips and gifts on your posts"
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingProgramItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FeatheredBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FeatheredBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
