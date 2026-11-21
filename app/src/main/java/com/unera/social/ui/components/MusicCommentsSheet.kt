package com.unera.social.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.AudioTrack
import com.unera.social.model.SongComment
import com.unera.social.model.User
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import java.text.SimpleDateFormat
import java.util.*

private const val DEFAULT_MUSIC_COVER = "https://media.featheredsocial.site/task_01kftb3024ed7bm84gy6j485fh_1769336848_img_0.webp"

@Composable
fun MusicCommentsSheet(
    track: AudioTrack?,
    currentUser: User,
    comments: List<SongComment>,
    onClose: () -> Unit,
    onAddComment: (String) -> Unit,
    onToggleHide: (Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onProfileClick: (Long) -> Unit
) {
    if (track == null) return

    var inputText by remember { mutableStateOf("") }
    var selectedCommentForActions by remember { mutableStateOf<SongComment?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.scrim)
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .clickable(enabled = false) {}
                    .testTag("music_comments_sheet"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = AppColors.bgScreen,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header (Blue in Light Mode)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.headerBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (AppColors.isLight) Color.White.copy(alpha = 0.55f) else AppColors.borderStrong)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = onClose) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = AppColors.headerIconTint
                                    )
                                }
                                Text(
                                    text = "Discussions",
                                    color = AppColors.headerText,
                                    fontSize = 20.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(onClick = onClose) {
                                Text(
                                    text = "Done",
                                    color = if (AppColors.isLight) Color.White else AppColors.primaryBlue,
                                    fontSize = 16.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Track Preview Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        color = AppColors.bgCard,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = if (track.cover.isNotBlank()) track.cover else DEFAULT_MUSIC_COVER,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = AppColors.textPrimary,
                                    fontSize = 15.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = AppColors.textSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Comments List
                    if (comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No discussions yet",
                                    color = AppColors.textPrimary,
                                    fontSize = 17.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Be the first to start a discussion about this track!",
                                    color = AppColors.textSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(comments, key = { it.id }) { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Author avatar
                                    AsyncImage(
                                        model = comment.userAvatar,
                                        contentDescription = comment.userName,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable { onProfileClick(comment.userId) },
                                        contentScale = ContentScale.Crop
                                    )

                                    // Comment bubble
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = if (comment.isHidden) AppColors.bgCardElevated.copy(alpha = 0.6f) else AppColors.bgCard,
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (comment.isHidden) Color(0xFFF59E0B).copy(alpha = 0.45f) else AppColors.border
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = comment.userName,
                                                            color = AppColors.textPrimary,
                                                            fontSize = 14.sp,
                                                            fontFamily = InterFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { onProfileClick(comment.userId) }
                                                        )
                                                        Text(
                                                            text = "• ${formatTimestamp(comment.createdAt)}",
                                                            color = AppColors.textSecondary,
                                                            fontSize = 11.sp,
                                                            fontFamily = InterFontFamily,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (comment.isHidden) {
                                                            Text(
                                                                text = "Hidden",
                                                                color = Color(0xFFD97706),
                                                                fontSize = 10.sp,
                                                                fontFamily = InterFontFamily,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { selectedCommentForActions = comment },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.MoreHoriz,
                                                                contentDescription = "Options",
                                                                tint = AppColors.textSecondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = comment.text,
                                                    color = AppColors.textBody,
                                                    fontSize = 14.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }

                                        // Actions under comment
                                        Row(
                                            modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Like",
                                                color = AppColors.textSecondary,
                                                fontSize = 14.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable { }
                                            )
                                            Text(
                                                text = "Reply",
                                                color = AppColors.textSecondary,
                                                fontSize = 14.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable {
                                                    inputText = "@${comment.userName} "
                                                }
                                            )
                                            if (comment.likesCount > 0) {
                                                Text(
                                                    text = "${comment.likesCount} likes",
                                                    color = AppColors.textMuted,
                                                    fontSize = 12.sp,
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Comment Input Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.bgSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = LocalTextStyle.current.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                                placeholder = { Text("Write a comment...", color = AppColors.textMuted, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AppColors.bgInput,
                                    unfocusedContainerColor = AppColors.bgInput,
                                    focusedBorderColor = AppColors.primaryBlue,
                                    unfocusedBorderColor = AppColors.border,
                                    focusedTextColor = AppColors.textPrimary,
                                    unfocusedTextColor = AppColors.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_field"),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onAddComment(inputText.trim())
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.primaryBlue,
                                    contentColor = Color.White,
                                    disabledContainerColor = AppColors.bgChip,
                                    disabledContentColor = AppColors.textMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text("Post", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Comment Options Modal Dialog using CommentActionModal
    selectedCommentForActions?.let { c ->
        val isAuthor = c.userId == currentUser.id
        CommentActionModal(
            isOpen = true,
            onClose = { selectedCommentForActions = null },
            songComment = c,
            canHide = true,
            canDelete = isAuthor || currentUser.role in listOf("admin", "moderator"),
            onToggleHide = { commentId ->
                onToggleHide(commentId)
            },
            onDelete = { commentId ->
                onDeleteComment(commentId)
            },
            onReply = { comment ->
                inputText = "@${comment.userName} "
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val min = diff / (60 * 1000L)
    val hrs = diff / (3600 * 1000L)
    val days = diff / (24 * 3600 * 1000L)

    return when {
        min < 1 -> "Just now"
        min < 60 -> "${min}m"
        hrs < 24 -> "${hrs}h"
        days < 7 -> "${days}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
