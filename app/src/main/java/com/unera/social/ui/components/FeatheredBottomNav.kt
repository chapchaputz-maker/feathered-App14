package com.unera.social.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unera.social.model.Screen
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily

private fun formatBadge(count: Int): String {
    if (count <= 0) return ""
    return if (count > 15) "15+" else count.toString()
}

@Composable
fun FeatheredBottomNav(
    currentScreen: Screen,
    unreadMessagesCount: Int = 0,
    marketplaceBadgeCount: Int = 0,
    homeBadgeCount: Int = 0,
    onTabSelected: (Screen) -> Unit,
    onPostClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val colors = AppColors
    val isHomeActive = currentScreen == Screen.FEED
    val isMarketActive = currentScreen == Screen.MARKETPLACE
    val isMessageActive = currentScreen == Screen.MESSAGES

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("feathered_bottom_navigation"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = colors.navBg,
            border = BorderStroke(1.dp, colors.navBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (colors.isLight) 10.dp else 16.dp,
                    ambientColor = if (colors.isLight) Color.Black.copy(alpha = 0.14f) else Color.Black,
                    spotColor = if (colors.isLight) Color.Black.copy(alpha = 0.18f) else Color.Black
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    label = "Home",
                    icon = Icons.Default.GridView,
                    isActive = isHomeActive,
                    badgeCount = homeBadgeCount,
                    testTag = "nav_tab_home",
                    onClick = { onTabSelected(Screen.FEED) },
                    modifier = Modifier.weight(1f)
                )

                NavTabItem(
                    label = "MarketPoint",
                    icon = Icons.Default.Storefront,
                    isActive = isMarketActive,
                    badgeCount = marketplaceBadgeCount,
                    testTag = "nav_tab_marketplace",
                    onClick = { onTabSelected(Screen.MARKETPLACE) },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onPostClick() }
                        .testTag("nav_tab_post"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "POST",
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primaryBlue,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                NavTabItem(
                    label = "Message",
                    icon = Icons.Default.ChatBubble,
                    isActive = isMessageActive,
                    badgeCount = unreadMessagesCount,
                    testTag = "nav_tab_messages",
                    onClick = { onTabSelected(Screen.MESSAGES) },
                    modifier = Modifier.weight(1f)
                )

                NavTabItem(
                    label = "Menu",
                    icon = Icons.Default.Menu,
                    isActive = false,
                    badgeCount = 0,
                    testTag = "nav_tab_menu",
                    onClick = onMenuClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF1877F2),
                        spotColor = Color(0xFF1877F2)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF166FE5), Color(0xFF1877F2), Color(0xFF3B82F6))
                        )
                    )
                    .border(3.5.dp, colors.navPostBorder, CircleShape)
                    .clickable { onPostClick() }
                    .testTag("nav_post_floating_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Post",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppColors
    val itemColor = if (isActive) colors.navActive else colors.navInactive

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier.size(23.dp)
            )

            if (badgeCount > 0) {
                val badgeText = formatBadge(badgeCount)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-6).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE41E3F))
                        .padding(horizontal = 4.5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontFamily = InterFontFamily,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = itemColor
        )

        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(colors.navActive)
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}
