package com.unera.social.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.User
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily

data class WebShortcutItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

val ALL_SHORTCUT_ITEMS = listOf(
    WebShortcutItem("marketplace", "MarketPoint", Icons.Default.Storefront, Color(0xFF1877F2)),
    WebShortcutItem("saved-posts", "Saved Posts", Icons.Default.Bookmark, Color(0xFFF59E0B)),
    WebShortcutItem("events", "Events", Icons.Default.CalendarMonth, Color(0xFF2563EB)),
    WebShortcutItem("profiles", "Profiles", Icons.Default.People, Color(0xFF1877F2)),
    WebShortcutItem("groups", "Groups", Icons.Default.Groups, Color(0xFF0EA5E9)),
    WebShortcutItem("music", "F-Music", Icons.Default.MusicNote, Color(0xFF1877F2)),
    WebShortcutItem("tools", "Feathered Tools", Icons.Default.Work, Color(0xFF14B8A6)),
    WebShortcutItem("reels", "Videos", Icons.Default.PlayCircle, Color(0xFF1877F2)),
    WebShortcutItem("birthdays", "Birthdays", Icons.Default.Cake, Color(0xFFF59E0B)),
    WebShortcutItem("memories", "Memories", Icons.Default.History, Color(0xFF6366F1)),
    WebShortcutItem("story-feed", "Story Feed", Icons.Default.Layers, Color(0xFF10B981)),
    WebShortcutItem("notifications", "Notifications", Icons.Default.Notifications, Color(0xFF1877F2)),
    WebShortcutItem("ads", "Ad Manager", Icons.Default.TrendingUp, Color(0xFF10B981))
)

data class MenuBottomOption(
    val id: String,
    val title: String,
    val icon: ImageVector
)

val MENU_BOTTOM_OPTIONS = listOf(
    MenuBottomOption("settings", "Settings & Privacy", Icons.Default.Settings),
    MenuBottomOption("privacy", "Privacy Policy", Icons.Default.Security),
    MenuBottomOption("help", "Help & Support", Icons.Default.HelpOutline),
    MenuBottomOption("terms", "Terms of Service", Icons.Default.Description)
)

@Composable
fun MenuOverlay(
    currentUser: User,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val colors = AppColors
    val cardColor = if (colors.isLight) colors.bgCard else Color(0xFF162137).copy(alpha = 0.65f)
    val cardBorder = if (colors.isLight) colors.border else Color(0xFF1E293B).copy(alpha = 0.60f)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (colors.isLight) colors.bgScreen else Color(0xFF0B1120))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("menu_overlay")
        ) {
            // Header: Blue in Light Mode, Navy in Dark Mode
            Surface(
                color = colors.headerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(colors.headerBg, colors.headerBgEnd)))
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (colors.isLight) {
                                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.16f)))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF166FE5), Color(0xFF1877F2)))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Menu",
                            fontSize = 20.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.headerText
                        )
                    }

                    Surface(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        color = if (colors.isLight) colors.headerIconBg else Color(0xFF162137).copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            if (colors.isLight) colors.headerIconBorder else Color(0xFF1E293B).copy(alpha = 0.60f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close menu",
                                tint = if (colors.isLight) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.headerDivider, thickness = 1.dp)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        onClick = {
                            onNavigate("profile")
                            onClose()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (colors.isLight) 1.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("menu_profile_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentUser.profileImageUrl,
                                contentDescription = currentUser.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.name,
                                    fontSize = 19.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "View your profile →",
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.primaryBlue
                                )
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "ALL SHORTCUTS",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                    )
                }

                items(ALL_SHORTCUT_ITEMS, key = { it.id }) { item ->
                    Surface(
                        onClick = {
                            onNavigate(item.id)
                            onClose()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (colors.isLight) 1.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_shortcut_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (colors.isLight) item.color.copy(alpha = 0.12f)
                                        else Color(0xFF1E293B).copy(alpha = 0.60f)
                                    )
                                    .border(
                                        1.dp,
                                        if (colors.isLight) item.color.copy(alpha = 0.22f)
                                        else Color(0xFF334155).copy(alpha = 0.40f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = item.title,
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    HorizontalDivider(
                        color = colors.border,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MENU_BOTTOM_OPTIONS.forEach { item ->
                            Surface(
                                onClick = {
                                    onNavigate(item.id)
                                    onClose()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (colors.isLight) colors.bgCard else Color(0xFF162137).copy(alpha = 0.50f),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = item.title,
                                            fontSize = 18.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            onClick = {
                                onLogout()
                                onClose()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (colors.isLight) colors.bgCard else Color(0xFF162137).copy(alpha = 0.50f),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("menu_logout_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Log Out",
                                    tint = colors.dangerText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Log Out",
                                    fontSize = 18.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.dangerText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
