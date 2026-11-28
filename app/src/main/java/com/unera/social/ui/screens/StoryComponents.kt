package com.unera.social.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.unera.social.ui.components.DiscussButton
import com.unera.social.ui.components.FacebookReactionButton
import com.unera.social.ui.components.FeatheredAvatar
import com.unera.social.ui.components.ShareBottomSheetDialog
import com.unera.social.ui.components.VerifiedBadge
import com.unera.social.ui.components.TextPostBackgroundView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Post
import com.unera.social.model.Story
import com.unera.social.model.StoryViewer
import com.unera.social.model.User
import com.unera.social.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.unera.social.data.PostsApiService

/**
 * StoryViewerDialog
 *
 * Fullscreen story viewer matching the web StoryViewer component in Storypage (824000-845000):
 * - Fixed dark backdrop #050B18
 * - Progress bar segments at the top for each story
 * - Header with author avatar, name, time ago, Follow button, mute toggle, viewers eye button, and close (X)
 * - Media display: text story with gradient background, photo/video with aspect fit/cover
 * - Tap left to go previous, tap right to go next, press & hold to pause
 * - Reaction pill tray (like, love, wow, haha, sad, angry)
 * - Bottom action bar: Heart reaction button with count, Comment/Discussion button with count, Share button with count
 * - Viewers sheet when creator clicks the view counter
 * - Discussions / quick reply input
 */
