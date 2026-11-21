package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.Post
import com.unera.social.model.User
import com.unera.social.ui.components.AdPreview
import com.unera.social.ui.components.AdPreviewData
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

val CTA_OPTIONS = listOf(
    "Learn More", "Sign Up", "Subscribe", "Shop Now",
    "Contact Us", "Call Now", "Email Us", "WhatsApp",
    "Download", "Get Quote", "Book Now"
)

enum class ContactMethodType(val label: String, val icon: ImageVector, val placeholder: String) {
    LINK("Website Link", Icons.Default.Link, "https://example.com"),
    PHONE("Phone Number", Icons.Default.Phone, "+255 123 456 789"),
    EMAIL("Email Address", Icons.Default.Email, "contact@example.com")
}

/**
 * AdCreator - Matches web AdCreator.tsx exactly.
 * 4-Step campaign builder: Post Selection -> Details Configuration -> Review -> Launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdCreator(
    currentUser: User,
    userPosts: List<Post>,
    initialPost: Post? = null,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(if (initialPost != null) 2 else 1) }
    var selectedPost by remember { mutableStateOf<Post?>(initialPost) }
    var displayMode by remember { mutableStateOf("grid") } // "grid" or "list"
    var contactType by remember { mutableStateOf(ContactMethodType.LINK) }

    // Form data
    var campaignName by remember {
        mutableStateOf(initialPost?.content?.take(30) ?: "New Campaign")
    }
    var contactValue by remember { mutableStateOf("https://example.com") }
    var selectedCta by remember { mutableStateOf("Learn More") }
    var targetLocation by remember { mutableStateOf("Global") }
    var dailyBudget by remember { mutableFloatStateOf(5f) }
    var durationDays by remember { mutableFloatStateOf(7f) }
    var isSubmitting by remember { mutableStateOf(false) }

    val currentPostMedia = remember(selectedPost) {
        val p = selectedPost
        when {
            p == null -> emptyList()
            p.mediaUrls.isNotEmpty() -> p.mediaUrls
            !p.mediaUrl.isNullOrBlank() -> listOf(p.mediaUrl)
            else -> emptyList()
        }
    }

    val adType = if (selectedPost?.postType == "video" || selectedPost?.postType == "reel") "video" else "image"

    val previewData = AdPreviewData(
        name = campaignName,
        type = adType,
        mediaUrl = currentPostMedia.firstOrNull(),
        mediaUrls = currentPostMedia,
        description = selectedPost?.content ?: "",
        cta = selectedCta,
        link = if (contactType == ContactMethodType.LINK) contactValue else null,
        phone = if (contactType == ContactMethodType.PHONE) contactValue else null,
        email = if (contactType == ContactMethodType.EMAIL) contactValue else null
    )

    // Auto transition on Step 4
    LaunchedEffect(step) {
        if (step == 4) {
            delay(1800)
            onSuccess()
        }
    }

    val promoGreen = if (AppColors.isLight) Color(0xFF059669) else Color(0xFF34D399)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("ad_creator_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header with Back Button and Steps Indicator
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.headerBg)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (step > 1) {
                                        step -= 1
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.headerIconBg)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go Back",
                                    tint = AppColors.headerIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (initialPost != null) "Boost Your Post" else "Create New Campaign",
                                    fontFamily = InterFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.headerText
                                )
                                Text(
                                    text = if (initialPost != null) "Configure your ad settings below"
                                    else "Boost your posts and reach more people for FREE",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = AppColors.headerSubtext
                                )
                            }

                            // Steps Circles: 1, 2, 3, 4
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..4).forEach { s ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    step == s -> if (AppColors.isLight) Color.White else AppColors.primaryBlue
                                                    step > s -> Color(0xFF10B981)
                                                    else -> AppColors.headerIconBg
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (step > s) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = s.toString(),
                                                fontFamily = InterFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when {
                                                    step == s -> if (AppColors.isLight) AppColors.primaryBlue else Color.White
                                                    else -> if (AppColors.isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = AppColors.headerDivider, thickness = 1.dp)

                    // Free Promotion Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF10B981).copy(alpha = if (AppColors.isLight) 0.12f else 0.15f),
                                        AppColors.primaryBlue.copy(alpha = if (AppColors.isLight) 0.10f else 0.15f)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = promoGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Free Promotion on Feathered",
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = promoGreen
                                )
                                Text(
                                    text = "All ad campaigns are completely free. No costs, no hidden fees!",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = AppColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // STEP 1: Select a post to boost
            // ==========================================
            if (step == 1) {
                item(key = "step_1_controls") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Select a post to boost",
                                fontFamily = InterFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.bgCard)
                                    .border(1.dp, AppColors.border, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { displayMode = "grid" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (displayMode == "grid") AppColors.primaryBlue else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Grid",
                                        tint = if (displayMode == "grid") Color.White else AppColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { displayMode = "list" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (displayMode == "list") AppColors.primaryBlue else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "List",
                                        tint = if (displayMode == "list") Color.White else AppColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (userPosts.isEmpty()) {
                    item(key = "no_posts_view") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = AppColors.textMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No posts available to boost",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else if (displayMode == "grid") {
                    item(key = "posts_grid_view") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            userPosts.chunked(2).forEach { rowPosts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowPosts.forEach { post ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            PostSelectionCard(
                                                post = post,
                                                onSelect = {
                                                    selectedPost = post
                                                    campaignName = post.content.take(30).ifBlank { "New Campaign" }
                                                    step = 2
                                                }
                                            )
                                        }
                                    }
                                    if (rowPosts.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // List mode
                    items(userPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            PostSelectionRow(
                                post = post,
                                onSelect = {
                                    selectedPost = post
                                    campaignName = post.content.take(30).ifBlank { "New Campaign" }
                                    step = 2
                                }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // STEP 2: Configure Campaign Settings
            // ==========================================
            if (step == 2 && selectedPost != null) {
                item(key = "step_2_form") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Selected Post Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.bgBlueTint)
                                .border(1.dp, AppColors.primaryBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.bgCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentPostMedia.isNotEmpty()) {
                                    AsyncImage(
                                        model = currentPostMedia.first(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = AppColors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected Post",
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.primaryBlue
                                )
                                Text(
                                    text = selectedPost?.content?.ifBlank { "Post media" } ?: "",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = AppColors.textBody,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            TextButton(onClick = { step = 1 }) {
                                Text("Change", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.primaryBlue, fontSize = 13.sp)
                            }
                        }

                        // Campaign Name
                        Column {
                            Text(
                                text = "Campaign Name",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = campaignName,
                                onValueChange = { campaignName = it },
                                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                                placeholder = { Text("e.g. Summer Sale 2026", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textMuted) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgCard,
                                    unfocusedContainerColor = AppColors.bgCard,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Contact Method Selection
                        Column {
                            Text(
                                text = "Contact Method",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ContactMethodType.values().forEach { method ->
                                    val isSel = contactType == method
                                    Surface(
                                        onClick = {
                                            contactType = method
                                            contactValue = method.placeholder
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) AppColors.primaryBlue else AppColors.bgCard,
                                        border = BorderStroke(1.dp, if (isSel) AppColors.primaryBlue else AppColors.border),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = method.icon,
                                                contentDescription = null,
                                                tint = if (isSel) Color.White else AppColors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = method.label.split(" ").first(),
                                                fontFamily = InterFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSel) Color.White else AppColors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = contactValue,
                                onValueChange = { contactValue = it },
                                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                                placeholder = { Text(contactType.placeholder, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textMuted) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgCard,
                                    unfocusedContainerColor = AppColors.bgCard,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Call to Action Dropdown Selector
                        Column {
                            Text(
                                text = "Call to Action",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            var ctaExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = ctaExpanded,
                                onExpandedChange = { ctaExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedCta,
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ctaExpanded) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = AppColors.bgCard,
                                        unfocusedContainerColor = AppColors.bgCard,
                                        focusedTextColor = AppColors.textPrimary,
                                        unfocusedTextColor = AppColors.textPrimary,
                                        focusedBorderColor = AppColors.primaryBlue,
                                        unfocusedBorderColor = AppColors.borderStrong
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = ctaExpanded,
                                    onDismissRequest = { ctaExpanded = false },
                                    modifier = Modifier.background(AppColors.bgCard)
                                ) {
                                    CTA_OPTIONS.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary) },
                                            onClick = {
                                                selectedCta = opt
                                                ctaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Target Location
                        Column {
                            Text(
                                text = "Target Location",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = targetLocation,
                                onValueChange = { targetLocation = it },
                                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                                placeholder = { Text("e.g. Tanzania, Nairobi, Global", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textMuted) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgCard,
                                    unfocusedContainerColor = AppColors.bgCard,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Sliders: Budget & Duration
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Budget slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Daily Budget ($)", fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.bgCard)
                                            .border(1.dp, AppColors.border, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("$${dailyBudget.toInt()}", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                                Slider(
                                    value = dailyBudget,
                                    onValueChange = { dailyBudget = it },
                                    valueRange = 1f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppColors.primaryBlue,
                                        activeTrackColor = AppColors.primaryBlue,
                                        inactiveTrackColor = AppColors.bgChip
                                    )
                                )
                                Text("✨ Free promotion - No actual charges", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = promoGreen, fontSize = 12.sp)
                            }

                            // Duration slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Duration (Days)", fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.bgCard)
                                            .border(1.dp, AppColors.border, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("${durationDays.toInt()}d", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                                Slider(
                                    value = durationDays,
                                    onValueChange = { durationDays = it },
                                    valueRange = 1f..30f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppColors.primaryBlue,
                                        activeTrackColor = AppColors.primaryBlue,
                                        inactiveTrackColor = AppColors.bgChip
                                    )
                                )
                            }
                        }

                        // Total Campaign Cost Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = if (AppColors.isLight) 0.10f else 0.12f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Total Campaign Cost", fontFamily = InterFontFamily, fontSize = 13.sp, color = promoGreen, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$0.00", fontFamily = InterFontFamily, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("✨ Completely free on Feathered", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = promoGreen)
                            }
                        }

                        // Live Ad Preview Section
                        Column {
                            Text(
                                text = "Live Ad Preview",
                                fontFamily = InterFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                AdPreview(
                                    data = previewData,
                                    advertiserName = currentUser.name,
                                    advertiserAvatar = currentUser.profileImageUrl,
                                    isVerified = currentUser.isVerified,
                                    isFullView = true
                                )
                            }
                        }

                        // Buttons: Back and Review
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { step = 1 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.bgChip, contentColor = AppColors.textPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Back", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary)
                            }

                            Button(
                                onClick = { step = 3 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                                modifier = Modifier
                                    .weight(2f)
                                    .height(48.dp)
                            ) {
                                Text("Review Campaign", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // STEP 3: Review Campaign
            // ==========================================
            if (step == 3 && selectedPost != null) {
                item(key = "step_3_review") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Review Your Campaign",
                                fontFamily = InterFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Review your ad settings before launching.",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = AppColors.textSecondary
                            )
                        }

                        AdPreview(
                            data = previewData,
                            advertiserName = currentUser.name,
                            advertiserAvatar = currentUser.profileImageUrl,
                            isVerified = currentUser.isVerified,
                            isFullView = true
                        )

                        // Campaign Summary Card matching web
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                            border = BorderStroke(1.dp, AppColors.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SummaryRow(label = "Campaign Name", value = campaignName)
                                SummaryRow(label = "Contact Method", value = contactValue)
                                SummaryRow(
                                    label = "Budget & Duration",
                                    value = "$${dailyBudget.toInt()}/day • ${durationDays.toInt()} days"
                                )
                                SummaryRow(label = "Target Location", value = targetLocation)
                                SummaryRow(label = "Call to Action", value = selectedCta)

                                HorizontalDivider(color = AppColors.border, thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Cost", fontFamily = InterFontFamily, fontSize = 12.sp, color = AppColors.textSecondary, fontWeight = FontWeight.SemiBold)
                                        Text("$0.00", fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary)
                                    }
                                    Text("✨ Free promotion on Feathered", fontFamily = InterFontFamily, color = promoGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Action Buttons: Edit Details & Launch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { step = 2 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.bgChip, contentColor = AppColors.textPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text("Edit Details", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary)
                            }

                            Button(
                                onClick = {
                                    isSubmitting = true
                                    onCreateCampaign(
                                        selectedPost?.id,
                                        campaignName,
                                        adType,
                                        currentPostMedia.firstOrNull(),
                                        currentPostMedia,
                                        selectedPost?.content ?: "",
                                        if (contactType == ContactMethodType.LINK) contactValue else null,
                                        if (contactType == ContactMethodType.PHONE) contactValue else null,
                                        if (contactType == ContactMethodType.EMAIL) contactValue else null,
                                        selectedCta,
                                        targetLocation,
                                        dailyBudget.toInt(),
                                        durationDays.toInt()
                                    )
                                    isSubmitting = false
                                    step = 4
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(2f)
                                    .height(50.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Launch Free Campaign", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // STEP 4: Campaign Launched Success
            // ==========================================
            if (step == 4) {
                item(key = "step_4_success") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Text(
                            text = "Campaign Launched!",
                            fontFamily = InterFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary
                        )

                        Text(
                            text = "Your free ad is now live and reaching people. Redirecting to dashboard...",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = AppColors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = InterFontFamily, fontSize = 12.sp, color = AppColors.textSecondary, fontWeight = FontWeight.SemiBold)
        Text(text = value, fontFamily = InterFontFamily, fontSize = 13.sp, color = AppColors.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PostSelectionCard(
    post: Post,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        border = BorderStroke(1.dp, AppColors.border)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(AppColors.bgCardElevated)
            ) {
                val mediaUrl = post.mediaUrls.firstOrNull() ?: post.mediaUrl
                if (!mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(28.dp))
                    }
                }

                if (post.postType == "video" || post.postType == "reel") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Video", fontFamily = InterFontFamily, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = post.content.ifBlank { "Post update" },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = AppColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(post.timestamp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = AppColors.textSecondary)
                    Text("❤️ ${post.likesCount}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = AppColors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PostSelectionRow(
    post: Post,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        border = BorderStroke(1.dp, AppColors.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.bgCardElevated)
            ) {
                val mediaUrl = post.mediaUrls.firstOrNull() ?: post.mediaUrl
                if (!mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.content.ifBlank { "Post update" },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = AppColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${post.timestamp} • ${post.likesCount} reactions",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}
