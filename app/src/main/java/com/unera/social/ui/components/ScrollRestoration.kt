package com.unera.social.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ConcurrentHashMap

/**
 * ScrollRestorationController - Preserves exact scroll positions across routes, tabs, and panels.
 *
 * Behavior:
 * 1. Saves (firstVisibleItemIndex, firstVisibleItemScrollOffset) / ScrollState.value per key so
 *    navigating back to Feed, MarketPoint, Comments, or Notifications restores the exact position.
 * 2. Allows smooth or instant scroll-to-top when tapping the already-active bottom navigation tab.
 * 3. Clears saved positions cleanly on logout / user switch.
 */
class ScrollRestorationController {
    data class ListPosition(
        val index: Int = 0,
        val offset: Int = 0
    )

    private val listStates = mutableMapOf<String, LazyListState>()
    private val gridStates = mutableMapOf<String, LazyGridState>()
    private val scrollStates = mutableMapOf<String, ScrollState>()

    private val savedListPositions = ConcurrentHashMap<String, ListPosition>()
    private val savedGridPositions = ConcurrentHashMap<String, ListPosition>()
    private val savedScrollOffsets = ConcurrentHashMap<String, Int>()

    fun register(key: String, state: LazyListState) {
        listStates[key] = state
    }

    fun register(key: String, state: LazyGridState) {
        gridStates[key] = state
    }

    fun register(key: String, state: ScrollState) {
        scrollStates[key] = state
    }

    fun saveListPosition(key: String, index: Int, offset: Int) {
        savedListPositions[key] = ListPosition(index.coerceAtLeast(0), offset.coerceAtLeast(0))
    }

    fun getSavedListPosition(key: String): ListPosition =
        savedListPositions[key] ?: ListPosition()

    fun saveGridPosition(key: String, index: Int, offset: Int) {
        savedGridPositions[key] = ListPosition(index.coerceAtLeast(0), offset.coerceAtLeast(0))
    }

    fun getSavedGridPosition(key: String): ListPosition =
        savedGridPositions[key] ?: ListPosition()

    fun saveScrollOffset(key: String, value: Int) {
        savedScrollOffsets[key] = value.coerceAtLeast(0)
    }

    fun getSavedScrollOffset(key: String): Int =
        savedScrollOffsets[key] ?: 0

    fun unregister(key: String) {
        listStates.remove(key)?.let {
            saveListPosition(key, it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset)
        }
        gridStates.remove(key)?.let {
            saveGridPosition(key, it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset)
        }
        scrollStates.remove(key)?.let {
            saveScrollOffset(key, it.value)
        }
    }

    suspend fun scrollToTop(key: String, animate: Boolean = true) {
        savedListPositions[key] = ListPosition(0, 0)
        savedGridPositions[key] = ListPosition(0, 0)
        savedScrollOffsets[key] = 0

        listStates[key]?.let {
            if (animate) it.animateScrollToItem(0, 0) else it.scrollToItem(0, 0)
        }
        gridStates[key]?.let {
            if (animate) it.animateScrollToItem(0, 0) else it.scrollToItem(0, 0)
        }
        scrollStates[key]?.let {
            if (animate) it.animateScrollTo(0) else it.scrollTo(0)
        }
    }

    fun clearAll() {
        savedListPositions.clear()
        savedGridPositions.clear()
        savedScrollOffsets.clear()
    }
}

val LocalScrollRestoration = staticCompositionLocalOf {
    ScrollRestorationController()
}

@Composable
fun ProvideScrollRestoration(
    controller: ScrollRestorationController = remember { ScrollRestorationController() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalScrollRestoration provides controller) {
        content()
    }
}

/**
 * Keeps ScrollRestoration composable signature compatible while preserving scroll positions
 * on back navigation instead of wiping feed scroll to (0, 0) on every route transition.
 */
@Composable
fun ScrollRestoration(
    currentRoute: Any,
    controller: ScrollRestorationController = LocalScrollRestoration.current
) {
    // Scroll position is preserved per route key inside ScrollRestorationController.
    // Explicit scroll-to-top happens only when tapping an already-active tab.
}

@Composable
fun rememberRestoredLazyListState(
    key: String,
    controller: ScrollRestorationController = LocalScrollRestoration.current
): LazyListState {
    val saved = remember(key) { controller.getSavedListPosition(key) }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = saved.index,
        initialFirstVisibleItemScrollOffset = saved.offset
    )

    LaunchedEffect(key, state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collectLatest { (idx, off) ->
                controller.saveListPosition(key, idx, off)
            }
    }

    DisposableEffect(key, state) {
        controller.register(key, state)
        onDispose {
            controller.saveListPosition(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            controller.unregister(key)
        }
    }
    return state
}

@Composable
fun rememberRestoredLazyGridState(
    key: String,
    controller: ScrollRestorationController = LocalScrollRestoration.current
): LazyGridState {
    val saved = remember(key) { controller.getSavedGridPosition(key) }
    val state = rememberLazyGridState(
        initialFirstVisibleItemIndex = saved.index,
        initialFirstVisibleItemScrollOffset = saved.offset
    )

    LaunchedEffect(key, state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collectLatest { (idx, off) ->
                controller.saveGridPosition(key, idx, off)
            }
    }

    DisposableEffect(key, state) {
        controller.register(key, state)
        onDispose {
            controller.saveGridPosition(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            controller.unregister(key)
        }
    }
    return state
}

@Composable
fun rememberRestoredScrollState(
    key: String,
    controller: ScrollRestorationController = LocalScrollRestoration.current
): ScrollState {
    val savedOffset = remember(key) { controller.getSavedScrollOffset(key) }
    val state = rememberScrollState(initial = savedOffset)

    LaunchedEffect(key, state) {
        snapshotFlow { state.value }
            .collectLatest { offset ->
                controller.saveScrollOffset(key, offset)
            }
    }

    DisposableEffect(key, state) {
        controller.register(key, state)
        onDispose {
            controller.saveScrollOffset(key, state.value)
            controller.unregister(key)
        }
    }
    return state
}