@Composable
fun StoryViewerDialog(
    story: Story,
    allStories: List<Story>,
    currentUser: User,
    onClose: () -> Unit,
    onNextStory: () -> Unit,
    onPrevStory: () -> Unit,
    onReact: (String) -> Unit,
    onDeleteStory: (() -> Unit)? = null,
    onToggleFollow: ((Long) -> Unit)? = null,
    onProfileClick: ((Long) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
    isAuthorFollowing: Boolean = story.author.isFollowing,
    storyViewers: List<StoryViewer> = emptyList(),
    onFetchViewers: (() -> Unit)? = null,
    onReportStory: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    // Current story index among allStories
    val currentIndex = allStories.indexOfFirst { it.id == story.id }.let { if (it >= 0) it else 0 }
    val isOwner = currentUser.id == story.author.id
    val isAdmin = currentUser.role.equals("admin", ignoreCase = true)

    var isPaused by remember { mutableStateOf(false) }
    var progress by remember(story.id) { mutableFloatStateOf(0f) }
    // Ensure story music starts unmuted for everyone, especially story owner and admin
    var isMuted by remember(story.id, isOwner, isAdmin) { mutableStateOf(false) }
    var showReactionsTray by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var localLiked by remember(story.id, story.myReaction) {
        mutableStateOf(story.myReaction != null || story.isLiked)
    }

    val effectivePaused = isPaused || showReactionsTray || showViewersSheet || showCommentSheet || showShareSheet || showMoreMenu

    val resolvedMusicUrl = remember(story.musicUrl) {
        val raw = story.musicUrl?.trim().orEmpty()
        when {
            raw.isBlank() -> ""
            raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true) ||
                raw.startsWith("content://", ignoreCase = true) ||
                raw.startsWith("file://", ignoreCase = true) -> raw
            raw.startsWith("/") -> "https://featheredsocial.site$raw"
            else -> "https://featheredsocial.site/$raw"
        }
    }

    // Real audio playback for story music via Media3 ExoPlayer
    val musicPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.stop()
            musicPlayer.release()
        }
    }

    LaunchedEffect(story.id, resolvedMusicUrl) {
        if (resolvedMusicUrl.isNotBlank()) {
            // Never start muted for owner or admin
            isMuted = false
            try {
                musicPlayer.stop()
                musicPlayer.setMediaItem(MediaItem.fromUri(resolvedMusicUrl))
                musicPlayer.prepare()
                if (story.musicStart > 0) {
                    musicPlayer.seekTo(story.musicStart * 1000L)
                }
                musicPlayer.volume = 1f
                musicPlayer.playWhenReady = !effectivePaused
            } catch (_: Exception) {
                // Ignore malformed audio URLs safely
            }
        } else {
            musicPlayer.stop()
            musicPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(effectivePaused, isMuted, resolvedMusicUrl) {
        if (resolvedMusicUrl.isNotBlank()) {
            musicPlayer.volume = if (isMuted) 0f else 1f
            musicPlayer.playWhenReady = !effectivePaused
        }
    }

    // Auto-advance progress timer (paused if held down or modal/menu open)
    LaunchedEffect(story.id, effectivePaused, resolvedMusicUrl) {
        if (!effectivePaused) {
            val stepMs = 50L
            val totalMs = if (resolvedMusicUrl.isNotBlank()) 15000L else 5000L
            val increment = stepMs.toFloat() / totalMs.toFloat()
            while (progress < 1f) {
                delay(stepMs)
                if (!effectivePaused) {
                    progress = (progress + increment).coerceAtMost(1f)
                }
            }
            if (progress >= 1f) {
                onNextStory()
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("story_viewer_dialog")
        ) {
            // Main Story Content Container (max-w 480dp on tablet, full on mobile)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp)
                    .align(Alignment.Center)
                    .background(Color.Black)
                    // Gesture detection: Left tap = prev, Right tap = next, Long press = pause
                    .pointerInput(story.id) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.35f) {
                                    onPrevStory()
                                } else if (offset.x > width * 0.65f) {
                                    onNextStory()
                                } else {
                                    isPaused = !isPaused
                                }
                            }
                        )
                    }
            ) {
                // 1. Story Media Display
                StoryMediaContent(
                    story = story,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient Overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom Gradient Overlay for action bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f),
                                    Color.Black
                                )
                            )
                        )
                )

                // 2. Story Progress Bars (one segment per story)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allStories.forEachIndexed { index, s ->
                        val segProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segProgress)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                // 3. Top Header Bar: Avatar, Name (20sp) + Follow on same line, Mute, Close (no shade, 21dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 26.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Author info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        FeatheredAvatar(
                            name = story.author.name,
                            username = story.author.username,
                            imageUrl = story.author.profileImageUrl,
                            size = 44.dp,
                            borderColor = Color(0xFF1877F2),
                            borderWidth = 2.dp,
                            fontSize = 16.sp,
                            onClick = {
                                isPaused = true
                                onProfileClick?.invoke(story.author.id)
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clickable {
                                            isPaused = true
                                            onProfileClick?.invoke(story.author.id)
                                        }
                                ) {
                                    Text(
                                        text = story.author.name,
                                        fontFamily = InterFontFamily,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (story.author.isVerified) {
                                        VerifiedBadge(size = 16.dp)
                                    }
                                }

                                // Follow button in the exact same horizontal line as Name
                                if (!isOwner && onToggleFollow != null) {
                                    val following = isAuthorFollowing || story.author.isFollowing
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onToggleFollow(story.author.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (following) Color(0xFF1E293B) else Color(0xFF1877F2)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("story_follow_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (following) Icons.Filled.Check else Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            tint = if (following) Color(0xFFF8FAFC) else Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (following) "Following" else "Follow",
                                            fontFamily = InterFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            color = if (following) Color(0xFFF8FAFC) else Color.White
                                        )
                                    }
                                }
                            }

                            Text(
                                text = PostsApiService.formatRelativeTime(story.timestamp),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Top-right controls: Mute toggle & unshaded 21dp Close (X) button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (resolvedMusicUrl.isNotBlank() || !story.musicTitle.isNullOrBlank()) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("story_mute_btn")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Close (X) button with no background shade, matching Follow button 34dp height
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_story_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // 4. Floating Music Pill (if story has music)
                if (!story.musicTitle.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 100.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = story.musicTitle ?: "",
                                fontFamily = InterFontFamily,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 5. Bottom Action Bar: React, Discuss, and Share matching FeedScreen exactly
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .navigationBarsPadding()
                ) {
                    // Interaction counts summary row
                    if (story.reactionsCount > 0 || story.commentsCount > 0 || story.sharesCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (story.reactionsCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1877F2),
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("❤️", fontFamily = InterFontFamily, fontSize = 10.sp)
                                        }
                                    }
                                    Text(
                                        text = "${formatCount(story.reactionsCount)} React..",
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (story.commentsCount > 0) {
                                    Text(
                                        text = "${formatCount(story.commentsCount)} Discuss..",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.clickable {
                                            isPaused = true
                                            showCommentSheet = true
                                        }
                                    )
                                }
                                if (story.sharesCount > 0) {
                                    Text(
                                        text = "${formatCount(story.sharesCount)} Shares",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.clickable {
                                            isPaused = true
                                            showShareSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.14f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Main Action Row: React, Discuss, Share grouped closely on left (matching FeedScreen),
                    // with Three White Bold Dots menu on right corner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FacebookReactionButton(
                                isLiked = localLiked,
                                myReaction = story.myReaction,
                                likesCount = story.reactionsCount,
                                onLike = {
                                    localLiked = !localLiked
                                    onReact("like")
                                },
                                onReact = { reactionType ->
                                    localLiked = true
                                    onReact(reactionType)
                                },
                                showReactionDock = showReactionsTray,
                                onToggleReactionDock = { showReactionsTray = it },
                                fontSize = 19.sp,
                                fontFamily = InterFontFamily,
                                unlikedTint = Color.White,
                                modifier = Modifier.testTag("story_react_btn")
                            )

                            DiscussButton(
                                commentCount = story.commentsCount,
                                onClick = {
                                    isPaused = true
                                    showCommentSheet = true
                                },
                                testTag = "story_discuss_btn",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                defaultTint = Color.White
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isPaused = true
                                        showShareSheet = true
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .testTag("story_share_btn"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(rotationZ = -35f)
                                )
                                if (story.sharesCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatCount(story.sharesCount),
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Three White Bold Dots Menu on right corner
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { showMoreMenu = true }
                                    .testTag("story_more_menu_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = Color(0xFF18181B),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Views (${story.viewsCount})",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        isPaused = true
                                        if (isAdmin) {
                                            onFetchViewers?.invoke()
                                        }
                                        showViewersSheet = true
                                    },
                                    modifier = Modifier.testTag("story_menu_views")
                                )

                                if ((isOwner || isAdmin) && onDeleteStory != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF43F5E),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Delete",
                                                    color = Color(0xFFF43F5E),
                                                    fontSize = 14.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onDeleteStory()
                                        },
                                        modifier = Modifier.testTag("story_menu_delete")
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Report",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 14.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onReportStory?.invoke("Inappropriate content")
                                        Toast.makeText(
                                            context,
                                            "Thank you. This report has been submitted to moderators.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("story_menu_report")
                                )
                            }
                        }
                    }
                }
            }

            // 6. Story Viewers Modal Sheet (names & reactions visible to admin only; non-admin sees count)
            if (showViewersSheet) {
                val resolvedViewers = remember(storyViewers, story.viewers, isAdmin) {
                    if (!isAdmin) {
                        emptyList()
                    } else if (storyViewers.isNotEmpty()) {
                        storyViewers
                    } else {
                        story.viewers.map { u ->
                            StoryViewer(
                                id = u.id,
                                storyId = story.id,
                                userId = u.id,
                                name = u.name,
                                username = u.username,
                                profileImageUrl = u.profileImageUrl,
                                isVerified = u.isVerified,
                                role = u.role
                            )
                        }
                    }
                }
                StoryViewersModal(
                    viewers = resolvedViewers,
                    totalViews = if (isAdmin) maxOf(story.viewsCount, resolvedViewers.size) else story.viewsCount,
                    isAdmin = isAdmin,
                    onDismiss = {
                        showViewersSheet = false
                        isPaused = false
                    },
                    onProfileClick = onProfileClick
                )
            }

            // 7. Story Comments / Discussion Modal Sheet
            if (showCommentSheet) {
                StoryDiscussionsModal(
                    story = story,
                    currentUser = currentUser,
                    onDismiss = {
                        showCommentSheet = false
                        isPaused = false
                    }
                )
            }

            // 8. Story Share Bottom Sheet Modal
            if (showShareSheet) {
                val sharePostProxy = remember(story) {
                    Post(
                        id = story.id,
                        userId = story.author.id,
                        author = story.author,
                        content = story.textContent,
                        timestamp = story.timestamp,
                        mediaUrl = story.mediaUrl.ifBlank { null },
                        backgroundStyle = story.backgroundStyle,
                        likesCount = story.reactionsCount,
                        commentsCount = story.commentsCount,
                        sharesCount = story.sharesCount
                    )
                }
                ShareBottomSheetDialog(
                    post = sharePostProxy,
                    onDismiss = {
                        showShareSheet = false
                        isPaused = false
                    },
                    onShareToFeed = {
                        showShareSheet = false
                        isPaused = false
                        onShare?.invoke("feed")
                    },
                    onShareToGroup = {
                        showShareSheet = false
                        isPaused = false
                        onShare?.invoke("group")
                    },
                    onSendMessage = {
                        showShareSheet = false
                        isPaused = false
                        onShare?.invoke("message")
                    },
                    onCopyLink = {
                        showShareSheet = false
                        isPaused = false
                        onShare?.invoke("copy")
                    }
                )
            }
        }
    }
}

