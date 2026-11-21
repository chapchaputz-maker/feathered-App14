package com.unera.social.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.Post
import com.unera.social.model.Story
import com.unera.social.model.User
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import java.util.Calendar

// ============================================================
// 1. SUGGESTED PROFILES PAGE (Discover People)
// Matches web SuggestedProfilesPage in MenuPages.tsx
// ============================================================
data class SuggestedUserItem(
    val user: User,
    val score: Int,
    val reason: String
)

@Composable
fun SuggestedProfilesPage(
    currentUser: User,
    users: List<User>,
    onFollow: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var hiddenUserIds by remember { mutableStateOf(setOf<Long>()) }

    val availableUsers = remember(users, currentUser, hiddenUserIds) {
        users.filter { u ->
            u.id != currentUser.id && !hiddenUserIds.contains(u.id)
        }.map { u ->
            var score = 0
            val reason = if (u.location == currentUser.location) {
                score += 5
                "Near you in ${u.location.split(",").firstOrNull()?.trim() ?: u.location}"
            } else {
                "Suggested for you"
            }
            SuggestedUserItem(user = u, score = score, reason = reason)
        }.sortedByDescending { it.score }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("suggested_profiles_page")
    ) {
        // Header matching web: flex items-center gap-3 mb-6
        Surface(
            color = AppColors.headerBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColors.headerIconBg)
                                .testTag("discover_people_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppColors.headerIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = "Discover People",
                        fontSize = 24.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.headerText
                    )
                }
                HorizontalDivider(color = AppColors.headerDivider)
            }
        }

        if (availableUsers.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(availableUsers, key = { it.user.id }) { item ->
                    val user = item.user
                    val isFollowed = user.isFollowing

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = if (AppColors.isLight) {
                                                listOf(AppColors.headerBg, Color(0xFF2563EB))
                                            } else {
                                                listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                                            }
                                        )
                                    )
                            ) {
                                if (user.coverImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.coverImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .offset(x = 16.dp, y = 28.dp)
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.bgCard)
                                        .border(4.dp, AppColors.bgCard, CircleShape)
                                        .clickable { onProfileClick(user.id) }
                                ) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = user.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 16.dp)
                            ) {
                                Text(
                                    text = user.name,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = AppColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clickable { onProfileClick(user.id) }
                                )

                                Text(
                                    text = "@${user.username}",
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.reason,
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { onFollow(user.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowed) AppColors.bgChip else AppColors.primaryBlue,
                                        contentColor = if (isFollowed) AppColors.textPrimary else Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("follow_user_${user.id}")
                                ) {
                                    Text(
                                        text = if (isFollowed) "Following" else "Follow",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Text(
                    text = "No new suggestions at the moment.",
                    color = AppColors.textSecondary,
                    fontSize = 15.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================================================
// 2. BIRTHDAYS PAGE
// Matches web BirthdaysPage in MenuPages.tsx
// ============================================================
@Composable
fun BirthdaysPage(
    currentUser: User,
    users: List<User>,
    onMessage: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance()
    val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12
    val currentDay = cal.get(Calendar.DAY_OF_MONTH) // 1-31

    val isBirthdayToday: (String?) -> Boolean = { dateStr ->
        if (dateStr.isNullOrBlank()) false
        else {
            val parts = dateStr.trim().split("-")
            if (parts.size >= 3) {
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                m == currentMonth && d == currentDay
            } else false
        }
    }

    val todayStars = remember(users, currentUser) {
        users.filter { it.id != currentUser.id && isBirthdayToday(it.birthDate) }
    }

    val upcomingBirthdays = remember(users, currentUser, todayStars) {
        users.filter { it.id != currentUser.id && it !in todayStars && !it.birthDate.isNullOrBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("birthdays_page")
    ) {
        // Header matching web: flex items-center gap-4 mb-8
        Surface(
            color = AppColors.headerBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColors.headerIconBg)
                                .testTag("birthdays_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppColors.headerIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .rotate(-3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF0080), Color(0xFF7928CA))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Birthdays",
                            fontSize = 26.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.headerText,
                            lineHeight = 30.sp
                        )
                        Text(
                            text = "Celebrate special moments with your community.",
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.headerSubtext
                        )
                    }
                }
                HorizontalDivider(color = AppColors.headerDivider)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section 1: Today's Stars
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Today's Stars",
                        fontSize = 20.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3425F)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (todayStars.isNotEmpty()) {
                items(todayStars, key = { it.id }) { star ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = AppColors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF1877F2),
                                                Color(0xFFF3425F),
                                                Color(0xFFFAB400),
                                                Color(0xFF1877F2)
                                            )
                                        )
                                    )
                                    .padding(4.dp)
                                    .clickable { onProfileClick(star.id) }
                            ) {
                                AsyncImage(
                                    model = star.profileImageUrl,
                                    contentDescription = star.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(3.dp, AppColors.bgCard, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = star.name,
                                fontSize = 20.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimary,
                                modifier = Modifier.clickable { onProfileClick(star.id) }
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = star.location,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary
                                )
                            }

                            Button(
                                onClick = { onMessage(star.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.primaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("wish_birthday_${star.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cake,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wish Him/Her 🎉",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = AppColors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = AppColors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Birthdays Today",
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check back tomorrow or see upcoming birthdays below.",
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Section 2: Upcoming Birthdays
            if (upcomingBirthdays.isNotEmpty()) {
                item {
                    Text(
                        text = "Upcoming Birthdays",
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(upcomingBirthdays, key = { it.id }) { u ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onProfileClick(u.id) }
                            ) {
                                AsyncImage(
                                    model = u.profileImageUrl,
                                    contentDescription = u.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, AppColors.primaryBlue.copy(alpha = 0.3f), CircleShape)
                                )

                                Column {
                                    Text(
                                        text = u.name,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AppColors.textPrimary
                                    )
                                    Text(
                                        text = u.birthDate ?: "",
                                        fontSize = 12.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.textSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onMessage(u.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.bgBlueTint)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Message",
                                    tint = AppColors.primaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 3. SAVED POSTS PAGE
// Note: SavedPostsPage is implemented with full 1:1 parity in
// com.unera.social.ui.screens.SavedPostsPage.kt
// ============================================================

// ============================================================
// 4. MEMORIES PAGE
// Matches web MemoriesPage in MenuPages.tsx
// ============================================================
@Composable
fun MemoriesPage(
    currentUser: User,
    posts: List<Post>,
    onLikePost: (Long) -> Unit,
    onCommentPost: (Post) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val memoryPosts = remember(posts, currentUser) {
        posts.filter { it.author.id == currentUser.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("memories_page")
    ) {
        Surface(
            color = AppColors.headerBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColors.headerIconBg)
                                .testTag("memories_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppColors.headerIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (AppColors.isLight) AppColors.headerIconBg else Color(0xFF818CF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (AppColors.isLight) Color.White else Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Memories",
                            fontSize = 20.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.headerText
                        )
                        Text(
                            text = "On this day in previous years",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.headerSubtext
                        )
                    }
                }
                HorizontalDivider(color = AppColors.headerDivider)
            }
        }

        if (memoryPosts.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AppColors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = AppColors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Memories Found",
                            fontSize = 18.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When you share posts and moments, your memories will appear here.",
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(memoryPosts, key = { it.id }) { post ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "1 year ago today",
                                        fontSize = 12.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (AppColors.isLight) Color(0xFF4F46E5) else Color(0xFF818CF8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = post.content,
                                fontSize = 15.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textPrimary
                            )

                            if (post.mediaUrl != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = post.mediaUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 5. STORY FEED PAGE
// Matches web StoryFeedPage in MenuPages.tsx
// ============================================================
@Composable
fun StoryFeedPage(
    currentUser: User,
    stories: List<Story>,
    onBack: (() -> Unit)? = null,
    onAddStoryClick: (() -> Unit)? = null,
    onStoryClick: ((Story) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("story_feed_page")
    ) {
        Surface(
            color = AppColors.headerBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.headerIconBg)
                                    .testTag("story_feed_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (AppColors.isLight) AppColors.headerIconBg else Color(0xFF34D399).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (AppColors.isLight) Color.White else Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Story Feed",
                            fontSize = 20.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.headerText
                        )
                    }

                    if (onAddStoryClick != null) {
                        Button(
                            onClick = onAddStoryClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                                contentColor = if (AppColors.isLight) AppColors.primaryBlue else Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("story_feed_add_story_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (AppColors.isLight) AppColors.primaryBlue else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Story",
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = if (AppColors.isLight) AppColors.primaryBlue else Color.White
                            )
                        }
                    }
                }
                HorizontalDivider(color = AppColors.headerDivider)
            }
        }

        if (stories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AppColors.bgBlueTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.primaryBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = "No Stories Yet",
                        fontSize = 20.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )

                    Text(
                        text = "Share a photo, video (up to 90s), or text story with your friends and followers!",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (onAddStoryClick != null) {
                        Button(
                            onClick = onAddStoryClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Story", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stories, key = { it.id }) { story ->
                    var commentText by remember { mutableStateOf("") }
                    var selectedEmoji by remember { mutableStateOf<String?>(null) }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AppColors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStoryClick?.invoke(story) }
                    ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = story.author.profileImageUrl,
                                contentDescription = story.author.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, AppColors.primaryBlue, CircleShape)
                            )
                            Column {
                                Text(
                                    text = story.author.name,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = story.timestamp,
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AsyncImage(
                            model = story.mediaUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Reaction emojis: 👍 ❤️ 🔥 😂
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("👍" to "Like", "❤️" to "Love", "🔥" to "Fire", "😂" to "Haha").forEach { (emoji, label) ->
                                val isSelected = selectedEmoji == emoji
                                Surface(
                                    onClick = { selectedEmoji = if (isSelected) null else emoji },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) AppColors.bgBlueTint else AppColors.bgInput,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AppColors.primaryBlue else AppColors.border
                                    ),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "$emoji $label",
                                        fontSize = 13.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = if (isSelected) AppColors.primaryBlue else AppColors.textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reply field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                                placeholder = { Text("Reply to story...", fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.border,
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary
                                )
                            )

                            IconButton(
                                onClick = { commentText = "" },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.primaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send reply",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}



