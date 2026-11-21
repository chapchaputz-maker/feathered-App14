package com.unera.social.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unera.social.R
import com.unera.social.model.User
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily

@Composable
fun FeatheredTopBar(
    currentUser: User,
    isGuest: Boolean = false,
    unreadNotificationsCount: Int = 0,
    marketplaceBadgeCount: Int = 0,
    isMarketplaceActive: Boolean = false,
    onHomeClick: () -> Unit = {},
    onMarketplaceClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val colors = AppColors
    val headerBrush = Brush.horizontalGradient(
        listOf(colors.headerBg, colors.headerBgEnd)
    )

    Surface(
        color = colors.headerBg,
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBrush)
            .statusBarsPadding()
            .testTag("feathered_top_header")
    ) {
        Column(
            modifier = Modifier.background(headerBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: Brand Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onHomeClick() }
                        .padding(start = 2.dp)
                        .testTag("app_brand_header")
                ) {
                    Text(
                        text = "feathered",
                        fontSize = 27.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.headerText,
                        letterSpacing = (-0.5).sp
                    )
                }

                // RIGHT: MarketPoint, Search, Notifications, Profile Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. MarketPoint Button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMarketplaceActive) colors.headerIconBgActive
                                    else colors.headerIconBg
                                )
                                .then(
                                    if (colors.isLight) Modifier.border(1.dp, colors.headerIconBorder, CircleShape)
                                    else Modifier
                                )
                                .clickable { onMarketplaceClick() }
                                .testTag("top_bar_marketplace_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "MarketPoint",
                                tint = colors.headerIconTint,
                                modifier = Modifier.size(25.5.dp)
                            )
                        }

                        if (marketplaceBadgeCount > 0) {
                            val badgeStr = if (marketplaceBadgeCount > 99) "99+" else marketplaceBadgeCount.toString()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE41E3F))
                                    .border(1.5.dp, colors.headerBadgeBorder, CircleShape)
                                    .padding(horizontal = if (badgeStr.length > 1) 4.5.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeStr,
                                    fontSize = 10.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 2. Search Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.headerIconBg)
                            .then(
                                if (colors.isLight) Modifier.border(1.dp, colors.headerIconBorder, CircleShape)
                                else Modifier
                            )
                            .clickable { onSearchClick() }
                            .testTag("top_bar_search_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_bold),
                            contentDescription = "Search",
                            tint = if (colors.isLight) Color.White else Color.Unspecified,
                            modifier = Modifier.size(25.5.dp)
                        )
                    }

                    // 3. Notification Button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.headerIconBg)
                                .then(
                                    if (colors.isLight) Modifier.border(1.dp, colors.headerIconBorder, CircleShape)
                                    else Modifier
                                )
                                .clickable { onNotificationsClick() }
                                .testTag("top_bar_notifications_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (colors.isLight) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_bell),
                                    contentDescription = "Notifications",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(25.5.dp)
                                )
                            }
                        }

                        if (unreadNotificationsCount > 0) {
                            val badgeStr = when {
                                unreadNotificationsCount > 99 -> "99+"
                                unreadNotificationsCount > 15 -> "15+"
                                unreadNotificationsCount > 9 -> "9+"
                                else -> unreadNotificationsCount.toString()
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE41E3F))
                                    .border(1.5.dp, colors.headerBadgeBorder, CircleShape)
                                    .padding(horizontal = if (badgeStr.length > 1) 4.5.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeStr,
                                    fontSize = 10.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 4. Guest Log In button or User Profile Avatar
                    if (isGuest || currentUser.id <= 0L) {
                        Button(
                            onClick = onLoginClick,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (colors.isLight) Color.White else Color(0xFF1877F2),
                                contentColor = if (colors.isLight) Color(0xFF1877F2) else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("top_bar_login_btn")
                        ) {
                            Text(
                                text = "Log In",
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isLight) Color(0xFF1877F2) else Color.White
                            )
                        }
                    } else {
                        FeatheredAvatar(
                            name = currentUser.name,
                            username = currentUser.username,
                            imageUrl = currentUser.profileImageUrl,
                            size = 40.dp,
                            borderWidth = 2.dp,
                            borderColor = if (colors.isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF334155).copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            onClick = onProfileClick,
                            modifier = Modifier.testTag("top_bar_profile_btn")
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.headerDivider, thickness = 1.dp)
        }
    }
}
