package com.unera.social.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.unera.social.model.Screen

/**
 * Maps a Feathered Screen route to its semantic DestinationType for coordinated transitions.
 */
fun Screen.toDestinationType(): DestinationType = when (this) {
    Screen.FEED -> DestinationType.FEED
    Screen.PROFILE -> DestinationType.PROFILE
    Screen.MARKETPLACE -> DestinationType.MARKETPLACE_LIST
    Screen.MESSAGES -> DestinationType.MESSAGES
    Screen.GROUPS -> DestinationType.GROUPS
    Screen.EVENTS -> DestinationType.EVENTS
    Screen.MUSIC -> DestinationType.MUSIC
    Screen.REELS -> DestinationType.REELS
    else -> DestinationType.OTHER
}

/**
 * Coordinated ContentTransform per destination type for main-scaffold AnimatedContent.
 * Preserves dark navy / light theme continuity without sudden blank screens or bouncing.
 */
fun destinationContentTransform(
    targetScreen: Screen,
    initialScreen: Screen,
    isReturningBack: Boolean,
    reducedMotion: Boolean
): ContentTransform {
    if (reducedMotion) {
        return fadeIn(tween(80)) togetherWith fadeOut(tween(60))
    }

    if (isReturningBack) {
        return when (initialScreen) {
            Screen.MESSAGES -> {
                (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 10 })
                    .togetherWith(
                        fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 6 }
                    )
            }
            Screen.PROFILE -> {
                (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.985f, animationSpec = tween(200, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(tween(170, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.95f, animationSpec = tween(170, easing = FastOutSlowInEasing))
                    )
            }
            else -> {
                fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(150, easing = FastOutSlowInEasing))
            }
        }
    }

    return when (targetScreen) {
        Screen.PROFILE -> {
            (fadeIn(tween(230, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(230, easing = FastOutSlowInEasing)) { it / 24 })
                .togetherWith(
                    fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.98f, animationSpec = tween(160))
                )
        }
        Screen.MESSAGES -> {
            (fadeIn(tween(210, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 6 })
                .togetherWith(
                    fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 12 }
                )
        }
        Screen.MARKETPLACE -> {
            (fadeIn(tween(210, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                .togetherWith(
                    fadeOut(tween(150, easing = FastOutSlowInEasing))
                )
        }
        Screen.FEED -> {
            fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(140, easing = FastOutSlowInEasing))
        }
        else -> {
            (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.97f, animationSpec = tween(200, easing = FastOutSlowInEasing)))
                .togetherWith(
                    fadeOut(tween(140, easing = FastOutSlowInEasing))
                )
        }
    }
}

/**
 * Coordinated entrance reveal wrapper for modal/panel destinations
 * (Post expansion, Comments panel slide-up, MarketPoint detail expansion).
 */
@Composable
fun CoordinatedDestinationReveal(
    destinationType: DestinationType,
    key: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val anim = remember(key) { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(key, reducedMotion) {
        if (reducedMotion) {
            anim.snapTo(1f)
        } else {
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = when (destinationType) {
                        DestinationType.POST, DestinationType.MARKETPLACE_ITEM -> 230
                        DestinationType.COMMENTS -> 240
                        DestinationType.MESSAGES -> 210
                        else -> 200
                    },
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val progress = anim.value
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = (0.35f + 0.65f * progress).coerceIn(0f, 1f)
                when (destinationType) {
                    DestinationType.POST, DestinationType.MARKETPLACE_ITEM -> {
                        val s = 0.93f + 0.07f * progress
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin.Center
                        translationY = (1f - progress) * 24f
                    }
                    DestinationType.COMMENTS -> {
                        val s = 0.97f + 0.03f * progress
                        scaleX = s
                        scaleY = s
                        translationY = (1f - progress) * 84f
                    }
                    DestinationType.MESSAGES -> {
                        translationX = (1f - progress) * 64f
                    }
                    else -> {
                        val s = 0.97f + 0.03f * progress
                        scaleX = s
                        scaleY = s
                    }
                }
            }
    ) {
        content()
    }
}
