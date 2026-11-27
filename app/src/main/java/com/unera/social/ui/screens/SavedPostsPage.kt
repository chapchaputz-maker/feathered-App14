package com.unera.social.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.unera.social.R
import com.unera.social.ui.components.GalleryViewerDialog
import com.unera.social.ui.components.ReactionsSheetDialog
import com.unera.social.ui.components.ShareBottomSheetDialog
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.*
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun savedAccentGold(): Color = if (AppColors.isLight) Color(0xFFD97706) else Color(0xFFF59E0B)

@Composable
private fun savedTabActive(): Color = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)

private val LikeRed = Color(0xFFEF4444)

/**
 * 1:1 Implementation of web's SavedPostsPage component.
 * Features:
 * - Dynamic Feed / Grid view modes
 * - 5-item pagination with "See More" animated button
 * - Video & regular post card rendering with author metadata
 * - Grid cards with video badges, center play buttons, discuss and remove actions
 * - Interactive Lightbox image viewer
 * - Interactive Video preview modal
 * - Interactive Comments modal dialog
 * - Local-first empty state with "Explore Posts" navigation
 */
@Composable
fun SavedPostsPage(
    currentUser: User?,
    users: List<User> = emptyList(),
    posts: List<Post> = emptyList(),
    reels: List<Reel> = emptyList(),
    onBack: (() -> Unit)? = null,
    onProfileClick: ((Long) -> Unit)? = null,
    onVideoClick: ((Reel) -> Unit)? = null,
    onViewImage: ((String) -> Unit)? = null,
    onOpenComments: ((Post) -> Unit)? = null,
    onReact: ((Long, String) -> Unit)? = null,
    onLikePost: ((Long) -> Unit)? = null,
    onCommentPost: ((Post) -> Unit)? = null,
    onSavePost: ((Long) -> Unit)? = null,
    onSaveReel: ((Long) -> Unit)? = null,
    onShare: ((Post) -> Unit)? = null,
    onFollowUser: ((Long) -> Unit)? = null,
    onOpenProduct: ((Product) -> Unit)? = null,
    onOpenEvent: ((Event) -> Unit)? = null,
    onOpenGroup: ((Long) -> Unit)? = null,
    onPlayMusic: ((Song) -> Unit)? = null,
    onOpenVideo: ((Post) -> Unit)? = null,
    onDeletePost: ((Long) -> Unit)? = null,
    onEditPost: ((Long, String) -> Unit)? = null,
    onHidePost: ((Long) -> Unit)? = null,
    onReportPost: ((Long, String) -> Unit)? = null,
    onLikeComment: ((Long, Long) -> Unit)? = null,
    onRsvpEvent: ((Long, String, String) -> Unit)? = null,
    onRequestAuth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("feed") } // "feed" or "grid"
    val pageSize = 5
    var visibleCount by remember { mutableIntStateOf(pageSize) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val feedListState = rememberLazyListState()

    // Modals & Dialogs state
    var lightboxImageUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoPreview by remember { mutableStateOf<SavedPostItem?>(null) }
    var activeCommentingItem by remember { mutableStateOf<SavedPostItem?>(null) }
    var newCommentText by remember { mutableStateOf("") }
    var localRemovedIds by remember { mutableStateOf(setOf<Long>()) }
    var galleryState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var activePostForGallery by remember { mutableStateOf<Post?>(null) }
    var reactionsSheetPost by remember { mutableStateOf<Post?>(null) }
    var shareSheetPost by remember { mutableStateOf<Post?>(null) }

    val resolvedCurrentUser = currentUser ?: User(
        id = 0L,
        name = "Guest",
        username = "guest",
        profileImageUrl = ""
    )

    // Combine saved posts and saved reels (deduplicating reel IDs already present as saved posts)
    val allSavedItems = remember(posts, reels, localRemovedIds) {
        val savedPostItems = posts
            .filter { it.isSaved && it.id !in localRemovedIds }
            .map { post ->
                val isVid = post.postType == "video" || post.postType == "reel" ||
                    post.mediaType == "video" ||
                    (!post.mediaUrl.isNullOrBlank() && post.mediaUrl.contains("video", ignoreCase = true))
                SavedPostItem(
                    id = post.id,
                    type = if (isVid) "video" else "normal",
                    post = post
                )
            }

        val savedPostIds = savedPostItems.map { it.id }.toSet()
        val savedReelItems = reels
            .filter { it.isSaved && it.id !in savedPostIds && it.id !in localRemovedIds }
            .map { reel ->
                SavedPostItem(
                    id = reel.id,
                    type = "video",
                    reel = reel
                )
            }

        (savedPostItems + savedReelItems).sortedByDescending { it.savedAt }
    }

    val visibleItems = remember(allSavedItems, visibleCount) {
        allSavedItems.take(visibleCount)
    }
    val hasMore = visibleCount < allSavedItems.size

    val handleSeeMore = {
        isLoadingMore = true
        coroutineScope.launch {
            delay(250)
            visibleCount += pageSize
            isLoadingMore = false
        }
    }

    val handleRemove: (SavedPostItem) -> Unit = { item ->
        localRemovedIds = localRemovedIds + item.id
        if (item.post != null) {
            onSavePost?.invoke(item.id)
        } else if (item.reel != null) {
            onSaveReel?.invoke(item.id)
        } else {
            onSavePost?.invoke(item.id)
        }
    }

    val accentGold = savedAccentGold()
    val tabActive = savedTabActive()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("saved_posts_page")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = AppColors.headerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
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
                                        .testTag("saved_posts_back_btn")
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (AppColors.isLight) AppColors.headerIconBg else accentGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (AppColors.isLight) Color.White else accentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Saved Posts",
                                    fontSize = 20.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.headerText,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = "${allSavedItems.size} saved item${if (allSavedItems.size == 1) "" else "s"} • Stored locally",
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.headerSubtext
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (AppColors.isLight) Color.White.copy(alpha = 0.18f) else AppColors.bgCard)
                                .border(
                                    1.dp,
                                    if (AppColors.isLight) Color.White.copy(alpha = 0.3f) else AppColors.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewMode = "feed" },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == "feed") (if (AppColors.isLight) Color.White else AppColors.bgChip) else Color.Transparent)
                                    .testTag("saved_posts_feed_view_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                                    contentDescription = "Feed view",
                                    tint = if (viewMode == "feed") tabActive else if (AppColors.isLight) Color.White else AppColors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewMode = "grid" },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == "grid") (if (AppColors.isLight) Color.White else AppColors.bgChip) else Color.Transparent)
                                    .testTag("saved_posts_grid_view_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Grid view",
                                    tint = if (viewMode == "grid") tabActive else if (AppColors.isLight) Color.White else AppColors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = AppColors.headerDivider)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 700.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
            ) {

            // Main Content Area
            if (allSavedItems.isEmpty()) {
                // Empty state matching web: rounded-2xl p-8 sm:p-12 text-center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppColors.bgInput)
                                    .border(1.dp, AppColors.border, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = accentGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No saved posts yet",
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Tap the bookmark icon in the action bar of any post or reel to save it here. Your saved items are preserved locally on this device.",
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (onBack != null) {
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.primaryBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Explore Posts",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (viewMode == "grid") {
                    // Grid View: 2 columns (adaptive on wide displays)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(visibleItems, key = { "grid-${it.id}" }) { item ->
                            SavedGridItemCard(
                                item = item,
                                onCardClick = {
                                    if (item.type == "video") {
                                        if (item.reel != null && onVideoClick != null) {
                                            onVideoClick(item.reel)
                                        } else {
                                            activeVideoPreview = item
                                        }
                                    } else {
                                        val media = item.mediaUrl
                                        if (!media.isNullOrBlank()) {
                                            lightboxImageUrl = media
                                            onViewImage?.invoke(media)
                                        } else {
                                            activeCommentingItem = item
                                            if (item.post != null) onOpenComments?.invoke(item.post)
                                        }
                                    }
                                },
                                onDiscussClick = {
                                    activeCommentingItem = item
                                    if (item.post != null) onOpenComments?.invoke(item.post)
                                },
                                onRemoveClick = { handleRemove(item) },
                                onAuthorClick = {
                                    val authorId = item.author.id
                                    onProfileClick?.invoke(authorId)
                                }
                            )
                        }

                        if (hasMore) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                SeeMoreButton(
                                    isLoading = isLoadingMore,
                                    onClick = { handleSeeMore() }
                                )
                            }
                        }
                    }
                } else {
                    // Feed View: Exact 1:1 WebPostCard stream matching FeedScreen
                    LazyColumn(
                        state = feedListState,
                        contentPadding = PaddingValues(bottom = 40.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(visibleItems, key = { "feed-${it.id}" }) { item ->
                            val cardPost = remember(item) { item.toFeedPost() }
                            val itemKey = "feed-${item.id}"
                            val isItemVisible by remember(feedListState, itemKey) {
                                derivedStateOf {
                                    feedListState.layoutInfo.visibleItemsInfo.any { it.key == itemKey }
                                }
                            }
                            val liveFollowing = users.find { it.id == cardPost.author.id }?.isFollowing
                                ?: cardPost.author.isFollowing

                            WebPostCard(
                                post = cardPost,
                                currentUser = resolvedCurrentUser,
                                isFollowing = liveFollowing,
                                isVisible = isItemVisible,
                                onFollowToggle = { onFollowUser?.invoke(cardPost.author.id) },
                                onLike = { onLikePost?.invoke(cardPost.id) },
                                onReact = { reaction ->
                                    onReact?.invoke(cardPost.id, reaction) ?: onLikePost?.invoke(cardPost.id)
                                },
                                onComment = {
                                    val commentHandler = onCommentPost ?: onOpenComments
                                    if (commentHandler != null) {
                                        commentHandler(cardPost)
                                    } else {
                                        activeCommentingItem = item
                                    }
                                },
                                onShare = {
                                    shareSheetPost = cardPost
                                },
                                onSave = { handleRemove(item) },
                                onUserClick = { onProfileClick?.invoke(cardPost.author.id) },
                                onAuthorClick = { user -> onProfileClick?.invoke(user.id) },
                                onOpenGallery = { imgs, idx ->
                                    activePostForGallery = cardPost
                                    galleryState = Pair(imgs, idx)
                                },
                                onOpenReactions = {
                                    reactionsSheetPost = cardPost
                                },
                                onOpenProduct = { onOpenProduct?.invoke(it) },
                                onOpenEvent = { onOpenEvent?.invoke(it) },
                                onOpenGroup = { onOpenGroup?.invoke(it) },
                                onPlayMusic = { onPlayMusic?.invoke(it) },
                                onOpenVideo = { postToOpen ->
                                    when {
                                        item.reel != null && onVideoClick != null -> onVideoClick(item.reel)
                                        onOpenVideo != null -> onOpenVideo(postToOpen)
                                        else -> activeVideoPreview = item
                                    }
                                },
                                onDeletePost = { onDeletePost?.invoke(it) },
                                onEditPost = { id, text -> onEditPost?.invoke(id, text) },
                                onHidePost = { onHidePost?.invoke(it) },
                                onReportPost = onReportPost,
                                allUsers = users,
                                onLikeComment = onLikeComment,
                                onRequestAuth = { onRequestAuth?.invoke() },
                                onRsvpEvent = onRsvpEvent,
                                modifier = Modifier.testTag("saved_feed_item_${item.id}")
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(AppColors.bgScreen)
                            )
                        }

                        if (hasMore) {
                            item {
                                SeeMoreButton(
                                    isLoading = isLoadingMore,
                                    onClick = { handleSeeMore() }
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // ==========================================
        // 1. LIGHTBOX IMAGE VIEWER
        // ==========================================
        if (lightboxImageUrl != null) {
            Dialog(
                onDismissRequest = { lightboxImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { lightboxImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = lightboxImageUrl,
                        contentDescription = "Full view image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { lightboxImageUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. VIDEO PREVIEW MODAL
        // ==========================================
        if (activeVideoPreview != null) {
            val videoItem = activeVideoPreview!!
            var isVideoPlaying by remember { mutableStateOf(true) }
            var isVideoMuted by remember { mutableStateOf(false) }

            Dialog(
                onDismissRequest = { activeVideoPreview = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Video container preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isVideoPlaying = !isVideoPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = videoItem.mediaUrl ?: videoItem.reel?.thumbnailUrl,
                            contentDescription = "Video preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Play / Pause central indicator
                        AnimatedVisibility(
                            visible = !isVideoPlaying,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = videoItem.author.profileImageUrl,
                                contentDescription = videoItem.author.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Column {
                                Text(
                                    text = videoItem.author.name,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@${videoItem.author.username}",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isVideoMuted = !isVideoMuted },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = if (isVideoMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Toggle sound",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { activeVideoPreview = null },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Bottom info overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        if (videoItem.content.isNotBlank()) {
                            Text(
                                text = videoItem.content,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = if (videoItem.isLiked) LikeRed else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${videoItem.likesCount}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${videoItem.commentsCount}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Button(
                                onClick = { handleRemove(videoItem) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFF59E0B)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saved", fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. COMMENTS MODAL DIALOG
        // ==========================================
        if (activeCommentingItem != null) {
            val item = activeCommentingItem!!
            val post = item.post
            val commentsList = post?.comments ?: emptyList()

            Dialog(
                onDismissRequest = { activeCommentingItem = null }
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Comments (${commentsList.size})",
                                fontSize = 17.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )

                            IconButton(
                                onClick = { activeCommentingItem = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = AppColors.border
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (commentsList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No comments yet. Start the conversation!",
                                            color = AppColors.textSecondary,
                                            fontSize = 13.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(commentsList) { comment ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = comment.author.profileImageUrl,
                                            contentDescription = comment.author.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AppColors.bgInput)
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = comment.author.name,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = AppColors.textPrimary
                                                )
                                                Text(
                                                    text = comment.timestamp,
                                                    fontSize = 11.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    color = AppColors.textSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = comment.text,
                                                fontSize = 13.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                color = AppColors.textBody
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
                                placeholder = { Text("Write a comment...", color = AppColors.textMuted, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.border,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        if (post != null) {
                                            onCommentPost?.invoke(post)
                                        }
                                        newCommentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.primaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send comment",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        // ==========================================
        // 4. GALLERY VIEWER, REACTIONS & SHARE SHEETS (1:1 FeedScreen Parity)
        // ==========================================
        galleryState?.let { (imgs, idx) ->
            activePostForGallery?.let { currentPost ->
                GalleryViewerDialog(
                    images = imgs,
                    initialIndex = idx,
                    post = currentPost,
                    onDismiss = {
                        galleryState = null
                        activePostForGallery = null
                    },
                    onLike = { onLikePost?.invoke(currentPost.id) },
                    onReact = { reaction ->
                        onReact?.invoke(currentPost.id, reaction) ?: onLikePost?.invoke(currentPost.id)
                    },
                    onComment = {
                        galleryState = null
                        (onCommentPost ?: onOpenComments)?.invoke(currentPost)
                    },
                    onShare = {
                        galleryState = null
                        shareSheetPost = currentPost
                    },
                    onSave = { onSavePost?.invoke(currentPost.id) }
                )
            }
        }

        reactionsSheetPost?.let { currentPost ->
            ReactionsSheetDialog(
                post = currentPost,
                allUsers = users,
                onDismiss = { reactionsSheetPost = null },
                onUserClick = { user ->
                    reactionsSheetPost = null
                    onProfileClick?.invoke(user.id)
                }
            )
        }

        shareSheetPost?.let { currentPost ->
            ShareBottomSheetDialog(
                post = currentPost,
                onDismiss = { shareSheetPost = null },
                onShareToFeed = {
                    shareSheetPost = null
                    onShare?.invoke(currentPost)
                },
                onShareToGroup = {
                    shareSheetPost = null
                    onShare?.invoke(currentPost)
                },
                onSendMessage = {
                    shareSheetPost = null
                    onShare?.invoke(currentPost)
                },
                onCopyLink = {
                    shareSheetPost = null
                    onShare?.invoke(currentPost)
                }
            )
        }
    }
}

/**
 * Grid Item Card for Saved Posts
 * Matches web aspect-[4/5] rounded-xl overflow-hidden hover:border-[#38BDF8]/40
 */
@Composable
private fun SavedGridItemCard(
    item: SavedPostItem,
    onCardClick: () -> Unit,
    onDiscussClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = item.mediaUrl ?: item.reel?.thumbnailUrl
    val hasMedia = !media.isNullOrBlank()
    val accentGold = savedAccentGold()
    val tabActive = savedTabActive()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasMedia) Color(0xFF050B18) else AppColors.bgCard
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable(onClick = onCardClick)
            .testTag("saved_grid_item_${item.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasMedia) {
                AsyncImage(
                    model = media,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.type == "video") {
                    // Dark scrim for video
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Top-left "Video" badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = tabActive,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Video",
                            color = tabActive,
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // Text-only post in grid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"${item.content.ifBlank { "Post" }}\"",
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        color = AppColors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top-right action buttons (Discuss & Remove)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDiscussClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .testTag("saved_grid_discuss_${item.id}")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_far_comment),
                        contentDescription = "Discuss",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .testTag("saved_grid_remove_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove from saved",
                        tint = accentGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom user bar gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clickable(onClick = onAuthorClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = item.author.profileImageUrl,
                        contentDescription = item.author.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(0.8.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )

                    Text(
                        text = item.author.name,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun SavedPostItem.toFeedPost(): Post {
    if (post != null) return post
    val r = reel
    return Post(
        id = id,
        author = r?.author ?: author,
        content = r?.caption ?: content,
        mediaUrl = r?.videoUrl?.takeIf { it.isNotBlank() } ?: r?.thumbnailUrl,
        mediaUrls = listOfNotNull(
            r?.videoUrl?.takeIf { it.isNotBlank() },
            r?.thumbnailUrl?.takeIf { it.isNotBlank() }
        ),
        mediaTypes = buildList {
            if (!r?.videoUrl.isNullOrBlank()) add("video")
            if (!r?.thumbnailUrl.isNullOrBlank()) add("image")
        },
        timestamp = "Saved",
        likesCount = r?.likesCount ?: likesCount,
        commentsCount = r?.commentsCount ?: commentsCount,
        sharesCount = r?.sharesCount ?: sharesCount,
        isLiked = r?.isLiked ?: isLiked,
        isSaved = true,
        postType = "video",
        videoUrl = r?.videoUrl,
        mediaType = "video"
    )
}

/**
 * See More Pagination Button
 * Matches web's animated spinner button
 */
@Composable
private fun SeeMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.bgCardElevated,
                contentColor = AppColors.textPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.borderStrong),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            modifier = Modifier.testTag("saved_posts_see_more_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.primaryBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loading...",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = AppColors.textPrimary
                )
            } else {
                Text(
                    text = "See More",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.textPrimary
                )
            }
        }
    }
}
