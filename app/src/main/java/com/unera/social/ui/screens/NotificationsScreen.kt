package com.unera.social.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unera.social.model.Group
import com.unera.social.model.Notification
import com.unera.social.model.NotificationType
import com.unera.social.model.User
import com.unera.social.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val INITIAL_NOTIFICATIONS_COUNT = 10
private const val LOAD_MORE_NOTIFICATIONS_COUNT = 5

@Composable
fun NotificationsScreen(
    notifications: List<Notification>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    users: List<User> = emptyList(),
    groups: List<Group> = emptyList(),
    currentUser: User? = null,
    onProfileClick: (Long) -> Unit = {},
    onOpenNotification: ((Notification) -> Unit)? = null,
    onMarkAllAsRead: (() -> Unit)? = null,
    onDeleteNotification: ((String) -> Unit)? = null,
    onToggleRead: ((String, Boolean) -> Unit)? = null,
    onAcceptGroupInvite: ((String, Long?) -> Unit)? = null,
    onDeclineGroupInvite: ((String, Long?) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var visibleNotificationsCount by remember { mutableIntStateOf(INITIAL_NOTIFICATIONS_COUNT) }
    var activeDropdownId by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isToastError by remember { mutableStateOf(false) }
    var groupInviteStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) } // notificationId -> "joined" / "declined"
    var groupInviteLoading by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    fun showToast(message: String, isError: Boolean = false) {
        toastMessage = message
        isToastError = isError
        scope.launch {
            delay(2800)
            if (toastMessage == message) {
                toastMessage = null
            }
        }
    }

    // Filter notifications by search
    val filteredNotifications = remember(notifications, searchQuery) {
        if (searchQuery.isBlank()) {
            notifications
        } else {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            notifications.filter { notif ->
                notif.user.name.lowercase(Locale.ROOT).contains(query) ||
                        notif.content.lowercase(Locale.ROOT).contains(query) ||
                        notif.rawType.lowercase(Locale.ROOT).contains(query) ||
                        notif.reactionType.lowercase(Locale.ROOT).contains(query) ||
                        (notif.groupName?.lowercase(Locale.ROOT)?.contains(query) == true) ||
                        (notif.previewText?.lowercase(Locale.ROOT)?.contains(query) == true)
            }
        }
    }

    // Sort by timestamp descending
    val sortedNotifications = remember(filteredNotifications) {
        filteredNotifications.sortedByDescending { it.timestamp }
    }

    // Limit visible notifications to 10 initially, +5 on each "See More" click
    val visibleNotifications = remember(sortedNotifications, visibleNotificationsCount) {
        sortedNotifications.take(visibleNotificationsCount)
    }

    // Partition visible notifications into New, Today, Earlier
    val (newNotifications, todayNotifications, earlierNotifications) = remember(visibleNotifications) {
        val twoHoursAgo = System.currentTimeMillis() - 7200 * 1000
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis

        val newNotifs = mutableListOf<Notification>()
        val todayNotifs = mutableListOf<Notification>()
        val earlierNotifs = mutableListOf<Notification>()

        visibleNotifications.forEach { notif ->
            if (!notif.isRead || notif.timestamp >= twoHoursAgo) {
                newNotifs.add(notif)
            } else if (notif.timestamp >= startOfToday) {
                todayNotifs.add(notif)
            } else {
                earlierNotifs.add(notif)
            }
        }

        Triple(newNotifs, todayNotifs, earlierNotifs)
    }

    val canShowMore = visibleNotificationsCount < sortedNotifications.size ||
        (hasMore && onLoadMore != null && searchQuery.isBlank())
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.bgScreen)
            .testTag("notifications_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(
                color = AppColors.headerBg,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("notifications_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppColors.headerText
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Notifications",
                                fontSize = 24.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.headerText
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mark all as read button
                            IconButton(
                                onClick = {
                                    if (unreadCount > 0) {
                                        onMarkAllAsRead?.invoke()
                                        showToast("All notifications marked as read")
                                    }
                                },
                                enabled = unreadCount > 0,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("notifications_mark_all_read_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Mark all as read",
                                    tint = if (unreadCount > 0) {
                                        if (AppColors.isLight) Color.White else UneraBlue
                                    } else {
                                        if (AppColors.isLight) Color.White.copy(alpha = 0.42f) else Navy700
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Search toggle button
                            IconButton(
                                onClick = {
                                    isSearchVisible = !isSearchVisible
                                    if (!isSearchVisible) searchQuery = ""
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSearchVisible) {
                                            if (AppColors.isLight) AppColors.headerIconBgActive else UneraBlue.copy(alpha = 0.2f)
                                        } else Color.Transparent
                                    )
                                    .testTag("notifications_search_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search notifications",
                                    tint = if (isSearchVisible) {
                                        if (AppColors.isLight) Color.White else UneraBlue
                                    } else {
                                        if (AppColors.isLight) Color.White else TextSecondary
                                    }
                                )
                            }

                            if (onRefresh != null) {
                                Spacer(modifier = Modifier.width(4.dp))

                                // Manual refresh button
                                IconButton(
                                    onClick = {
                                        visibleNotificationsCount = INITIAL_NOTIFICATIONS_COUNT
                                        onRefresh()
                                        showToast("Checking for new notifications...")
                                    },
                                    enabled = !isRefreshing,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .testTag("notifications_refresh_btn")
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = if (AppColors.isLight) Color.White else UneraBlue,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh notifications",
                                            tint = if (AppColors.isLight) Color.White else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search input field
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notifications...", color = AppColors.textSecondary, fontSize = 14.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = AppColors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (AppColors.isLight) Color.White else AppColors.bgCardDeep,
                                unfocusedContainerColor = if (AppColors.isLight) Color.White else AppColors.bgCardDeep,
                                focusedBorderColor = AppColors.primaryBlue,
                                unfocusedBorderColor = if (AppColors.isLight) Color.White.copy(alpha = 0.7f) else AppColors.borderStrong,
                                focusedTextColor = AppColors.textPrimary,
                                unfocusedTextColor = AppColors.textPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                                .testTag("notifications_search_input")
                        )
                    }
                }
            }

            HorizontalDivider(color = AppColors.headerDivider, thickness = 1.dp)

            // Content List
            if (isRefreshing && sortedNotifications.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.primaryBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading notifications...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connecting to live notifications API",
                            fontSize = 13.sp,
                            color = AppColors.textSecondary
                        )
                    }
                }
            } else if (sortedNotifications.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(AppColors.bgCard)
                                .border(1.dp, AppColors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = AppColors.primaryBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "No notifications matching search" else "No notifications yet",
                            fontSize = 18.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching for a different person or keyword" else "When people like, comment, or interact with your content, you'll see them here.",
                            fontSize = 14.sp,
                            color = AppColors.textSecondary,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notifications_list"),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 1. New Section
                    if (newNotifications.isNotEmpty()) {
                        item(key = "header_new") {
                            SectionHeader(
                                title = "New",
                                badgeCount = newNotifications.size,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(newNotifications, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                isMenuOpen = activeDropdownId == notif.id,
                                onMenuToggle = { activeDropdownId = if (activeDropdownId == notif.id) null else notif.id },
                                onMenuDismiss = { if (activeDropdownId == notif.id) activeDropdownId = null },
                                onProfileClick = onProfileClick,
                                onOpen = {
                                    if (!notif.isRead) onToggleRead?.invoke(notif.id, true)
                                    onOpenNotification?.invoke(notif) ?: onProfileClick(notif.user.id)
                                },
                                onToggleRead = {
                                    activeDropdownId = null
                                    val newReadState = !notif.isRead
                                    onToggleRead?.invoke(notif.id, newReadState)
                                    showToast(if (newReadState) "Marked as read" else "Marked as unread")
                                },
                                onDelete = {
                                    activeDropdownId = null
                                    onDeleteNotification?.invoke(notif.id)
                                    showToast("Notification removed")
                                },
                                inviteStatus = groupInviteStatus[notif.id],
                                isInviteLoading = groupInviteLoading[notif.id] == true,
                                onAcceptInvite = {
                                    groupInviteLoading = groupInviteLoading + (notif.id to true)
                                    scope.launch {
                                        delay(400)
                                        groupInviteStatus = groupInviteStatus + (notif.id to "joined")
                                        groupInviteLoading = groupInviteLoading + (notif.id to false)
                                        onAcceptGroupInvite?.invoke(notif.id, notif.groupId)
                                        showToast("Joined ${notif.groupName ?: "group"}!")
                                    }
                                },
                                onDeclineInvite = {
                                    groupInviteLoading = groupInviteLoading + (notif.id to true)
                                    scope.launch {
                                        delay(400)
                                        groupInviteStatus = groupInviteStatus + (notif.id to "declined")
                                        groupInviteLoading = groupInviteLoading + (notif.id to false)
                                        onDeclineGroupInvite?.invoke(notif.id, notif.groupId)
                                        showToast("Invitation declined")
                                    }
                                }
                            )
                            HorizontalDivider(color = AppColors.border.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }

                    // 2. Today Section
                    if (todayNotifications.isNotEmpty()) {
                        item(key = "header_today") {
                            SectionHeader(
                                title = "Today",
                                badgeCount = null,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                        items(todayNotifications, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                isMenuOpen = activeDropdownId == notif.id,
                                onMenuToggle = { activeDropdownId = if (activeDropdownId == notif.id) null else notif.id },
                                onMenuDismiss = { if (activeDropdownId == notif.id) activeDropdownId = null },
                                onProfileClick = onProfileClick,
                                onOpen = {
                                    if (!notif.isRead) onToggleRead?.invoke(notif.id, true)
                                    onOpenNotification?.invoke(notif) ?: onProfileClick(notif.user.id)
                                },
                                onToggleRead = {
                                    activeDropdownId = null
                                    val newReadState = !notif.isRead
                                    onToggleRead?.invoke(notif.id, newReadState)
                                    showToast(if (newReadState) "Marked as read" else "Marked as unread")
                                },
                                onDelete = {
                                    activeDropdownId = null
                                    onDeleteNotification?.invoke(notif.id)
                                    showToast("Notification removed")
                                },
                                inviteStatus = groupInviteStatus[notif.id],
                                isInviteLoading = groupInviteLoading[notif.id] == true,
                                onAcceptInvite = {
                                    groupInviteLoading = groupInviteLoading + (notif.id to true)
                                    scope.launch {
                                        delay(400)
                                        groupInviteStatus = groupInviteStatus + (notif.id to "joined")
                                        groupInviteLoading = groupInviteLoading + (notif.id to false)
                                        onAcceptGroupInvite?.invoke(notif.id, notif.groupId)
                                        showToast("Joined ${notif.groupName ?: "group"}!")
                                    }
                                },
                                onDeclineInvite = {
                                    groupInviteLoading = groupInviteLoading + (notif.id to true)
                                    scope.launch {
                                        delay(400)
                                        groupInviteStatus = groupInviteStatus + (notif.id to "declined")
                                        groupInviteLoading = groupInviteLoading + (notif.id to false)
                                        onDeclineGroupInvite?.invoke(notif.id, notif.groupId)
                                        showToast("Invitation declined")
                                    }
                                }
                            )
                            HorizontalDivider(color = AppColors.border.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }

                    // 3. Earlier Section
                    if (earlierNotifications.isNotEmpty()) {
                        item(key = "header_earlier") {
                            SectionHeader(
                                title = "Earlier",
                                badgeCount = null,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                        items(earlierNotifications, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                isMenuOpen = activeDropdownId == notif.id,
                                onMenuToggle = { activeDropdownId = if (activeDropdownId == notif.id) null else notif.id },
                                onMenuDismiss = { if (activeDropdownId == notif.id) activeDropdownId = null },
                                onProfileClick = onProfileClick,
                                onOpen = {
                                    if (!notif.isRead) onToggleRead?.invoke(notif.id, true)
                                    onOpenNotification?.invoke(notif) ?: onProfileClick(notif.user.id)
                                },
                                onToggleRead = {
                                    activeDropdownId = null
                                    val newReadState = !notif.isRead
                                    onToggleRead?.invoke(notif.id, newReadState)
                                    showToast(if (newReadState) "Marked as read" else "Marked as unread")
                                },
                                onDelete = {
                                    activeDropdownId = null
                                    onDeleteNotification?.invoke(notif.id)
                                    showToast("Notification removed")
                                },
                                inviteStatus = groupInviteStatus[notif.id],
                                isInviteLoading = groupInviteLoading[notif.id] == true,
                                onAcceptInvite = {
                                    groupInviteLoading = groupInviteLoading + (notif.id to true)
                                    scope.launch {
                                        delay(400)
                                        groupInviteStatus = groupInviteStatus + (notif.id to "joined")
                                        groupInviteLoading = groupInviteLoading + (notif.id to false)
                                        onAcceptGroupInvite?.invoke(notif.id, notif.groupId)
                                        showToast("Joined ${notif.groupName ?: "group"}!")
                                    }
                                },
                                onDeclineInvite = {
                                    groupInviteLoading = groupInviteLoading + (notif.id to true)
                                    scope.launch {
                                        delay(400)
                                        groupInviteStatus = groupInviteStatus + (notif.id to "declined")
                                        groupInviteLoading = groupInviteLoading + (notif.id to false)
                                        onDeclineGroupInvite?.invoke(notif.id, notif.groupId)
                                        showToast("Invitation declined")
                                    }
                                }
                            )
                            HorizontalDivider(color = AppColors.border.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }

                    // Pagination Button ("See More" requests 5 more from database)
                    if (canShowMore) {
                        item(key = "pagination_btn") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isLoadingMore) {
                                            visibleNotificationsCount += LOAD_MORE_NOTIFICATIONS_COUNT
                                            onLoadMore?.invoke()
                                        }
                                    },
                                    enabled = !isLoadingMore,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.bgCard,
                                        contentColor = AppColors.textPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .border(1.dp, AppColors.borderStrong, RoundedCornerShape(12.dp))
                                        .testTag("notifications_load_more_btn")
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = AppColors.primaryBlue,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading more...",
                                            fontSize = 15.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = "See More",
                                            fontSize = 15.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Toast Feedback
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                color = if (isToastError) AppColors.dangerBg else AppColors.bgCard,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isToastError) AppColors.dangerText.copy(alpha = 0.5f) else AppColors.primaryBlue
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .testTag("notifications_toast")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isToastError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isToastError) AppColors.dangerText else AppColors.primaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toastMessage.orEmpty(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badgeCount: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary
        )

        if (badgeCount != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.bgBlueTint)
                    .border(1.dp, AppColors.primaryBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeCount.toString(),
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    isMenuOpen: Boolean,
    onMenuToggle: () -> Unit,
    onMenuDismiss: () -> Unit,
    onProfileClick: (Long) -> Unit,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit,
    inviteStatus: String?,
    isInviteLoading: Boolean,
    onAcceptInvite: () -> Unit,
    onDeclineInvite: () -> Unit
) {
    val isUnread = !notification.isRead
    val defaultBadgeBg = AppColors.bgChip
    val badgeInfo = remember(notification, defaultBadgeBg) { getNotificationBadge(notification, defaultBadgeBg) }
    val cardBg = if (isUnread) {
        if (AppColors.isLight) Color(0xFFE7F3FF).copy(alpha = 0.55f) else UneraBlue.copy(alpha = 0.08f)
    } else {
        if (AppColors.isLight) AppColors.bgCard else Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onOpen
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Actor Avatar with Badge Overlay
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clickable { onProfileClick(notification.user.id) }
            ) {
                AsyncImage(
                    model = notification.user.profileImageUrl,
                    contentDescription = notification.user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(AppColors.bgChip)
                        .border(1.dp, AppColors.border, CircleShape)
                )

                // Bottom-right status badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(badgeInfo.backgroundColor)
                        .border(2.dp, AppColors.bgCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (badgeInfo.emoji != null) {
                        Text(
                            text = badgeInfo.emoji,
                            fontSize = 11.sp,
                            lineHeight = 11.sp
                        )
                    } else if (badgeInfo.icon != null) {
                        Icon(
                            imageVector = badgeInfo.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text and Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                // Actor Name (21sp Bold) + Action description (20sp Regular)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = buildMiddleText(notification, AppColors.textPrimary),
                        fontSize = 20.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = if (isUnread) AppColors.textPrimary else AppColors.textSecondary,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Multi-actor overlapping avatars if any
                if (notification.actorsCount > 1 && notification.actors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        notification.actors.take(3).forEachIndexed { idx, actor ->
                            AsyncImage(
                                model = actor.profileImageUrl,
                                contentDescription = actor.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .offset(x = (-idx * 8).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, AppColors.bgCard, CircleShape)
                                    .clickable { onProfileClick(actor.id) }
                            )
                        }
                    }
                }

                // Quoted Preview Card (if previewText or previewImage exists)
                if (!notification.previewText.isNullOrBlank() || !notification.previewImage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.bgInput)
                            .border(1.dp, AppColors.border, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!notification.previewImage.isNullOrBlank()) {
                            AsyncImage(
                                model = notification.previewImage,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.bgChip)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (!notification.previewText.isNullOrBlank()) {
                            Text(
                                text = "“${notification.previewText}”",
                                fontSize = 16.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Italic,
                                color = AppColors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Group Invite Actions
                val isGroupInvite = notification.type == NotificationType.GROUP_INVITE ||
                        notification.rawType.lowercase(Locale.ROOT) == "group_invite" ||
                        notification.entityType.lowercase(Locale.ROOT) == "group_invite"

                if (isGroupInvite) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (inviteStatus == "joined") {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Joined Group",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    } else if (inviteStatus == "declined") {
                        Text(
                            text = "Invitation declined",
                            fontSize = 12.sp,
                            color = AppColors.textSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onAcceptInvite,
                                enabled = !isInviteLoading,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.primaryBlue,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (isInviteLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GroupAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Join Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = onDeclineInvite,
                                enabled = !isInviteLoading,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AppColors.textSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.borderStrong),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time ago + Reaction pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = notification.timeAgo.ifBlank { formatRelativeTime(notification.timestamp) },
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = if (isUnread) (if (AppColors.isLight) AppColors.primaryBlue else Color(0xFF38BDF8)) else AppColors.textSecondary,
                        fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal
                    )

                    // Reaction Pill badge if reaction
                    if (notification.type == NotificationType.REACTION && notification.reactionType.isNotBlank()) {
                        val reactionEmoji = getReactionEmoji(notification.reactionType)
                        if (reactionEmoji.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.bgChip)
                                    .border(1.dp, AppColors.border, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = reactionEmoji,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Right side: Blue unread indicator + Three dots menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AppColors.primaryBlue)
                            .shadow(4.dp, CircleShape)
                    )
                }

                Box {
                    IconButton(
                        onClick = onMenuToggle,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = onMenuDismiss,
                        modifier = Modifier
                            .background(AppColors.bgCard)
                            .border(1.dp, AppColors.borderStrong, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isUnread) "Mark as read" else "Mark as unread",
                                    fontSize = 13.sp,
                                    color = AppColors.textPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isUnread) Icons.Default.Check else Icons.Default.MarkEmailUnread,
                                    contentDescription = null,
                                    tint = AppColors.primaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = onToggleRead
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Remove this notification",
                                    fontSize = 13.sp,
                                    color = AppColors.dangerText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = AppColors.dangerText,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

private data class BadgeInfo(
    val emoji: String? = null,
    val icon: ImageVector? = null,
    val backgroundColor: Color = Color(0xFF1877F2)
)

private fun getNotificationBadge(notification: Notification, defaultBg: Color = Navy800): BadgeInfo {
    val rawType = notification.rawType.lowercase(Locale.ROOT)
    val entityType = notification.entityType.lowercase(Locale.ROOT)
    val reactionType = notification.reactionType.lowercase(Locale.ROOT)

    // Check reaction first
    val reactionEmoji = getReactionEmoji(reactionType)
    if (reactionEmoji.isNotBlank()) {
        return BadgeInfo(emoji = reactionEmoji, backgroundColor = defaultBg)
    }

    return when {
        rawType.contains("birthday") || notification.type == NotificationType.BIRTHDAY ->
            BadgeInfo(emoji = "🎂", backgroundColor = defaultBg)
        rawType.contains("comment") || rawType.contains("reply") || notification.type == NotificationType.COMMENT ->
            BadgeInfo(icon = Icons.Default.ChatBubble, backgroundColor = Color(0xFF10B981))
        rawType.contains("follow") || notification.type == NotificationType.FOLLOW ->
            BadgeInfo(icon = Icons.Default.PersonAdd, backgroundColor = UneraBlue)
        rawType.contains("share") || notification.type == NotificationType.SHARE ->
            BadgeInfo(icon = Icons.Default.Share, backgroundColor = UneraBlue)
        entityType == "song" || rawType == "song" || notification.type == NotificationType.MUSIC ->
            BadgeInfo(icon = Icons.Default.MusicNote, backgroundColor = Color(0xFF6366F1))
        entityType == "event" || rawType == "event" || notification.type == NotificationType.EVENT ->
            BadgeInfo(icon = Icons.Default.Event, backgroundColor = Color(0xFF2563EB))
        entityType == "group" || entityType == "group_post" || entityType == "group_invite" ||
                rawType.contains("group") || notification.type == NotificationType.GROUP_INVITE ->
            BadgeInfo(icon = Icons.Default.Groups, backgroundColor = UneraBlue)
        entityType == "reel" || entityType == "video" ->
            BadgeInfo(icon = Icons.Default.Videocam, backgroundColor = Color(0xFFE11D48))
        entityType == "story" ->
            BadgeInfo(icon = Icons.Default.Bolt, backgroundColor = Color(0xFF06B6D4))
        entityType == "product" || rawType.contains("product") ->
            BadgeInfo(icon = Icons.Default.ShoppingBag, backgroundColor = Color(0xFF0284C7))
        notification.type == NotificationType.MESSAGE ->
            BadgeInfo(icon = Icons.Default.Mail, backgroundColor = Color(0xFF10B981))
        else ->
            BadgeInfo(icon = Icons.Default.Notifications, backgroundColor = UneraBlue)
    }
}

private fun getReactionEmoji(reactionType: String): String {
    val lower = reactionType.lowercase(Locale.ROOT)
    return when {
        lower.contains("love") || lower.contains("heart") -> "❤️"
        lower.contains("haha") || lower.contains("laugh") -> "😂"
        lower.contains("wow") -> "😮"
        lower.contains("sad") -> "😢"
        lower.contains("angry") -> "😡"
        lower.contains("fire") -> "🔥"
        lower.contains("party") -> "🎉"
        lower.contains("clap") -> "👏"
        lower.contains("like") -> "👍"
        else -> ""
    }
}

private fun buildMiddleText(
    notification: Notification,
    actorNameColor: Color
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val actorName = notification.user.name
        val actorsCount = notification.actorsCount
        val otherCount = maxOf(0, actorsCount - 1)

        // Bold actor name (21sp, Bold 700, Inter)
        withStyle(
            SpanStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = actorNameColor
            )
        ) {
            append(actorName)
        }

        // Notification text (20sp, Regular 400, Inter)
        withStyle(
            SpanStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp
            )
        ) {
            if (otherCount > 0) {
                append(" and $otherCount ${if (otherCount == 1) "other" else "others"}")
            }

            append(" ")

            // Content / Action description
            if (notification.content.isNotBlank()) {
                val contentToAppend = notification.content.removePrefix(actorName).trim()
                append(contentToAppend)
            } else {
                append("interacted with your content.")
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0) return "Just now"
    val diff = System.currentTimeMillis() - timestamp
    val minute = 60 * 1000L
    val hour = 60 * minute
    val day = 24 * hour

    return when {
        diff < minute -> "Just now"
        diff < hour -> "${diff / minute}m ago"
        diff < day -> "${diff / hour}h ago"
        diff < 7 * day -> "${diff / day}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
