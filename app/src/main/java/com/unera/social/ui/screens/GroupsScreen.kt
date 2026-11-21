package com.unera.social.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import com.unera.social.data.GroupMemberInfo
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unera.social.ui.components.VerifiedBadge
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import com.unera.social.ui.components.FeatheredAvatar
import com.unera.social.ui.components.PostUploadBottomPill
import com.unera.social.ui.components.PostUploadState
import coil.compose.AsyncImage
import com.unera.social.model.*
import android.content.Intent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.Send
import com.unera.social.ui.components.DiscussionsSheetDialog
import com.unera.social.ui.components.GalleryViewerDialog
import com.unera.social.ui.components.ShareBottomSheetDialog
import com.unera.social.ui.components.ReactionsSheetDialog
import com.unera.social.data.PostsApiService
import com.unera.social.ui.components.MediaGrid
import androidx.compose.ui.platform.testTag
import com.unera.social.ui.components.DiscussButton
import com.unera.social.ui.components.FacebookReactionButton
import com.unera.social.ui.components.FacebookReactionSummaryRow
import com.unera.social.ui.components.REACTION_EMOJIS
import com.unera.social.ui.components.TextPostBackgroundView
import com.unera.social.ui.components.getReactionEmoji
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.unera.social.data.CommentsCache
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily

fun isCareerCategory(category: String?, name: String? = null): Boolean {
    val c = category?.lowercase() ?: ""
    val n = name?.lowercase() ?: ""
    return c == "recruitment" || c == "career" || c == "careers" || c == "job" || c == "jobs" ||
            n.contains("career") || n.contains("job") || n.contains("recruitment") || n.contains("hiring") || n.contains("employment")
}

fun GroupPost.toPost(group: Group? = null): Post {
    return Post(
        id = id,
        author = author,
        content = content,
        mediaUrl = imageUrls.firstOrNull(),
        mediaUrls = imageUrls,
        timestamp = createdAt,
        likesCount = likesCount,
        commentsCount = commentsCount,
        sharesCount = sharesCount,
        isLiked = isLiked,
        myReaction = myReaction,
        groupId = groupId,
        groupName = group?.name,
        groupImageUrl = group?.profileImageUrl,
        jobTitle = jobTitle,
        company = company,
        jobType = jobType,
        salary = salary,
        applicationValue = applicationValue,
        comments = comments,
        postType = "group_post",
        createdAt = createdAt
    )
}

enum class GroupsTab(val label: String) {
    YOUR_GROUPS("Your groups"),
    DISCOVER("Discover"),
    INVITES("Invites")
}

enum class GroupDetailTab(val label: String) {
    DISCUSSION("Discussion"),
    EVENTS("Events"),
    MEMBERS("Members"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentUser: User,
    groups: List<Group>,
    groupPosts: List<GroupPost>,
    groupInvites: List<GroupInvite>,
    allEvents: List<Event>,
    allUsers: List<User>,
    initialGroupId: Long? = null,
    groupSuggestions: List<Group> = emptyList(),
    groupMembers: List<GroupMemberInfo> = emptyList(),
    groupEvents: List<Event> = emptyList(),
    isGroupsLoading: Boolean = false,
    isGroupPostsLoading: Boolean = false,
    onBack: () -> Unit,
    onSelectGroup: (Long?) -> Unit = {},
    onCreateGroup: (name: String, desc: String, type: String, category: String, profile: String, cover: String) -> Unit,
    onJoinGroup: (Long) -> Unit,
    onLeaveGroup: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onUpdateGroupSettings: (Long, String, String, Boolean) -> Unit,
    onPostToGroup: (Long, String, List<String>, Double?, String, String?, String?, String, String?, String?, String?, String?, String?, String?) -> Unit,
    onToggleLikePost: (Long, String) -> Unit,
    onAddPostComment: (Long, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onSharePost: (Long) -> Unit,
    onInviteUser: (Long, Long) -> Unit,
    onAcceptInvite: (Long, Long) -> Unit,
    onDeclineInvite: (Long) -> Unit,
    onToggleMemberPosting: (Long, Long, Boolean) -> Unit,
    onSetMemberRole: (Long, Long, String) -> Unit,
    onRemoveMember: (Long, Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    uploadState: PostUploadState? = null,
    onDismissUpload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeGroupId by remember { mutableStateOf(initialGroupId) }
    var selectedTopTab by remember { mutableStateOf(GroupsTab.YOUR_GROUPS) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Most visited") }
    var showSortDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeGroupId) {
        onSelectGroup(activeGroupId)
    }

    // Modals
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedCategoryForCreate by remember { mutableStateOf("general") }

    val context = LocalContext.current
    val activeGroup = groups.find { it.id == activeGroupId }

    BackHandler {
        if (activeGroupId != null) {
            activeGroupId = null
        } else {
            onBack()
        }
    }

    if (activeGroup != null) {
        val effectiveEvents = if (groupEvents.isNotEmpty()) groupEvents else allEvents.filter { it.groupId == activeGroup.id }
        GroupDetailScreen(
            group = activeGroup,
            currentUser = currentUser,
            allPosts = groupPosts.filter { it.groupId == activeGroup.id },
            allEvents = effectiveEvents,
            allUsers = allUsers,
            groupMembersInfo = groupMembers,
            isPostsLoading = isGroupPostsLoading,
            onBack = { activeGroupId = null },
            onJoinGroup = { onJoinGroup(activeGroup.id) },
            onLeaveGroup = { onLeaveGroup(activeGroup.id) },
            onDeleteGroup = {
                onDeleteGroup(activeGroup.id)
                activeGroupId = null
            },
            onUpdateSettings = { name, desc, postingAllowed ->
                onUpdateGroupSettings(activeGroup.id, name, desc, postingAllowed)
            },
            onPost = { content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal ->
                onPostToGroup(activeGroup.id, content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal)
            },
            onToggleLikePost = onToggleLikePost,
            onAddPostComment = onAddPostComment,
            onDeletePost = onDeletePost,
            onSharePost = onSharePost,
            onInviteUser = { userId -> onInviteUser(activeGroup.id, userId) },
            onToggleMemberPosting = { userId, disabled -> onToggleMemberPosting(activeGroup.id, userId, disabled) },
            onSetMemberRole = { userId, role -> onSetMemberRole(activeGroup.id, userId, role) },
            onRemoveMember = { userId -> onRemoveMember(activeGroup.id, userId) },
            onProfileClick = onProfileClick,
            uploadState = uploadState,
            onDismissUpload = onDismissUpload,
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(AppColors.headerBg)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.headerIconTint)
                            }
                            Text(
                                text = "Groups",
                                fontFamily = InterFontFamily,
                                color = AppColors.headerText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showCategoryDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AppColors.headerIconBg)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create Group", tint = AppColors.headerIconTint)
                            }
                        }
                    }

