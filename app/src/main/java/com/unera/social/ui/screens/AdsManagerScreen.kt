package com.unera.social.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.AdCampaign
import com.unera.social.model.Post
import com.unera.social.model.User
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily

/**
 * AdsManagerScreen - Matches web AdsManager.tsx exactly.
 * Full Ads Manager dashboard with free promotion banner, campaign cards, CTR stats,
 * pause/resume controls, delete confirmation, and creation flow.
 */
@Composable
fun AdsManagerScreen(
    campaigns: List<AdCampaign>,
    currentUser: User,
    userPosts: List<Post>,
    initialPost: Post? = null,
    onPauseCampaign: (Long) -> Unit,
    onResumeCampaign: (Long) -> Unit,
    onDeleteCampaign: (Long) -> Unit,
    onCreateCampaign: (
        postId: Long?,
        name: String,
        type: String,
        mediaUrl: String?,
        mediaUrls: List<String>,
        description: String,
        link: String?,
        phone: String?,
        email: String?,
        cta: String,
        location: String,
        budget: Int,
        days: Int
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(if (initialPost != null) "create" else "dashboard") } // "dashboard", "create", "campaigns"
    var filter by remember { mutableStateOf("all") } // "all", "active", "paused", "completed"
    var campaignToDelete by remember { mutableStateOf<AdCampaign?>(null) }
    var selectedAnalyticsCampaign by remember { mutableStateOf<AdCampaign?>(null) }

    val filteredCampaigns = remember(campaigns, filter) {
        val base = campaigns.distinctBy { it.id }
        if (filter == "all") base
        else base.filter { it.status.equals(filter, ignoreCase = true) }
    }

    BackHandler(enabled = selectedTab == "create") {
        selectedTab = if (initialPost != null) "dashboard" else "campaigns"
    }

    if (selectedTab == "create") {
        AdCreator(
            currentUser = currentUser,
            userPosts = userPosts,
            initialPost = initialPost,
            onBack = { selectedTab = if (initialPost != null) "dashboard" else "campaigns" },
            onSuccess = { selectedTab = "campaigns" },
            onCreateCampaign = { postId, name, type, mediaUrl, mediaUrls, desc, link, phone, email, cta, loc, budget, days ->
                onCreateCampaign(postId, name, type, mediaUrl, mediaUrls, desc, link, phone, email, cta, loc, budget, days)
                selectedTab = "campaigns"
            },
            modifier = modifier
        )
        return
    }

    val emeraldText = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("ads_manager_screen")
    ) {
        // Blue App Header in Light Mode / Dark Header in Dark Mode
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.headerBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.headerIconBg)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.headerIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Campaigns",
                        fontFamily = InterFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.headerText
                    )
                    Text(
                        text = "Manage your free advertisement campaigns.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = AppColors.headerSubtext
                    )
                }

                Button(
                    onClick = { selectedTab = "create" },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                        contentColor = if (AppColors.isLight) AppColors.primaryBlue else Color.White
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Ad", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = AppColors.headerDivider, thickness = 1.dp)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Controls: Tabs, Filters & Free Promo Banner
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 3-Tab selector matching web: Analytics, Create Ad, My Campaigns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.bgCard)
                            .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = "dashboard" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == "dashboard") AppColors.primaryBlue else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (selectedTab == "dashboard") Color.White else AppColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Analytics",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTab == "dashboard") Color.White else AppColors.textSecondary
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = "create" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == "create") AppColors.primaryBlue else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = if (selectedTab == "create") Color.White else AppColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Create Ad",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTab == "create") Color.White else AppColors.textSecondary
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = "campaigns" },
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedTab == "campaigns") AppColors.primaryBlue else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (selectedTab == "campaigns") Color.White else AppColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Campaigns",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedTab == "campaigns") Color.White else AppColors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Filters (shown in Campaigns tab)
                    if (selectedTab == "campaigns") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("all" to "All Status", "active" to "Active", "paused" to "Paused", "completed" to "Completed").forEach { (key, label) ->
                                val isSel = filter == key
                                Surface(
                                    onClick = { filter = key },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) AppColors.bgBlueTint else AppColors.bgCard,
                                    border = BorderStroke(1.dp, if (isSel) AppColors.primaryBlue else AppColors.border),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label.split(" ").first(),
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSel) (if (AppColors.isLight) AppColors.primaryBlue else Color.White) else AppColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Free Promotion Banner matching web
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = if (AppColors.isLight) 0.12f else 0.10f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = emeraldText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All campaigns are completely free on Feathered • $0 cost",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = emeraldText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Main Body: Analytics Dashboard View or Campaigns List View
            if (selectedTab == "dashboard") {
                item(key = "dashboard_body") {
                    AdDashboardView(
                        campaigns = campaigns,
                        onBack = null,
                        onNavigateToCampaigns = { selectedTab = "campaigns" },
                        isEmbedded = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (filteredCampaigns.isEmpty()) {
                item(key = "empty_campaigns") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                        border = BorderStroke(1.dp, AppColors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.bgChip),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = AppColors.textMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No campaigns found",
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = "Create your first free ad.",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = AppColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { selectedTab = "create" },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue, contentColor = Color.White)
                            ) {
                                Text("Create Campaign", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                items(filteredCampaigns, key = { it.id }) { ad ->
                    AdCampaignCard(
                        ad = ad,
                        onTogglePause = {
                            if (ad.status == "active") onPauseCampaign(ad.id)
                            else onResumeCampaign(ad.id)
                        },
                        onDelete = { campaignToDelete = ad },
                        onViewAnalytics = { selectedAnalyticsCampaign = ad }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        campaignToDelete?.let { ad ->
            AlertDialog(
                onDismissRequest = { campaignToDelete = null },
                containerColor = AppColors.bgCard,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text("Delete Campaign", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Text("Are you sure you want to delete \"${ad.name}\"? This action cannot be undone.", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCampaign(ad.id)
                            campaignToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Delete", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { campaignToDelete = null }) {
                        Text("Cancel", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textSecondary)
                    }
                }
            )
        }

        // Campaign Analytics Modal Dialog (matching web showAdAnalytics)
        selectedAnalyticsCampaign?.let { ad ->
            CampaignAnalyticsModal(
                campaign = ad,
                onClose = { selectedAnalyticsCampaign = null }
            )
        }
    }
}

/**
 * AdCampaignCard - Matches web Card view in AdsManager.tsx
 */
@Composable
private fun AdCampaignCard(
    ad: AdCampaign,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
    onViewAnalytics: () -> Unit
) {
    val mediaUrl = ad.mediaUrls.firstOrNull() ?: ad.mediaUrl
    val isVideo = ad.type == "video"
    val ctr = if (ad.impressions == 0) "0.0" else String.format(java.util.Locale.US, "%.1f", (ad.clicks.toDouble() / ad.impressions) * 100)

    val emeraldText = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)
    val amberText = if (AppColors.isLight) Color(0xFFD97706) else Color(0xFFFBBF24)
    val analyticsAccent = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        border = BorderStroke(1.dp, AppColors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Thumbnail + Info + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Thumbnail with FREE badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.bgChip)
                        .border(1.dp, AppColors.borderStrong, RoundedCornerShape(10.dp))
                ) {
                    if (!mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = ad.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        // FREE badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .clip(RoundedCornerShape(bottomEnd = 4.dp))
                                .background(Color(0xFF10B981))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("FREE", fontFamily = InterFontFamily, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ad.name.ifBlank { "Campaign #${ad.id}" },
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (ad.status) {
                                    "active" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "paused" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> AppColors.bgChip
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ad.status.uppercase(),
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (ad.status) {
                                "active" -> emeraldText
                                "paused" -> amberText
                                else -> AppColors.textSecondary
                            }
                        )
                    }
                }

                // Actions: Analytics, Pause/Resume & Delete
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onViewAnalytics,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.bgChip)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Analytics",
                            tint = analyticsAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePause,
                        enabled = ad.status != "completed",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.bgChip)
                    ) {
                        Icon(
                            imageVector = if (ad.status == "active") Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (ad.status == "active") "Pause" else "Resume",
                            tint = if (ad.status == "active") Color(0xFFF59E0B) else emeraldText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.bgChip)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = AppColors.dangerText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Stats Grid (5 columns matching web)
            HorizontalDivider(color = AppColors.border, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(icon = Icons.Default.Visibility, value = ad.impressions.toString(), label = "Impr.", valueColor = AppColors.textPrimary)
                StatColumn(icon = Icons.Default.PlayArrow, value = ad.views.toString(), label = "Views", valueColor = AppColors.textPrimary)
                StatColumn(icon = Icons.Default.TouchApp, value = ad.clicks.toString(), label = "Clicks", valueColor = AppColors.textPrimary)
                StatColumn(icon = Icons.Default.TrendingUp, value = "$ctr%", label = "CTR", valueColor = emeraldText)
                StatColumn(icon = Icons.Default.Star, value = "$0", label = "FREE", valueColor = emeraldText)
            }

            // Date Range Footer
            HorizontalDivider(color = AppColors.border, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp))
                    Text("${ad.startDate} - ${ad.endDate}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textMuted)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = emeraldText, modifier = Modifier.size(14.dp))
                    Text("Free", fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = emeraldText)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    value: String,
    label: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        Text(text = label, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp, color = AppColors.textMuted)
    }
}

