package com.unera.social.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unera.social.model.Screen
import com.unera.social.ui.navigation.NavigationLoadingState
import com.unera.social.ui.navigation.NavigationPhase
import com.unera.social.ui.navigation.isReducedMotionEnabled
import com.unera.social.ui.theme.AppColors
import com.unera.social.ui.theme.InterFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    loadingState: NavigationLoadingState = NavigationLoadingState(),
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

        // Central floating POST button + signature non-blocking navigation loading ring
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
                .size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            PostButtonSignatureLoadingRing(loadingState = loadingState)

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

/**
 * Signature animated circular progress ring around Feathered's central POST button.
 * Purely visual (non-clickable) so POST functionality remains 100% intact at all times.
 */
@Composable
private fun PostButtonSignatureLoadingRing(
    loadingState: NavigationLoadingState
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }

    val ringAlpha by animateFloatAsState(
        targetValue = if (loadingState.isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else if (loadingState.isVisible) 130 else 180,
            easing = FastOutSlowInEasing
        ),
        label = "post_ring_alpha"
    )

    if (ringAlpha <= 0.01f && !loadingState.isVisible) return

    val animatedProgress by animateFloatAsState(
        targetValue = when {
            loadingState.phase == NavigationPhase.FINISHING -> 1f
            loadingState.isVisible -> loadingState.progress.coerceIn(0.14f, 0.94f)
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else if (loadingState.phase == NavigationPhase.FINISHING) 150 else 140,
            easing = FastOutSlowInEasing
        ),
        label = "post_ring_progress"
    )

    val glowIntensity by animateFloatAsState(
        targetValue = if (loadingState.isFinishingGlow || loadingState.phase == NavigationPhase.FINISHING) 1f else 0.45f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 220),
        label = "post_ring_glow"
    )

    val clockwiseRotation = if (!reducedMotion && loadingState.isVisible) {
        val infiniteTransition = rememberInfiniteTransition(label = "post_ring_rotation")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1350, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "post_ring_clockwise"
        )
        rot
    } else {
        0f
    }

    val ringSweep = (animatedProgress * 360f).coerceIn(36f, 360f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (loadingState.isVisible) Modifier.testTag("nav_post_loading_ring")
                else Modifier
            )
    ) {
        val strokePx = 3.2.dp.toPx()
        val glowStrokePx = 7.dp.toPx()
        val ringDiameter = 62.dp.toPx()
        val topLeft = Offset(
            x = (size.width - ringDiameter) / 2f,
            y = (size.height - ringDiameter) / 2f
        )
        val arcSize = Size(ringDiameter, ringDiameter)

        // Subtle ambient track hugging the POST circle
        drawArc(
            color = Color(0xFF3B82F6).copy(alpha = 0.20f * ringAlpha),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx)
        )

        val startAngle = -90f + clockwiseRotation

        rotate(degrees = 0f, pivot = center) {
            // Soft outer blue glow halo
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF1877F2).copy(alpha = 0.22f * glowIntensity * ringAlpha),
                        Color(0xFF38BDF8).copy(alpha = 0.48f * glowIntensity * ringAlpha),
                        Color(0xFF93C5FD).copy(alpha = 0.56f * glowIntensity * ringAlpha),
                        Color(0xFF1877F2).copy(alpha = 0.22f * glowIntensity * ringAlpha)
                    ),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = ringSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
            )

            // Crisp primary clockwise blue gradient arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF166FE5).copy(alpha = 0.85f * ringAlpha),
                        Color(0xFF1877F2).copy(alpha = ringAlpha),
                        Color(0xFF38BDF8).copy(alpha = ringAlpha),
                        Color(0xFFBFDBFE).copy(alpha = ringAlpha),
                        Color(0xFF166FE5).copy(alpha = 0.85f * ringAlpha)
                    ),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = ringSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Refined specular tip highlight on the leading edge while loading
            if (ringSweep < 355f) {
                val tipAngleRad = ((startAngle + ringSweep) * PI / 180.0)
                val radius = ringDiameter / 2f
                val tipCenter = Offset(
                    x = center.x + (radius * cos(tipAngleRad)).toFloat(),
                    y = center.y + (radius * sin(tipAngleRad)).toFloat()
                )
                drawCircle(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.92f * ringAlpha),
                    radius = 2.4.dp.toPx(),
                    center = tipCenter
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