/**
 * Story Media View: Handles Text stories (with vibrant background) or Photo/Video stories
 */
@Composable
private fun StoryMediaContent(
    story: Story,
    modifier: Modifier = Modifier
) {
    if (story.type == "text" || story.mediaUrl.isBlank()) {
        TextPostBackgroundView(
            backgroundStyle = story.backgroundStyle,
            content = story.textContent.ifBlank { "Sharing moments on Unera ✨" },
            modifier = modifier.fillMaxSize(),
            isVisible = true,
            minHeight = 0.dp,
            defaultFontSizeSp = 28,
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 104.dp)
        )
    } else {
        // Pure black dark cinema backdrop (matching Videos / ReelsScreen)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = "Story Media",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Story Viewers Modal Sheet
 * Matches web Viewers Dialog in StoryViewer.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewersModal(
    viewers: List<StoryViewer>,
    totalViews: Int,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onProfileClick: ((Long) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isAdmin) "Story Viewers ($totalViews)" else "Views ($totalViews)",
                        fontFamily = InterFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAdmin) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (totalViews == 1) "1 View" else "$totalViews Views",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Only story admins can see viewer names and reactions.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (viewers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No viewers yet. Viewers and reactions will appear here.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(viewers, key = { it.userId }) { viewer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismiss()
                                    onProfileClick?.invoke(viewer.userId)
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatheredAvatar(
                                name = viewer.name,
                                username = viewer.username,
                                imageUrl = viewer.profileImageUrl,
                                size = 44.dp,
                                fontSize = 16.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = viewer.name,
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (viewer.isVerified) {
                                        VerifiedBadge(size = 13.dp)
                                    }
                                }
                                Text(
                                    text = "@${viewer.username}",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (!viewer.reaction.isNullOrBlank()) {
                                val emoji = when (viewer.reaction.lowercase()) {
                                    "like" -> "👍"
                                    "love" -> "❤️"
                                    "care" -> "🤗"
                                    "haha" -> "😂"
                                    "wow" -> "😮"
                                    "sad" -> "😢"
                                    "angry" -> "😡"
                                    else -> "❤️"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "$emoji ${viewer.reaction.replaceFirstChar { it.uppercase() }}",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Viewed",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Story Discussions & Comments Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDiscussionsModal(
    story: Story,
    currentUser: User,
    onDismiss: () -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    var commentsList by remember {
        mutableStateOf(
            listOf(
                Triple("Amina Mwangi", "This looks incredible! Where was this taken? 🔥", "10m ago"),
                Triple("Sam Adebayo", "Keep soaring brother, inspired by this! 🚀", "35m ago")
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Discussions (${commentsList.size})",
                    fontFamily = InterFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(commentsList) { (author, text, time) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeatheredAvatar(
                            name = author,
                            imageUrl = null,
                            size = 34.dp,
                            fontSize = 13.sp
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF1C1C1C), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = author, fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = time, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = text, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add comment input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                    placeholder = { Text("Write a comment…", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1C),
                        unfocusedContainerColor = Color(0xFF1C1C1C),
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = Color(0xFF2E2E2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            commentsList = commentsList + Triple(currentUser.name, newCommentText, "Just now")
                            newCommentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