/**
 * CampaignAnalyticsModal - Matches web showAdAnalytics modal dialog exactly.
 * Displays impressions, views, clicks, CTR%, cost ($0 FREE), location, and dates.
 */
@Composable
fun CampaignAnalyticsModal(
    campaign: AdCampaign,
    onClose: () -> Unit
) {
    val ctr = if (campaign.impressions > 0) {
        String.format(java.util.Locale.US, "%.2f", (campaign.clicks.toDouble() / campaign.impressions.toDouble()) * 100.0)
    } else "0.00"
    val emeraldText = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)
    val amberText = if (AppColors.isLight) Color(0xFFD97706) else Color(0xFFFBBF24)
    val ctaAccent = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = AppColors.bgCard,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = campaign.name.ifBlank { "Campaign #${campaign.id}" },
                            fontFamily = InterFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Campaign Performance Analytics",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = AppColors.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (campaign.status == "active") Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFFF59E0B).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = campaign.status.uppercase(),
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (campaign.status == "active") emeraldText else amberText
                        )
                    }
                }

                HorizontalDivider(color = AppColors.border, thickness = 1.dp)

                // 2x2 Metrics Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsMetricTile(
                        title = "Impressions",
                        value = java.text.NumberFormat.getIntegerInstance().format(campaign.impressions),
                        icon = Icons.Default.Visibility,
                        color = if (AppColors.isLight) Color(0xFF0284C7) else Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMetricTile(
                        title = "Views",
                        value = java.text.NumberFormat.getIntegerInstance().format(campaign.views),
                        icon = Icons.Default.PlayArrow,
                        color = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsMetricTile(
                        title = "Clicks",
                        value = java.text.NumberFormat.getIntegerInstance().format(campaign.clicks),
                        icon = Icons.Default.TouchApp,
                        color = emeraldText,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsMetricTile(
                        title = "Click-Through",
                        value = "$ctr%",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Additional Campaign Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.bgInput),
                    border = BorderStroke(1.dp, AppColors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spend / Cost:", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary)
                            Text("FREE ($0)", fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = emeraldText)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Location:", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary)
                            Text(campaign.location.ifBlank { "Global" }, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.textPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Campaign Dates:", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary)
                            Text("${campaign.startDate} - ${campaign.endDate}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textBody)
                        }
                        if (campaign.cta.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Call-To-Action:", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary)
                                Text(campaign.cta, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ctaAccent)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    )
}

@Composable
private fun AnalyticsMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCardElevated),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Text(text = title, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = AppColors.textSecondary)
            }
            Text(
                text = value,
                fontFamily = InterFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
        }
    }
}
