package com.unera.social.ui.navigation

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong

enum class DestinationType {
    FEED,
    POST,
    PROFILE,
    COMMENTS,
    MARKETPLACE_LIST,
    MARKETPLACE_ITEM,
    MESSAGES,
    NOTIFICATIONS,
    GROUPS,
    EVENTS,
    MUSIC,
    REELS,
    OTHER
}

enum class NavigationPhase {
    IDLE,
    LOADING,
    FINISHING
}

data class NavigationLoadingState(
    val requestId: Long = 0L,
    val phase: NavigationPhase = NavigationPhase.IDLE,
    val destinationType: DestinationType = DestinationType.OTHER,
    val progress: Float = 0f,
    val isVisible: Boolean = false,
    val isFinishingGlow: Boolean = false
) {
    val isActive: Boolean
        get() = phase != NavigationPhase.IDLE
}

/**
 * Checks Android accessibility "Remove animations" / Animator duration scale.
 */
fun isReducedMotionEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale <= 0f
    } catch (_: Exception) {
        false
    }
}

/**
 * Global single-flight navigation & signature loading state controller.
 *
 * Guarantees:
 * 1. Only one navigation loading sequence is active at any time.
 * 2. Rapid taps cancel superseded navigation cleanly via monotonic request IDs (no race conditions).
 * 3. Instant/cached second visits commit immediately with zero loader flicker.
 * 4. Perceptible-delay threshold (~110ms) prevents flashing a ring on ultra-fast responses.
 * 5. Progress never falsely hits 100% before destination data is genuinely ready.
 * 6. Timeouts, failures, and cancellations always reset the ring cleanly (never stuck).
 */