                    // 3 Tabs: Your groups, Discover, Invites
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GroupsTab.values().forEach { tab ->
                            val selected = selectedTopTab == tab
                            val pillBg = if (AppColors.isLight) {
                                if (selected) Color.White else Color.White.copy(alpha = 0.18f)
                            } else {
                                if (selected) AppColors.primaryBlue else AppColors.bgCardElevated
                            }
                            val pillText = if (AppColors.isLight) {
                                if (selected) AppColors.primaryBlue else Color.White
                            } else {
                                if (selected) Color.White else AppColors.textSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(pillBg)
                                    .clickable { selectedTopTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tab.label,
                                        fontFamily = InterFontFamily,
                                        color = pillText,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    if (tab == GroupsTab.INVITES && groupInvites.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color(0xFFF3425F))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${groupInvites.size}",
                                                fontFamily = InterFontFamily,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search Input
                    val searchContainer = if (AppColors.isLight) Color.White else AppColors.bgCardElevated
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        ),
                        placeholder = {
                            Text(
                                "Search Groups",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textMuted,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = searchContainer,
                            unfocusedContainerColor = searchContainer,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedBorderColor = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                }
            },
            containerColor = AppColors.bgScreen,
            modifier = modifier
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTopTab) {
                    GroupsTab.YOUR_GROUPS -> {
                        val userGroups = groups.filter { group ->
                            val isOwner = group.adminId == currentUser.id
                            val isMem = group.isMember || group.members.contains(currentUser.id)
                            isOwner || isMem
                        }.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                        }

                        val createdByMe = userGroups.filter { it.adminId == currentUser.id }
                        val joinedByMe = userGroups.filter { it.adminId != currentUser.id }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your groups (${userGroups.size})",
                                        fontFamily = InterFontFamily,
                                        color = AppColors.textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Sort",
                                        fontFamily = InterFontFamily,
                                        color = AppColors.primaryBlue,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable { showSortDialog = true }
                                    )
                                }
                            }

                            if (createdByMe.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Created by you",
                                        fontFamily = InterFontFamily,
                                        color = AppColors.textSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                items(createdByMe) { group ->
                                    GroupListItem(
                                        group = group,
                                        onClick = { activeGroupId = group.id }
                                    )
                                }
                            }

                            if (joinedByMe.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Groups you joined",
                                        fontFamily = InterFontFamily,
                                        color = AppColors.textSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                items(joinedByMe) { group ->
                                    GroupListItem(
                                        group = group,
                                        onClick = { activeGroupId = group.id }
                                    )
                                }
                            }

                            if (userGroups.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isGroupsLoading) {
                                            CircularProgressIndicator(color = AppColors.primaryBlue)
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Groups, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(56.dp))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text("No groups yet", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                                Text("Create your first group or discover active ones!", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = { showCategoryDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Create Group", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GroupsTab.DISCOVER -> {
                        val joinedGroupIds = groups.filter { group ->
                            val isOwner = currentUser.id > 0L && group.adminId == currentUser.id
                            val isMem = group.isMember || (currentUser.id > 0L && group.members.contains(currentUser.id))
                            isOwner || isMem
                        }.map { it.id }.toSet()
                        val unjoinedFromGroups = groups.filter { it.id !in joinedGroupIds }
                        val combinedSuggestions = (groupSuggestions + unjoinedFromGroups)
                            .distinctBy { it.id }
                            .filter { g ->
                                g.id !in joinedGroupIds &&
                                    !g.isMember &&
                                    (currentUser.id <= 0L || (g.adminId != currentUser.id && !g.members.contains(currentUser.id)))
                            }
                            .filter {
                                searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
                            }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = "Discover Groups (${combinedSuggestions.size})",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            if (isGroupsLoading && combinedSuggestions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = AppColors.primaryBlue)
                                    }
                                }
                            } else {
                                items(combinedSuggestions) { group ->
                                    GroupListItem(
                                        group = group,
                                        onClick = { activeGroupId = group.id },
                                        showJoinButton = true,
                                        onJoinClick = { onJoinGroup(group.id) }
                                    )
                                }

                                if (combinedSuggestions.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No more groups to discover right now", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GroupsTab.INVITES -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = "Group Invites (${groupInvites.size})",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            items(groupInvites) { invite ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, AppColors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = invite.group.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${invite.group.name}" },
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(invite.group.name, fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("Invited by ${invite.inviter.name}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = Color(0xFF45BD62), fontSize = 12.sp)
                                                Text("${invite.group.membersCount} members", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onAcceptInvite(invite.id, invite.groupId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Accept & Join", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color.White)
                                            }
                                            OutlinedButton(
                                                onClick = { onDeclineInvite(invite.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.textBody),
                                                border = BorderStroke(1.dp, AppColors.borderStrong),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Decline", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }

                            if (groupInvites.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.MailOutline, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(52.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("No pending invites", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                            Text("When someone invites you to a group, it will show up here.", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Category Selection Modal
    if (showCategoryDialog) {
        CategorySelectionDialog(
            onDismiss = { showCategoryDialog = false },
            onSelectCategory = { cat ->
                selectedCategoryForCreate = cat
                showCategoryDialog = false
                showCreateGroupDialog = true
            }
        )
    }

    // Create Group Modal
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            category = selectedCategoryForCreate,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, privacy ->
                onCreateGroup(
                    name,
                    desc,
                    privacy,
                    selectedCategoryForCreate,
                    "https://ui-avatars.com/api/?name=${name}&background=random",
                    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1500&q=80"
                )
                showCreateGroupDialog = false
            }
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort groups", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    listOf("Most visited", "Recently active", "Alphabetical").forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sortMode = mode
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textPrimary, fontSize = 16.sp)
                            if (sortMode == mode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.primaryBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = AppColors.bgCard
        )
    }

    if (uploadState != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PostUploadBottomPill(
                uploadState = uploadState,
                onDismiss = onDismissUpload
            )
        }
    }
}

@Composable
fun GroupListItem(
    group: Group,
    onClick: () -> Unit,
    showJoinButton: Boolean = false,
    onJoinClick: (() -> Unit)? = null
) {
    val catColor = when (group.category) {
        "recruitment" -> Color(0xFF45BD62)
        "buy_sell" -> Color(0xFFF7B928)
        else -> AppColors.primaryBlue
    }

    val catIcon = when (group.category) {
        "recruitment" -> Icons.Default.Work
        "buy_sell" -> Icons.Default.Storefront
        else -> Icons.Default.Groups
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.bgCard)
            .border(1.dp, AppColors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = group.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${group.name}" },
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    fontFamily = InterFontFamily,
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${group.membersCount} members • ${if (group.isPrivate) "Private" else "Public"}",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                color = AppColors.textSecondary,
                fontSize = 12.sp
            )
        }

        if (showJoinButton && onJoinClick != null) {
            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Join", fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.textMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    currentUser: User,
    allPosts: List<GroupPost>,
    allEvents: List<Event>,
    allUsers: List<User>,
    groupMembersInfo: List<GroupMemberInfo> = emptyList(),
    isPostsLoading: Boolean = false,
    onBack: () -> Unit,
    onJoinGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onUpdateSettings: (name: String, desc: String, postingAllowed: Boolean) -> Unit,
    onPost: (String, List<String>, Double?, String, String?, String?, String, String?, String?, String?, String?, String?, String?) -> Unit,
    onToggleLikePost: (Long, String) -> Unit,
    onAddPostComment: (Long, String) -> Unit,
    onDeletePost: (Long) -> Unit,
    onSharePost: (Long) -> Unit,
    onInviteUser: (Long) -> Unit,
    onToggleMemberPosting: (Long, Boolean) -> Unit,
    onSetMemberRole: (Long, String) -> Unit,
    onRemoveMember: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    uploadState: PostUploadState? = null,
    onDismissUpload: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(GroupDetailTab.DISCUSSION) }
    var showCreatePostModal by remember { mutableStateOf(false) }
    var autoLaunchPhotoPicker by remember { mutableStateOf(false) }
    var showInviteModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    var activeDiscussionPost by remember { mutableStateOf<Post?>(null) }
    var activeReactionsPost by remember { mutableStateOf<Post?>(null) }
    var activeSharePost by remember { mutableStateOf<Post?>(null) }
    var activeGalleryState by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var activePostForGallery by remember { mutableStateOf<Post?>(null) }
    val context = LocalContext.current

    val isOwner = group.adminId == currentUser.id
    val isMember = isOwner || group.isMember || group.members.contains(currentUser.id)
    val canPost = isOwner || (isMember && group.memberPostingAllowed)

    val catColor = when (group.category) {
        "recruitment" -> Color(0xFF45BD62)
        "buy_sell" -> Color(0xFFF7B928)
        else -> Color(0xFF1877F2)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        // Cover and Back Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = group.coverImageUrl.ifBlank { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1500&q=80" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row {
                        IconButton(
                            onClick = { showShareModal = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        if (isOwner) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showSettingsModal = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Group Identity Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.bgCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = group.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${group.name}" },
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, AppColors.primaryBlue, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            fontFamily = InterFontFamily,
                            color = AppColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${when (group.category) { "recruitment" -> "Careers & Hiring"; "buy_sell" -> "Buy & Sell"; else -> "General" }} • ${if (group.isPrivate) "Private" else "Public"} • ${group.membersCount} members",
                            fontFamily = InterFontFamily,
                            color = catColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Join/Joined, Invite, Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isMember) {
                        Button(
                            onClick = onLeaveGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.bgChip),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Joined", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { showInviteModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Invite", fontFamily = InterFontFamily, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Button(
                            onClick = onJoinGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Group", fontFamily = InterFontFamily, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Row: Discussion, Events, Members, About
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    GroupDetailTab.values().forEach { tab ->
                        val selected = selectedTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = tab.label,
                                fontFamily = InterFontFamily,
                                color = if (selected) AppColors.primaryBlue else AppColors.textSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(AppColors.primaryBlue)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            GroupDetailTab.DISCUSSION -> {
                if (canPost) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, AppColors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(AppColors.bgInput)
                                        .clickable {
                                            autoLaunchPhotoPicker = false
                                            showCreatePostModal = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = currentUser.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${currentUser.name}" },
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = when {
                                            group.category == "buy_sell" -> "Sell something in ${group.name}..."
                                            isCareerCategory(group.category, group.name) -> "Post a job opportunity in ${group.name}..."
                                            else -> "Write something to ${group.name}..."
                                        },
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.textSecondary,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(18.dp))
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = AppColors.border)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Photo / Video action
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                autoLaunchPhotoPicker = true
                                                showCreatePostModal = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Photo/Video", tint = Color(0xFF45BD62), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Photo/Video", fontFamily = InterFontFamily, color = AppColors.textBody, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // Event action
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedTab = GroupDetailTab.EVENTS
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = "Event", tint = AppColors.primaryBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Event", fontFamily = InterFontFamily, color = AppColors.textBody, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }

                                    if (group.category == "buy_sell") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    autoLaunchPhotoPicker = false
                                                    showCreatePostModal = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Storefront, contentDescription = "Sell Item", tint = Color(0xFFF7B928), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sell Item", fontFamily = InterFontFamily, color = AppColors.textBody, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else if (isCareerCategory(group.category, group.name)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    autoLaunchPhotoPicker = false
                                                    showCreatePostModal = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Work, contentDescription = "Post Job", tint = Color(0xFF45BD62), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Post Job", fontFamily = InterFontFamily, color = AppColors.textBody, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(allPosts, key = { it.id }) { post ->
                    GroupPostCard(
                        post = post,
                        group = group,
                        currentUser = currentUser,
                        onLike = { onToggleLikePost(post.id, "like") },
                        onReact = { reaction -> onToggleLikePost(post.id, reaction) },
                        onComment = {
                            activeDiscussionPost = post.toPost(group)
                        },
                        onShare = {
                            activeSharePost = post.toPost(group)
                        },
                        onSave = {
                            Toast.makeText(context, "Post saved to your bookmarks", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = if (isOwner || post.author.id == currentUser.id) { { onDeletePost(post.id) } } else null,
                        onProfileClick = onProfileClick,
                        onOpenGallery = { imgs, idx ->
                            activePostForGallery = post.toPost(group)
                            activeGalleryState = Pair(imgs, idx)
                        },
                        onOpenReactions = {
                            activeReactionsPost = post.toPost(group)
                        }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(AppColors.sectionDivider)
                    )
                }

                if (allPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPostsLoading) {
                                CircularProgressIndicator(color = AppColors.primaryBlue)
                            } else {
                                Text("No discussions yet. Be the first to start a conversation!", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            GroupDetailTab.EVENTS -> {
                items(allEvents) { event ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                        border = BorderStroke(1.dp, AppColors.border),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(event.title, fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(event.date, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.primaryBlue, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(event.description, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }

                if (allEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No group events scheduled.", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            GroupDetailTab.MEMBERS -> {
                if (groupMembersInfo.isNotEmpty()) {
                    items(groupMembersInfo) { member ->
                        val userIsAdmin = member.role.equals("admin", ignoreCase = true) || member.userId == group.adminId
                        val userIsMod = member.role.equals("moderator", ignoreCase = true)
                        var showActionMenu by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.bgCard)
                                .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                .clickable { onProfileClick(member.userId) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = member.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${member.name}" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(member.name.ifBlank { "Member" }, fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (member.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge(size = 14.dp)
                                    }
                                    if (userIsAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Admin", fontFamily = InterFontFamily, color = AppColors.primaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    } else if (userIsMod) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Moderator", fontFamily = InterFontFamily, color = Color(0xFF45BD62), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text("@${member.username}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                                if (member.postingDisabled) {
                                    Text("Posting muted", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color(0xFFF3425F), fontSize = 11.sp)
                                }
                            }

                            if (isOwner && member.userId != currentUser.id) {
                                Box {
                                    IconButton(onClick = { showActionMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Manage", tint = AppColors.textSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = showActionMenu,
                                        onDismissRequest = { showActionMenu = false },
                                        modifier = Modifier.background(AppColors.bgCard)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (member.postingDisabled) "Enable Posting" else "Mute Posting",
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AppColors.textPrimary
                                                )
                                            },
                                            onClick = {
                                                onToggleMemberPosting(member.userId, !member.postingDisabled)
                                                showActionMenu = false
                                            }
                                        )
                                        if (userIsAdmin) {
                                            DropdownMenuItem(
                                                text = { Text("Demote from Admin", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary) },
                                                onClick = {
                                                    onSetMemberRole(member.userId, "remove-admin")
                                                    showActionMenu = false
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Promote to Admin", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary) },
                                                onClick = {
                                                    onSetMemberRole(member.userId, "make-admin")
                                                    showActionMenu = false
                                                }
                                            )
                                        }
                                        if (!userIsMod) {
                                            DropdownMenuItem(
                                                text = { Text("Promote to Moderator", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary) },
                                                onClick = {
                                                    onSetMemberRole(member.userId, "make-moderator")
                                                    showActionMenu = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Remove from Group", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color(0xFFF3425F)) },
                                            onClick = {
                                                onRemoveMember(member.userId)
                                                showActionMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val memberIds = (group.members + group.adminId).distinct()
                    val groupUsers = allUsers.filter { memberIds.contains(it.id) }

                    items(groupUsers) { user ->
                        val userIsOwner = user.id == group.adminId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.bgCard)
                                .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                .clickable { onProfileClick(user.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (userIsOwner) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Admin (Owner)", fontFamily = InterFontFamily, color = AppColors.primaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text("@${user.username}", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                            }

                            if (isOwner && !userIsOwner) {
                                IconButton(onClick = { onRemoveMember(user.id) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color(0xFFF3425F))
                                }
                            }
                        }
                    }
                }
            }

            GroupDetailTab.ABOUT -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
                        border = BorderStroke(1.dp, AppColors.border),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("About this community", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(group.description.ifBlank { "No description provided." }, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textBody, fontSize = 14.sp, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = AppColors.border)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (group.isPrivate) Icons.Default.Lock else Icons.Default.Public, contentDescription = null, tint = AppColors.textSecondary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(if (group.isPrivate) "Private Group" else "Public Group", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(if (group.isPrivate) "Only members can see posts and members" else "Anyone can see who's in the group and what they post", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = AppColors.textSecondary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Created in 2026", fontFamily = InterFontFamily, color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Active East Africa Community", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uploadState != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PostUploadBottomPill(
                uploadState = uploadState,
                onDismiss = onDismissUpload
            )
        }
    }
}

    // Create Post Modal in Group
    if (showCreatePostModal) {
        CreateGroupPostDialog(
            group = group,
            currentUser = currentUser,
            autoPickPhotos = autoLaunchPhotoPicker,
            onDismiss = {
                showCreatePostModal = false
                autoLaunchPhotoPicker = false
            },
            onSubmit = { content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal ->
                onPost(content, images, price, curr, cond, loc, stat, job, comp, jType, sal, appType, appVal)
                showCreatePostModal = false
                autoLaunchPhotoPicker = false
            }
        )
    }

    // Invite Modal
    if (showInviteModal) {
        InviteFollowersDialog(
            group = group,
            followers = allUsers.filter { it.id != currentUser.id && !group.members.contains(it.id) },
            onDismiss = { showInviteModal = false },
            onInvite = { userId ->
                onInviteUser(userId)
                Toast.makeText(context, "Invitation sent!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Share Group Dialog
    if (showShareModal) {
        ShareGroupDialog(
            group = group,
            onDismiss = { showShareModal = false },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Group Link", "https://unera.social/groups/${group.id}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Group link copied to clipboard!", Toast.LENGTH_SHORT).show()
                showShareModal = false
            }
        )
    }

    // Settings Modal
    if (showSettingsModal) {
        GroupSettingsDialog(
            group = group,
            onDismiss = { showSettingsModal = false },
            onSave = { name, desc, postingAllowed ->
                onUpdateSettings(name, desc, postingAllowed)
                showSettingsModal = false
            },
            onDelete = {
                showSettingsModal = false
                onDeleteGroup()
            }
        )
    }

    // Discussions Modal (Full Comments Sheet matching web)
    activeDiscussionPost?.let { curPost ->
        DiscussionsSheetDialog(
            post = curPost,
            currentUser = currentUser,
            onDismiss = { activeDiscussionPost = null },
            onAddComment = { commentText ->
                onAddPostComment(curPost.id, commentText)
            },
            onLikeComment = { _ -> },
            onProfileClick = { user ->
                activeDiscussionPost = null
                onProfileClick(user.id)
            },
            onLikePost = {
                onToggleLikePost(curPost.id, "like")
            },
            onReactPost = { rType ->
                onToggleLikePost(curPost.id, rType)
            },
            onSharePost = {
                activeDiscussionPost = null
                activeSharePost = curPost
            },
            onSavePost = { },
            onOpenGallery = { imgs, idx ->
                activePostForGallery = curPost
                activeGalleryState = Pair(imgs, idx)
            }
        )
    }

    // Reactions Sheet Modal
    activeReactionsPost?.let { curPost ->
        ReactionsSheetDialog(
            post = curPost,
            allUsers = allUsers,
            onDismiss = { activeReactionsPost = null },
            onUserClick = { user ->
                activeReactionsPost = null
                onProfileClick(user.id)
            }
        )
    }

    // Share Modal
    activeSharePost?.let { curPost ->
        ShareBottomSheetDialog(
            post = curPost,
            onDismiss = { activeSharePost = null },
            onShareToFeed = {
                onSharePost(curPost.id)
                Toast.makeText(context, "Shared to feed!", Toast.LENGTH_SHORT).show()
                activeSharePost = null
            },
            onShareToGroup = {
                onSharePost(curPost.id)
                Toast.makeText(context, "Shared to group!", Toast.LENGTH_SHORT).show()
                activeSharePost = null
            },
            onSendMessage = {
                Toast.makeText(context, "Share link ready to send", Toast.LENGTH_SHORT).show()
                activeSharePost = null
            },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Post Link", "https://unera.social/posts/${curPost.id}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Post link copied to clipboard!", Toast.LENGTH_SHORT).show()
                activeSharePost = null
            }
        )
    }

    // Fullscreen Gallery Modal
    activeGalleryState?.let { (imgs, idx) ->
        activePostForGallery?.let { curPost ->
            GalleryViewerDialog(
                images = imgs,
                initialIndex = idx,
                post = curPost,
                onDismiss = {
                    activeGalleryState = null
                    activePostForGallery = null
                },
                onLike = { onToggleLikePost(curPost.id, "like") },
                onComment = {
                    activeGalleryState = null
                    activeDiscussionPost = curPost
                },
                onShare = {
                    activeGalleryState = null
                    activeSharePost = curPost
                },
                onSave = { }
            )
        }
    }
}

@Composable
fun GroupMediaGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    MediaGrid(
        images = images,
        onImageClick = onImageClick,
        maxHeight = 560.dp
    )
}

@Composable
fun GroupPostCard(
    post: GroupPost,
    group: Group,
    currentUser: User,
    onLike: () -> Unit,
    onReact: (String) -> Unit = {},
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onProfileClick: (Long) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit = { _, _ -> },
    onOpenReactions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showReactionDock by remember { mutableStateOf(false) }
    var isExpandedText by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val cacheVersion by CommentsCache.cacheVersion.collectAsState()
    var previewComment by remember(post.id, post.comments, cacheVersion) {
        mutableStateOf(
            post.comments.firstOrNull() ?: CommentsCache.getCachedComments("group_post", post.id)?.firstOrNull()
        )
    }

    LaunchedEffect(post.id, post.commentsCount) {
        if (previewComment == null && post.commentsCount > 0) {
            val fetched = CommentsCache.ensurePreviewComment(post.toPost(group), currentUser.id)
            if (fetched != null) {
                previewComment = fetched
            }
        }
    }

    val isCareer = group.category == "recruitment" ||
        isCareerCategory(group.category, group.name) ||
        !post.jobTitle.isNullOrBlank()
    val isBuySell = group.category == "buy_sell" || post.price != null

    val hasApplicationMethod = !post.applicationValue.isNullOrBlank() &&
        (post.applicationType == "email" || post.applicationType == "link" ||
         post.applicationValue.contains("@") || post.applicationValue.startsWith("http"))

    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.bgCard),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("group_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Author Avatar, Name, Verified Badge, Category, More Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.author.profileImageUrl.ifBlank { "https://ui-avatars.com/api/?name=${post.author.name}" },
                    contentDescription = post.author.name,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, AppColors.border, CircleShape)
                        .clickable { onProfileClick(post.author.id) },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onProfileClick(post.author.id) }
                    ) {
                        Text(
                            text = post.author.name,
                            fontFamily = InterFontFamily,
                            color = AppColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.author.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(size = 15.dp)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(PostsApiService.formatRelativeTime(post.createdAt), fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                        Text("•", fontFamily = InterFontFamily, color = AppColors.textSecondary, fontSize = 12.sp)
                        if (isCareer) {
                            Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(12.dp))
                            Text("Recruitment", fontFamily = InterFontFamily, color = Color(0xFF45BD62), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else if (isBuySell) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFF7B928), modifier = Modifier.size(12.dp))
                            Text("Marketplace", fontFamily = InterFontFamily, color = Color(0xFFF7B928), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = AppColors.primaryBlue, modifier = Modifier.size(12.dp))
                            Text("Group Post", fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }

                // 3-dots more menu
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppColors.textSecondary)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(AppColors.bgCard)
                    ) {
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = Color(0xFFF3425F)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF3425F)) },
                                onClick = {
                                    showMoreMenu = false
                                    onDelete()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Report Post", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = AppColors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = AppColors.textSecondary) },
                            onClick = {
                                showMoreMenu = false
                                Toast.makeText(context, "Post reported to group admins", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Category Feature: Careers / Recruitment (Job features & Apply button)
            if (isCareer) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    // JOB POSTING badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF45BD62).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF45BD62).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JOB POSTING", fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF45BD62))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Job Details Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.bgCardElevated),
                        border = BorderStroke(1.dp, AppColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = post.jobTitle ?: "Job Opening",
                                fontFamily = InterFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            if (!post.company.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(post.company, fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textBody)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!post.location.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(post.location, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppColors.textSecondary)
                                    }
                                }
                                if (!post.jobType.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFF7B928), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(post.jobType, fontFamily = InterFontFamily, fontSize = 12.sp, color = Color(0xFFF7B928), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (!post.salary.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF45BD62), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(post.salary, fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF45BD62))
                                }
                            }

                            // APPLY BUTTON: Only rendered if user attached an application method!
                            if (hasApplicationMethod) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val appVal = post.applicationValue?.trim() ?: ""
                                            val isEmail = post.applicationType == "email" || appVal.contains("@")
                                            if (isEmail) {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$appVal")).apply {
                                                    putExtra(Intent.EXTRA_SUBJECT, "Application for ${post.jobTitle ?: "Position"} at ${post.company ?: group.name}")
                                                }
                                                context.startActivity(intent)
                                            } else {
                                                val url = if (!appVal.startsWith("http://") && !appVal.startsWith("https://")) "https://$appVal" else appVal
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                            }
                                            Toast.makeText(context, "Opening application...", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open application: ${post.applicationValue}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue, contentColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Apply Now", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Category Feature: Buy & Sell (Price tag, condition, location)
            if (isBuySell) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${post.currency} ${post.price ?: ""}",
                            fontFamily = InterFontFamily,
                            color = Color(0xFF45BD62),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        )
                        if (!post.condition.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF7B928).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(post.condition, fontFamily = InterFontFamily, color = Color(0xFFF7B928), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (!post.location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF7B928), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(post.location, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Text Content (with See More / See Less or Animated/Styled Background)
            val cleanContent = remember(post.content) {
                post.content.trim().let { if (it.equals("null", ignoreCase = true) || it.equals("undefined", ignoreCase = true)) "" else it }
            }
            val hasBackground = !post.backgroundStyle.isNullOrBlank() &&
                    !post.backgroundStyle.equals("none", ignoreCase = true) &&
                    !post.backgroundStyle.equals("null", ignoreCase = true)

            if (hasBackground && cleanContent.isNotBlank() && post.imageUrls.isEmpty() && post.videoUrl.isNullOrBlank()) {
                TextPostBackgroundView(
                    backgroundStyle = post.backgroundStyle,
                    content = cleanContent,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (cleanContent.isNotBlank()) {
                val words = remember(cleanContent) { cleanContent.split("\\s+".toRegex()).filter { it.isNotEmpty() } }
                val isLong = words.size > 25 || cleanContent.length > 180
                val displayText = if (isExpandedText || !isLong) cleanContent else words.take(25).joinToString(" ") + "…"

                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(
                        text = displayText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.textPrimary,
                        fontSize = 16.sp,
                        lineHeight = 23.sp
                    )
                    if (isLong) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isExpandedText) "See Less" else "See More",
                            fontFamily = InterFontFamily,
                            color = AppColors.primaryBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable { isExpandedText = !isExpandedText }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Attached Media Grid
            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                GroupMediaGrid(
                    images = post.imageUrls,
                    onImageClick = { idx -> onOpenGallery(post.imageUrls, idx) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reactions / Discussions / Shares Summary Header
            if (post.likesCount > 0 || post.commentsCount > 0 || post.sharesCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3D Non-moving Overlapping Reactions & Count
                    FacebookReactionSummaryRow(
                        likesCount = post.likesCount,
                        onOpenReactions = onOpenReactions
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (post.commentsCount > 0) {
                            Text(
                                text = "${post.commentsCount} Discussions",
                                fontFamily = InterFontFamily,
                                color = AppColors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable(onClick = onComment)
                            )
                        }
                        if (post.commentsCount > 0 && post.sharesCount > 0) {
                            Text("•", fontFamily = InterFontFamily, color = AppColors.textSecondary, fontSize = 13.sp)
                        }
                        if (post.sharesCount > 0) {
                            Text(
                                text = "${post.sharesCount} Shares",
                                fontFamily = InterFontFamily,
                                color = AppColors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable(onClick = onShare)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.border, thickness = 1.dp)

            // Action Buttons Row: React, Discuss, Share, Save (Matching Feed & Web)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // React Button with 3D Facebook reactions dock on hold/long-press
                    FacebookReactionButton(
                        isLiked = post.isLiked,
                        myReaction = post.myReaction,
                        likesCount = post.likesCount,
                        onLike = onLike,
                        onReact = onReact,
                        showReactionDock = showReactionDock,
                        onToggleReactionDock = { showReactionDock = it },
                        modifier = Modifier.testTag("like_post_btn_${post.id}")
                    )

                    // Discuss Button
                    DiscussButton(
                        commentCount = post.commentsCount,
                        onClick = onComment
                    )

                    // Share Button (Facing Northeast: ↗)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onShare)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Share",
                            tint = AppColors.textPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer(rotationZ = -35f)
                        )
                        if (post.sharesCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatCount(post.sharesCount),
                                fontFamily = InterFontFamily,
                                color = AppColors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Save Post Button
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Comment Preview with Facebook-Style Compact Threaded Reply support
            if (previewComment != null) {
                val currentComment = previewComment!!
                val threadLineColor = AppColors.borderStrong
                HorizontalDivider(color = AppColors.border.copy(alpha = 0.6f), thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Parent comment row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (currentComment.replies.isNotEmpty()) {
                                    val startX = 19.dp.toPx()
                                    val startY = 38.dp.toPx()
                                    if (size.height > startY) {
                                        drawLine(
                                            color = threadLineColor,
                                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                                            end = androidx.compose.ui.geometry.Offset(startX, size.height),
                                            strokeWidth = 1.5.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            },
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatheredAvatar(
                            name = currentComment.author.name,
                            imageUrl = currentComment.author.profileImageUrl,
                            username = currentComment.author.username,
                            size = 38.dp,
                            onClick = { onProfileClick(currentComment.author.id) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppColors.bgCardElevated)
                                    .clickable(onClick = onComment)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentComment.author.name,
                                            fontFamily = InterFontFamily,
                                            color = AppColors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp
                                        )
                                        if (currentComment.author.isVerified) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            VerifiedBadge(size = 16.dp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentComment.text,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.textBody,
                                        fontSize = 20.sp,
                                        lineHeight = 26.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = if (currentComment.timestamp.isNotBlank()) currentComment.timestamp else "Just now",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (currentComment.isLiked) "❤️ Liked" else "Like",
                                    fontFamily = InterFontFamily,
                                    color = if (currentComment.isLiked) Color(0xFFF43F5E) else AppColors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable(onClick = onComment)
                                )
                                Text(
                                    text = "Reply",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable(onClick = onComment)
                                )
                                if (currentComment.likesCount > 0) {
                                    Text(
                                        text = "❤️ ${currentComment.likesCount}",
                                        fontFamily = InterFontFamily,
                                        color = Color(0xFFF43F5E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Threaded reply preview beneath parent comment if replies exist
                    if (currentComment.replies.isNotEmpty()) {
                        val reply = currentComment.replies.first()
                        val targetUser = reply.replyToAuthorName ?: if (!reply.author.name.equals(currentComment.author.name, ignoreCase = true)) {
                            currentComment.author.name
                        } else null

                        val cleanReplyText = if (targetUser != null && reply.text.startsWith("@$targetUser", ignoreCase = true)) {
                            reply.text.removePrefix("@$targetUser").trimStart()
                        } else {
                            reply.text
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val startX = 19.dp.toPx()
                                    val curveEndX = 38.dp.toPx()
                                    val targetY = 16.dp.toPx()
                                    val stroke = 1.5.dp.toPx()

                                    drawLine(
                                        color = threadLineColor,
                                        start = androidx.compose.ui.geometry.Offset(startX, 0f),
                                        end = androidx.compose.ui.geometry.Offset(startX, targetY - 6.dp.toPx()),
                                        strokeWidth = stroke
                                    )

                                    val path = Path().apply {
                                        moveTo(startX, targetY - 6.dp.toPx())
                                        quadraticBezierTo(
                                            startX, targetY,
                                            curveEndX, targetY
                                        )
                                    }
                                    drawPath(path, color = threadLineColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
                                }
                                .padding(start = 42.dp, top = 6.dp, bottom = 2.dp)
                                .clickable(onClick = onComment),
                            verticalAlignment = Alignment.Top
                        ) {
                            FeatheredAvatar(
                                name = reply.author.name,
                                imageUrl = reply.author.profileImageUrl,
                                size = 24.dp,
                                username = reply.author.username
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(AppColors.bgCardElevated)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    val annotatedString = buildAnnotatedString {
                                        withStyle(SpanStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = AppColors.textPrimary, fontSize = 13.sp)) {
                                            append(reply.author.name)
                                        }
                                        if (!targetUser.isNullOrBlank()) {
                                            append(" ")
                                            withStyle(SpanStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF93C5FD), fontSize = 13.sp)) {
                                                append(targetUser)
                                            }
                                        }
                                        if (cleanReplyText.isNotBlank()) {
                                            append(" ")
                                            withStyle(SpanStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, color = AppColors.textBody, fontSize = 13.sp)) {
                                                append(cleanReplyText)
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = annotatedString,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (reply.author.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge(size = 12.dp)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(start = 6.dp, top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (reply.timestamp.isNotBlank()) reply.timestamp else "Just now",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = AppColors.textSecondary
                                    )
                                    Text(
                                        text = if (reply.isLiked) "❤️ Liked" else "Like",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (reply.isLiked) Color(0xFFF43F5E) else AppColors.textSecondary
                                    )
                                    Text(
                                        text = "Reply",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.textSecondary
                                    )
                                    if (reply.likesCount > 0) {
                                        Text(
                                            text = "❤️ ${reply.likesCount}",
                                            fontFamily = InterFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF43F5E)
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
}

// Dialogs

data class GroupCategoryOption(
    val id: String,
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val features: List<String>
)

val GROUP_CATEGORIES = listOf(
    GroupCategoryOption(
        id = "general",
        label = "General",
        description = "Standard group for discussions and community",
        icon = Icons.Default.Groups,
        color = Color(0xFF1877F2),
        features = listOf("Discussions", "Media sharing", "Member posts")
    ),
    GroupCategoryOption(
        id = "recruitment",
        label = "Careers & Recruitment",
        description = "Find talent, job opportunities, and professional networking",
        icon = Icons.Default.Work,
        color = Color(0xFF45BD62),
        features = listOf("Job postings", "Talent search", "Professional networking")
    ),
    GroupCategoryOption(
        id = "buy_sell",
        label = "Buy and Sell",
        description = "Marketplace for buying, selling, and trading items",
        icon = Icons.Default.Storefront,
        color = Color(0xFFF7B928),
        features = listOf("Item listings", "Price tags", "Location filtering", "Sold/Pending status")
    )
)

data class CurrencyOption(val code: String, val symbol: String, val name: String)

val GROUP_CURRENCIES = listOf(
    CurrencyOption("TSh", "TSh", "Tanzanian Shilling"),
    CurrencyOption("KES", "KSh", "Kenyan Shilling"),
    CurrencyOption("UGX", "USh", "Ugandan Shilling"),
    CurrencyOption("USD", "$", "US Dollar"),
    CurrencyOption("EUR", "€", "Euro"),
    CurrencyOption("GBP", "£", "British Pound"),
    CurrencyOption("ZAR", "R", "South African Rand"),
    CurrencyOption("NGN", "₦", "Nigerian Naira"),
    CurrencyOption("GHS", "GH₵", "Ghanaian Cedi"),
    CurrencyOption("RWF", "RF", "Rwandan Franc")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionDialog(
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }

        Scaffold(
            containerColor = AppColors.bgScreen,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.headerBg)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint
                                )
                            }
                            Text(
                                text = "Choose Group Category",
                                fontFamily = InterFontFamily,
                                color = AppColors.headerText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                selectedId?.let { onSelectCategory(it) }
                            },
                            enabled = selectedId != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                                disabledContainerColor = if (AppColors.isLight) Color.White.copy(alpha = 0.35f) else AppColors.primaryBlue.copy(alpha = 0.4f),
                                contentColor = if (AppColors.isLight) AppColors.primaryBlue else Color.White,
                                disabledContentColor = if (AppColors.isLight) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Continue",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    HorizontalDivider(color = AppColors.headerDivider)
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Intro header
                Text(
                    text = "Select a Category",
                    fontFamily = InterFontFamily,
                    color = AppColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose the category that best describes your group's mission. This configures specialized tools, custom post formats, and features for your community.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppColors.border)

                // Flat categories list
                GROUP_CATEGORIES.forEach { category ->
                    val isSelected = selectedId == category.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = category.id }
                            .background(if (isSelected) AppColors.bgBlueTint else AppColors.bgCard)
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(category.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.label,
                                    tint = category.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.label,
                                        fontFamily = InterFontFamily,
                                        color = AppColors.textPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // Selection circle checkmark
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) AppColors.primaryBlue else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 0.dp else 1.5.dp,
                                                color = if (isSelected) Color.Transparent else AppColors.borderStrong,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = category.description,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    category.features.forEach { feature ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AppColors.bgChip)
                                                .border(
                                                    width = 1.dp,
                                                    color = AppColors.border,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = feature,
                                                fontFamily = InterFontFamily,
                                                color = AppColors.textBody,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = AppColors.border)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    category: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, privacy: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    val categoryInfo = GROUP_CATEGORIES.find { it.id == category } ?: GROUP_CATEGORIES.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }

        Scaffold(
            containerColor = AppColors.bgScreen,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.headerBg)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint
                                )
                            }
                            Text(
                                text = "Create ${categoryInfo.label} Group",
                                fontFamily = InterFontFamily,
                                color = AppColors.headerText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onCreate(name.trim(), description.trim(), if (isPrivate) "private" else "public")
                                }
                            },
                            enabled = name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                                disabledContainerColor = if (AppColors.isLight) Color.White.copy(alpha = 0.35f) else AppColors.primaryBlue.copy(alpha = 0.4f),
                                contentColor = if (AppColors.isLight) AppColors.primaryBlue else Color.White,
                                disabledContentColor = if (AppColors.isLight) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Create",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    HorizontalDivider(color = AppColors.headerDivider)
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Category Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(categoryInfo.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryInfo.icon,
                            contentDescription = categoryInfo.label,
                            tint = categoryInfo.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${categoryInfo.label} Community",
                            fontFamily = InterFontFamily,
                            color = AppColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = categoryInfo.description,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = AppColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(color = AppColors.border)

                // Group Name Field (Required)
                Column {
                    Text(
                        text = "GROUP NAME *",
                        fontFamily = InterFontFamily,
                        color = AppColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        ),
                        placeholder = {
                            Text(
                                text = "What's the name of your community?",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textMuted,
                                fontSize = 16.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppColors.bgCard,
                            unfocusedContainerColor = AppColors.bgCard,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedBorderColor = AppColors.primaryBlue,
                            unfocusedBorderColor = AppColors.borderStrong
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Description Field (Optional - not compulsory)
                Column {
                    Text(
                        text = "DESCRIPTION (OPTIONAL)",
                        fontFamily = InterFontFamily,
                        color = AppColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp
                        ),
                        placeholder = {
                            Text(
                                text = "Tell people what this group is about (optional)...",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.textMuted,
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppColors.bgCard,
                            unfocusedContainerColor = AppColors.bgCard,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedBorderColor = AppColors.primaryBlue,
                            unfocusedBorderColor = AppColors.borderStrong
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }

                // Privacy Section
                Column {
                    Text(
                        text = "PRIVACY",
                        fontFamily = InterFontFamily,
                        color = AppColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.bgCard)
                            .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppColors.bgBlueTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Public Group",
                                    tint = AppColors.primaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isPrivate) "Private Group" else "Public Group",
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isPrivate) "Only members can see who's in the group and what they post" else "Anyone can see who's in the group and what they post",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.primaryBlue
                            )
                        )
                    }
                }

                // Group Features
                Column {
                    Text(
                        text = "GROUP FEATURES:",
                        fontFamily = InterFontFamily,
                        color = AppColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryInfo.features.forEach { feature ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.bgChip)
                                    .border(1.dp, AppColors.border, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF45BD62),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = feature,
                                    fontFamily = InterFontFamily,
                                    color = AppColors.textBody,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupPostDialog(
    group: Group,
    currentUser: User,
    autoPickPhotos: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, Double?, String, String?, String?, String, String?, String?, String?, String?, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var selectedPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Photo picker launcher (Multiple images only, max 10, videos disabled)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedPhotos = (selectedPhotos + uris).distinct().take(10)
        }
    }

    LaunchedEffect(autoPickPhotos) {
        if (autoPickPhotos) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    // Buy & Sell fields
    var selectedCurrency by remember { mutableStateOf("TSh") }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var priceStr by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Used - Good") }
    var showConditionMenu by remember { mutableStateOf(false) }
    val conditionOptions = listOf("New", "Like New", "Used - Like New", "Used - Good", "Used - Fair", "For Parts/Not Working")
    var location by remember { mutableStateOf("") }

    // Recruitment / Careers fields
    val isCareer = group.category == "recruitment" || isCareerCategory(group.category, group.name)
    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var jobLocation by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("Full-time") }
    var showJobTypeMenu by remember { mutableStateOf(false) }
    val jobTypeOptions = listOf("Full-time", "Part-time", "Contract", "Internship", "Freelance")
    var applicationType by remember { mutableStateOf<String?>(null) } // "email" or "link"
    var applicationValue by remember { mutableStateOf("") }

    val canPublish = content.isNotBlank() || selectedPhotos.isNotEmpty() ||
        (isCareer && jobTitle.isNotBlank()) ||
        (group.category == "buy_sell" && (priceStr.isNotBlank() || content.isNotBlank()))

    val modalTitle = when {
        group.category == "buy_sell" -> "Sell an Item"
        isCareer -> "Post a Job Opening"
        else -> "Create Group Post"
    }

    val submitButtonText = when {
        group.category == "buy_sell" -> "List Item in Group"
        isCareer -> "Post Job"
        else -> "Post to Community"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }

        Scaffold(
            containerColor = AppColors.bgScreen,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.headerBg)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerIconTint
                                )
                            }
                            Text(
                                text = modalTitle,
                                color = AppColors.headerText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppColors.headerIconBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppColors.headerIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = AppColors.headerDivider)
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.bgCard)
                        .navigationBarsPadding()
                ) {
                    // "Add to your post" bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.bgCard)
                            .border(1.dp, AppColors.border)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Add to your post",
                            color = AppColors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.bgInput)
                                .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Photos",
                                tint = Color(0xFF45BD62),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (selectedPhotos.isNotEmpty()) "${selectedPhotos.size} photo${if (selectedPhotos.size > 1) "s" else ""}" else "Photos",
                                color = AppColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily
                            )
                        }
                    }

                    // Full-width Submit button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.bgCard)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (canPublish) {
                                    val photoUriStrings = selectedPhotos.map { it.toString() }
                                    val parsedPrice = priceStr.toDoubleOrNull()
                                    val finalAppType = if (applicationValue.isNotBlank()) (applicationType ?: if (applicationValue.contains("@")) "email" else "link") else null
                                    val finalAppVal = applicationValue.ifBlank { null }
                                    onSubmit(
                                        content.trim(),
                                        photoUriStrings,
                                        parsedPrice,
                                        selectedCurrency,
                                        condition.ifBlank { null },
                                        if (isCareer) jobLocation.ifBlank { null } else location.ifBlank { null },
                                        "available",
                                        jobTitle.ifBlank { null },
                                        company.ifBlank { null },
                                        jobType,
                                        salary.ifBlank { null },
                                        finalAppType,
                                        finalAppVal
                                    )
                                }
                            },
                            enabled = canPublish,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primaryBlue,
                                disabledContainerColor = AppColors.primaryBlue.copy(alpha = 0.4f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = submitButtonText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.bgCard)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Author row: Avatar, Name, "Posting to {group.name}"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatheredAvatar(
                        name = currentUser.name,
                        imageUrl = currentUser.profileImageUrl,
                        size = 48.dp,
                        username = currentUser.username
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.name,
                            color = AppColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = AppColors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Posting to ${group.name}",
                                color = AppColors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = AppColors.border)

                // Buy & Sell Fields
                if (group.category == "buy_sell") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Price with Currency
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Price",
                                    color = AppColors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Currency selector
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AppColors.bgInput)
                                                .border(1.dp, AppColors.borderStrong, RoundedCornerShape(12.dp))
                                                .clickable { showCurrencyMenu = true }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = selectedCurrency,
                                                color = AppColors.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = InterFontFamily
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = AppColors.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showCurrencyMenu,
                                            onDismissRequest = { showCurrencyMenu = false },
                                            modifier = Modifier.background(AppColors.bgCard)
                                        ) {
                                            GROUP_CURRENCIES.forEach { curr ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "${curr.code} (${curr.name})",
                                                            color = AppColors.textPrimary,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            fontFamily = InterFontFamily
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedCurrency = curr.code
                                                        showCurrencyMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Price numeric input
                                    OutlinedTextField(
                                        value = priceStr,
                                        onValueChange = { priceStr = it },
                                        textStyle = LocalTextStyle.current.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        placeholder = { Text("Price", color = AppColors.textMuted, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = AppColors.bgInput,
                                            unfocusedContainerColor = AppColors.bgInput,
                                            focusedTextColor = AppColors.textPrimary,
                                            unfocusedTextColor = AppColors.textPrimary,
                                            focusedBorderColor = AppColors.primaryBlue,
                                            unfocusedBorderColor = AppColors.borderStrong
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Condition
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Condition",
                                    color = AppColors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AppColors.bgInput)
                                            .border(1.dp, AppColors.borderStrong, RoundedCornerShape(12.dp))
                                            .clickable { showConditionMenu = true }
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = condition,
                                            color = AppColors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = AppColors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showConditionMenu,
                                        onDismissRequest = { showConditionMenu = false },
                                        modifier = Modifier.background(AppColors.bgCard)
                                    ) {
                                        conditionOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, color = AppColors.textPrimary, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    condition = opt
                                                    showConditionMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Location
                        Column {
                            Text(
                                text = "Location",
                                color = AppColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                placeholder = { Text("City, State / Area", color = AppColors.textMuted, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    HorizontalDivider(color = AppColors.border)
                }

                // Recruitment / Careers Fields
                if (isCareer) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Job Title
                        Column {
                            Text(
                                text = "Job Title",
                                color = AppColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = jobTitle,
                                onValueChange = { jobTitle = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                placeholder = { Text("e.g. Senior Frontend Engineer", color = AppColors.textMuted, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Company
                        Column {
                            Text(
                                text = "Company",
                                color = AppColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = company,
                                onValueChange = { company = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                placeholder = { Text("Company name", color = AppColors.textMuted, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Job Type & Salary Range Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Job Type Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Job Type",
                                    color = AppColors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AppColors.bgInput)
                                            .border(1.dp, AppColors.borderStrong, RoundedCornerShape(12.dp))
                                            .clickable { showJobTypeMenu = true }
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = jobType,
                                            color = AppColors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = InterFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = AppColors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showJobTypeMenu,
                                        onDismissRequest = { showJobTypeMenu = false },
                                        modifier = Modifier.background(AppColors.bgCard)
                                    ) {
                                        jobTypeOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, color = AppColors.textPrimary, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    jobType = opt
                                                    showJobTypeMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Salary Range
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Salary Range",
                                    color = AppColors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = salary,
                                    onValueChange = { salary = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    placeholder = { Text("e.g. $70k - $90k", color = AppColors.textMuted, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = AppColors.bgInput,
                                        unfocusedContainerColor = AppColors.bgInput,
                                        focusedTextColor = AppColors.textPrimary,
                                        unfocusedTextColor = AppColors.textPrimary,
                                        focusedBorderColor = AppColors.primaryBlue,
                                        unfocusedBorderColor = AppColors.borderStrong
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Job Location
                        Column {
                            Text(
                                text = "Job Location",
                                color = AppColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = jobLocation,
                                onValueChange = { jobLocation = it },
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                ),
                                placeholder = { Text("e.g. San Francisco, CA / Remote", color = AppColors.textMuted, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.borderStrong
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Application Method (Email or Link)
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = "How can candidates apply?",
                                color = AppColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = "Optional — if left empty, Apply button is hidden",
                                color = AppColors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isEmailSelected = applicationType == "email"
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isEmailSelected) AppColors.bgBlueTint else AppColors.bgInput)
                                        .border(
                                            1.dp,
                                            if (isEmailSelected) AppColors.primaryBlue else AppColors.borderStrong,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { applicationType = "email" }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (isEmailSelected) AppColors.primaryBlue else AppColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "By Email",
                                        color = if (isEmailSelected) AppColors.primaryBlue else AppColors.textSecondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }

                                val isLinkSelected = applicationType == "link"
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isLinkSelected) AppColors.bgBlueTint else AppColors.bgInput)
                                        .border(
                                            1.dp,
                                            if (isLinkSelected) AppColors.primaryBlue else AppColors.borderStrong,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { applicationType = "link" }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = if (isLinkSelected) AppColors.primaryBlue else AppColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "By External Link",
                                        color = if (isLinkSelected) AppColors.primaryBlue else AppColors.textSecondary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }

                            if (applicationType == "email") {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = applicationValue,
                                    onValueChange = { applicationValue = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = AppColors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    placeholder = { Text("e.g. jobs@company.com", color = AppColors.textMuted, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = AppColors.bgInput,
                                        unfocusedContainerColor = AppColors.bgInput,
                                        focusedTextColor = AppColors.textPrimary,
                                        unfocusedTextColor = AppColors.textPrimary,
                                        focusedBorderColor = AppColors.primaryBlue,
                                        unfocusedBorderColor = AppColors.borderStrong
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (applicationType == "link") {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = applicationValue,
                                    onValueChange = { applicationValue = it },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = AppColors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    placeholder = { Text("e.g. https://company.com/careers/apply", color = AppColors.textMuted, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = AppColors.bgInput,
                                        unfocusedContainerColor = AppColors.bgInput,
                                        focusedTextColor = AppColors.textPrimary,
                                        unfocusedTextColor = AppColors.textPrimary,
                                        focusedBorderColor = AppColors.primaryBlue,
                                        unfocusedBorderColor = AppColors.borderStrong
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (applicationType != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Remove application method (hide Apply button)",
                                        color = AppColors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = InterFontFamily,
                                        modifier = Modifier.clickable {
                                            applicationType = null
                                            applicationValue = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = AppColors.border)
                }

                // Large Professional Typing Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        ),
                        placeholder = {
                            Text(
                                text = when {
                                    group.category == "buy_sell" -> "Describe your item in detail (features, condition, specifications, warranty, why you're selling)..."
                                    isCareer -> "Describe the role, responsibilities, requirements, qualifications, and how to apply..."
                                    else -> "What's on your mind? Share thoughts, ask questions, or contribute to ${group.name}..."
                                },
                                color = AppColors.textMuted,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = InterFontFamily
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AppColors.textPrimary,
                            unfocusedTextColor = AppColors.textPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AppColors.primaryBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                    )
                }

                // Attached Photo Previews (Aspect-square thumbnails with remove button)
                if (selectedPhotos.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${selectedPhotos.size} photo${if (selectedPhotos.size > 1) "s" else ""} attached",
                            color = AppColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            selectedPhotos.forEachIndexed { index, uri ->
                                Box(modifier = Modifier.size(80.dp)) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Photo ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedPhotos = selectedPhotos.filter { it != uri }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.75f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            if (selectedPhotos.size < 10) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.bgCardElevated)
                                        .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add More",
                                        tint = AppColors.textSecondary,
                                        modifier = Modifier.size(24.dp)
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
}

@Composable
fun InviteFollowersDialog(
    group: Group,
    followers: List<User>,
    onDismiss: () -> Unit,
    onInvite: (Long) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val invitedIds = remember { mutableStateListOf<Long>() }

    val filtered = followers.filter {
        search.isBlank() || it.name.contains(search, ignoreCase = true) || it.username.contains(search, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Followers to ${group.name}", color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    placeholder = { Text("Search users...", color = AppColors.textMuted, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.borderStrong
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(filtered) { user ->
                        val alreadyInvited = invitedIds.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = user.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(user.name, color = AppColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    Text("@${user.username}", color = AppColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily)
                                }
                            }
                            Button(
                                onClick = {
                                    if (!alreadyInvited) {
                                        invitedIds.add(user.id)
                                        onInvite(user.id)
                                    }
                                },
                                enabled = !alreadyInvited,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (alreadyInvited) AppColors.bgChip else AppColors.primaryBlue,
                                    contentColor = if (alreadyInvited) AppColors.textSecondary else Color.White,
                                    disabledContainerColor = AppColors.bgChip,
                                    disabledContentColor = AppColors.textSecondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (alreadyInvited) "Invited" else "Invite", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = AppColors.primaryBlue, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
        },
        containerColor = AppColors.bgCard
    )
}

@Composable
fun ShareGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Community", color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily) },
        text = {
            Column {
                Text(group.name, color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(4.dp))
                Text("https://unera.social/groups/${group.id}", color = AppColors.primaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCopyLink,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Group Link", fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AppColors.textSecondary, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
        },
        containerColor = AppColors.bgCard
    )
}

@Composable
fun GroupSettingsDialog(
    group: Group,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, postingAllowed: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var desc by remember { mutableStateOf(group.description) }
    var postingAllowed by remember { mutableStateOf(group.memberPostingAllowed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group Settings", color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    label = { Text("Group Name", color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.borderStrong
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal
                    ),
                    label = { Text("Description", color = AppColors.textSecondary, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.bgInput,
                        unfocusedContainerColor = AppColors.bgInput,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary,
                        focusedBorderColor = AppColors.primaryBlue,
                        unfocusedBorderColor = AppColors.borderStrong
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Member Posting", color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = InterFontFamily)
                        Text("When off, only admin can post", color = AppColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily)
                    }
                    Switch(
                        checked = postingAllowed,
                        onCheckedChange = { postingAllowed = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.dangerBg, contentColor = AppColors.dangerText),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Group", color = AppColors.dangerText, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), desc.trim(), postingAllowed) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBlue, contentColor = Color.White)
            ) {
                Text("Save", fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.textSecondary, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
        },
        containerColor = AppColors.bgCard
    )
}
