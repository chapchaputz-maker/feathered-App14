package com.unera.social.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe session tracker to prevent duplicate view increments
 * for the same post during the user's active session.
 */
object PostViewTracker {

    private val viewedPostIds: MutableSet<Long> = Collections.newSetFromMap(ConcurrentHashMap())
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Records a view for a given post if not already recorded in this session.
     * Excludes self-views (author viewing their own post).
     */
    fun trackPostView(postId: Long, authorId: Long, viewerId: Long, token: String? = null) {
        if (postId <= 0L || viewerId <= 0L) return
        // Do not record author's own views
        if (authorId > 0L && authorId == viewerId) return

        // Deduplicate per app session
        if (viewedPostIds.add(postId)) {
            scope.launch {
                try {
                    PostsApiService.recordPostView(postId, viewerId, token)
                } catch (_: Exception) {
                    // Silently fail, network view tracking is best-effort
                }
            }
        }
    }

    /**
     * Check if a post was already viewed in this session
     */
    fun isViewed(postId: Long): Boolean = viewedPostIds.contains(postId)

    /**
     * Clear session tracking (e.g. on logout)
     */
    fun clear() {
        viewedPostIds.clear()
    }
}