class SmartNavigationController(
    private val scope: CoroutineScope,
    private val reducedMotionProvider: () -> Boolean = { false },
    private val perceptibleDelayMs: Long = 110L,
    private val finishingHoldMs: Long = 180L,
    private val requestTimeoutMs: Long = 15_000L
) {
    private val requestSeq = AtomicLong(0L)
    private var activeJob: Job? = null

    private val _state = MutableStateFlow(NavigationLoadingState())
    val state: StateFlow<NavigationLoadingState> = _state.asStateFlow()

    private val _lastTransitionType = MutableStateFlow(DestinationType.FEED)
    val lastTransitionType: StateFlow<DestinationType> = _lastTransitionType.asStateFlow()

    fun <T> navigate(
        destinationType: DestinationType,
        hasCachedData: Boolean,
        showBriefLoaderWhenCached: Boolean = false,
        onCachedCommit: (() -> Unit)? = null,
        onBackgroundRefresh: (suspend () -> Unit)? = null,
        onFetchData: suspend (reportProgress: (Float) -> Unit) -> T?,
        onCommit: (T) -> Unit,
        onUnavailableOrError: (() -> Unit)? = null
    ): Long {
        activeJob?.cancel()
        val requestId = requestSeq.incrementAndGet()
        _lastTransitionType.value = destinationType

        if (hasCachedData && onCachedCommit != null) {
            if (showBriefLoaderWhenCached && !reducedMotionProvider()) {
                _state.value = NavigationLoadingState(
                    requestId = requestId,
                    phase = NavigationPhase.LOADING,
                    destinationType = destinationType,
                    progress = 0.24f,
                    isVisible = true,
                    isFinishingGlow = false
                )
                activeJob = scope.launch {
                    try {
                        delay(70L)
                        if (!isCurrent(requestId)) return@launch
                        _state.value = _state.value.copy(progress = 0.62f, isVisible = true)
                        delay(70L)
                        if (!isCurrent(requestId)) return@launch
                        _state.value = _state.value.copy(
                            phase = NavigationPhase.FINISHING,
                            progress = 1.0f,
                            isVisible = true,
                            isFinishingGlow = true
                        )
                        delay(75L)
                        if (!isCurrent(requestId)) return@launch
                        onCachedCommit()
                    } catch (ce: CancellationException) {
                        throw ce
                    } finally {
                        if (isCurrent(requestId)) {
                            _state.value = NavigationLoadingState(
                                requestId = requestId,
                                phase = NavigationPhase.IDLE,
                                destinationType = destinationType,
                                progress = 0f,
                                isVisible = false,
                                isFinishingGlow = false
                            )
                        }
                    }

                    if (onBackgroundRefresh != null) {
                        try {
                            onBackgroundRefresh()
                        } catch (_: CancellationException) {
                        } catch (_: Exception) {
                        }
                    }
                }
                return requestId
            }

            _state.value = NavigationLoadingState(
                requestId = requestId,
                phase = NavigationPhase.IDLE,
                destinationType = destinationType,
                progress = 0f,
                isVisible = false,
                isFinishingGlow = false
            )
            onCachedCommit()
            if (onBackgroundRefresh != null) {
                activeJob = scope.launch {
                    try {
                        onBackgroundRefresh()
                    } catch (_: CancellationException) {
                        // Normal cancellation if user navigated elsewhere
                    } catch (_: Exception) {
                        // Silent background refresh must never disrupt active view
                    }
                }
            }
            return requestId
        }

        _state.value = NavigationLoadingState(
            requestId = requestId,
            phase = NavigationPhase.LOADING,
            destinationType = destinationType,
            progress = 0.08f,
            isVisible = false,
            isFinishingGlow = false
        )

        activeJob = scope.launch {
            val reducedMotion = reducedMotionProvider()
            var revealJob: Job? = null
            var tickerJob: Job? = null

            try {
                // Only show the ring if loading takes longer than perceptibleDelayMs
                revealJob = launch {
                    if (perceptibleDelayMs > 0L) delay(perceptibleDelayMs)
                    if (isCurrent(requestId) && _state.value.phase == NavigationPhase.LOADING) {
                        _state.value = _state.value.copy(isVisible = true)
                    }
                }

                // Smooth asymptotic progress while awaiting real network completion (caps at 0.88f)
                tickerJob = launch {
                    while (isCurrent(requestId) && _state.value.phase == NavigationPhase.LOADING) {
                        delay(70L)
                        val cur = _state.value
                        if (!isCurrent(requestId) || cur.phase != NavigationPhase.LOADING) break
                        val ceiling = 0.88f
                        val next = (cur.progress + (ceiling - cur.progress) * 0.14f).coerceAtMost(ceiling)
                        _state.value = cur.copy(
                            progress = next,
                            isFinishingGlow = next >= 0.72f
                        )
                    }
                }

                val result = withTimeout(requestTimeoutMs) {
                    onFetchData { milestone ->
                        if (isCurrent(requestId) && _state.value.phase == NavigationPhase.LOADING) {
                            val clamped = milestone.coerceIn(0.10f, 0.92f)
                            val cur = _state.value
                            if (clamped > cur.progress) {
                                _state.value = cur.copy(
                                    progress = clamped,
                                    isFinishingGlow = clamped >= 0.72f
                                )
                            }
                        }
                    }
                }

                revealJob.cancel()
                tickerJob.cancel()

                if (!isCurrent(requestId)) return@launch

                if (result != null) {
                    val wasVisible = _state.value.isVisible
                    if (wasVisible && !reducedMotion && finishingHoldMs > 0L) {
                        _state.value = _state.value.copy(
                            phase = NavigationPhase.FINISHING,
                            progress = 1.0f,
                            isVisible = true,
                            isFinishingGlow = true
                        )
                        delay(finishingHoldMs)
                        if (!isCurrent(requestId)) return@launch
                    }
                    onCommit(result)
                } else {
                    onUnavailableOrError?.invoke()
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                if (isCurrent(requestId)) {
                    onUnavailableOrError?.invoke()
                }
            } finally {
                revealJob?.cancel()
                tickerJob?.cancel()
                if (isCurrent(requestId)) {
                    _state.value = NavigationLoadingState(
                        requestId = requestId,
                        phase = NavigationPhase.IDLE,
                        destinationType = destinationType,
                        progress = 0f,
                        isVisible = false,
                        isFinishingGlow = false
                    )
                }
            }
        }
        return requestId
    }

    /**
     * Triggers the signature circular progress ring for interactive transitions such as
     * opening Share, Full Post Image Preview, "What's on your mind?" composer, or modal viewers.
     */
    fun launchInteractiveTransition(
        destinationType: DestinationType,
        durationMs: Long = 215L,
        onReady: () -> Unit
    ): Long {
        activeJob?.cancel()
        val requestId = requestSeq.incrementAndGet()
        _lastTransitionType.value = destinationType

        if (reducedMotionProvider() || durationMs <= 0L) {
            _state.value = NavigationLoadingState(
                requestId = requestId,
                phase = NavigationPhase.IDLE,
                destinationType = destinationType,
                progress = 0f,
                isVisible = false,
                isFinishingGlow = false
            )
            onReady()
            return requestId
        }

        _state.value = NavigationLoadingState(
            requestId = requestId,
            phase = NavigationPhase.LOADING,
            destinationType = destinationType,
            progress = 0.24f,
            isVisible = true,
            isFinishingGlow = false
        )

        activeJob = scope.launch {
            try {
                val stepMs = (durationMs / 3L).coerceAtLeast(35L)
                delay(stepMs)
                if (!isCurrent(requestId)) return@launch
                _state.value = _state.value.copy(progress = 0.62f, isVisible = true)

                delay(stepMs)
                if (!isCurrent(requestId)) return@launch
                _state.value = _state.value.copy(
                    phase = NavigationPhase.FINISHING,
                    progress = 1.0f,
                    isVisible = true,
                    isFinishingGlow = true
                )

                delay(stepMs)
                if (!isCurrent(requestId)) return@launch
                onReady()
            } catch (ce: CancellationException) {
                throw ce
            } finally {
                if (isCurrent(requestId)) {
                    _state.value = NavigationLoadingState(
                        requestId = requestId,
                        phase = NavigationPhase.IDLE,
                        destinationType = destinationType,
                        progress = 0f,
                        isVisible = false,
                        isFinishingGlow = false
                    )
                }
            }
        }
        return requestId
    }

    fun recordTransition(destinationType: DestinationType) {
        _lastTransitionType.value = destinationType
    }

    fun cancelActive() {
        activeJob?.cancel()
        activeJob = null
        val nextId = requestSeq.incrementAndGet()
        _state.value = NavigationLoadingState(
            requestId = nextId,
            phase = NavigationPhase.IDLE,
            destinationType = _state.value.destinationType,
            progress = 0f,
            isVisible = false,
            isFinishingGlow = false
        )
    }

    private fun isCurrent(requestId: Long): Boolean = requestSeq.get() == requestId
}

val LocalSmartNavigationController = staticCompositionLocalOf<SmartNavigationController?> { null }
