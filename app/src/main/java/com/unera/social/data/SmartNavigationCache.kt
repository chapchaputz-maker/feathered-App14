package com.unera.social.data

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.unera.social.model.Notification
import com.unera.social.model.Post
import com.unera.social.model.Product
import com.unera.social.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized, TTL-aware, privacy-respecting navigation & destination cache.
 *
 * Responsibilities:
 * - Instant second-visit opens for User Profiles, Posts, MarketPoint listings, Notifications,
 *   and previously visited destinations while quietly refreshing stale data in the background.
 * - Lightweight, deduplicated prefetching of profile info, post comments, and small thumbnails
 *   without wasting mobile data on heavy media.
 * - Strict privacy & deletion hygiene: clears user-scoped caches on logout/revoked access and
 *   evicts deleted/hidden posts or products immediately.
 */
object SmartNavigationCache {

    private const val PROFILE_TTL_MS = 3 * 60_000L
    private const val POST_TTL_MS = 2 * 60_000L
    private const val PRODUCT_TTL_MS = 3 * 60_000L
    private const val NOTIFICATIONS_TTL_MS = 60_000L
    private const val SCREEN_TTL_MS = 2 * 60_000L

    data class CacheEntry<T>(
        val data: T,
        val cachedAtMs: Long,
        val ttlMs: Long
    ) {
        fun isFresh(nowMs: Long = System.currentTimeMillis()): Boolean =
            (nowMs - cachedAtMs) <= ttlMs
    }

    private val profileCache = ConcurrentHashMap<Long, CacheEntry<User>>()
    private val profilePostsCache = ConcurrentHashMap<Long, CacheEntry<List<Post>>>()
    private val postCache = ConcurrentHashMap<Long, CacheEntry<Post>>()
    private val productCache = ConcurrentHashMap<Long, CacheEntry<Product>>()
    private val notificationsCache = ConcurrentHashMap<Long, CacheEntry<List<Notification>>>()
    private val screenFreshness = ConcurrentHashMap<String, Long>()
    private val visitedDestinations = ConcurrentHashMap.newKeySet<String>()
    private val inFlightPrefetches = ConcurrentHashMap<String, Boolean>()

    // ------------------------------------------------------
    // Destination Visited Tracking
    // ------------------------------------------------------
    fun markVisited(destinationKey: String) {
        visitedDestinations.add(destinationKey)
    }

    fun hasVisited(destinationKey: String): Boolean =
        visitedDestinations.contains(destinationKey)

    // ------------------------------------------------------
    // User Profile Caching
    // ------------------------------------------------------
    fun cacheProfile(user: User, nowMs: Long = System.currentTimeMillis()) {
        if (user.id <= 0L) return
        profileCache[user.id] = CacheEntry(user, nowMs, PROFILE_TTL_MS)
        markVisited("profile:${user.id}")
    }

    fun getProfile(userId: Long): CacheEntry<User>? = profileCache[userId]

    fun cacheProfilePosts(userId: Long, posts: List<Post>, nowMs: Long = System.currentTimeMillis()) {
        if (userId <= 0L) return
        profilePostsCache[userId] = CacheEntry(posts, nowMs, PROFILE_TTL_MS)
        posts.forEach { cachePost(it, nowMs) }
    }

    fun getProfilePosts(userId: Long): CacheEntry<List<Post>>? = profilePostsCache[userId]

    fun invalidateProfile(userId: Long) {
        profileCache.remove(userId)
        profilePostsCache.remove(userId)
        visitedDestinations.remove("profile:$userId")
    }

    // ------------------------------------------------------
    // Post Caching
    // ------------------------------------------------------
    fun cachePost(post: Post, nowMs: Long = System.currentTimeMillis()) {
        if (post.id <= 0L) return
        postCache[post.id] = CacheEntry(post, nowMs, POST_TTL_MS)
        if (post.comments.isNotEmpty()) {
            val itemType = CommentsCache.resolveItemType(post)
            CommentsCache.setCachedComments(itemType, post.id, post.comments)
        }
    }

    fun getPost(postId: Long): CacheEntry<Post>? = postCache[postId]

    fun invalidatePost(postId: Long) {
        postCache.remove(postId)
        visitedDestinations.remove("post:$postId")
        visitedDestinations.remove("comments:$postId")
        CommentsCache.invalidatePost(postId)
        profilePostsCache.entries.forEach { entry ->
            val filtered = entry.value.data.filterNot { it.id == postId }
            if (filtered.size != entry.value.data.size) {
                profilePostsCache[entry.key] = entry.value.copy(data = filtered)
            }
        }
    }

    // ------------------------------------------------------
    // MarketPoint Product Caching
    // ------------------------------------------------------
    fun cacheProduct(product: Product, nowMs: Long = System.currentTimeMillis()) {
        if (product.id <= 0L) return
        productCache[product.id] = CacheEntry(product, nowMs, PRODUCT_TTL_MS)
    }

    fun cacheProducts(products: List<Product>, nowMs: Long = System.currentTimeMillis()) {
        products.forEach { cacheProduct(it, nowMs) }
        markScreenRefreshed("MARKETPLACE", nowMs)
    }

    fun getProduct(productId: Long): CacheEntry<Product>? = productCache[productId]

    fun invalidateProduct(productId: Long) {
        productCache.remove(productId)
        visitedDestinations.remove("product:$productId")
    }

    // ------------------------------------------------------
    // Notifications Caching (Scoped per viewerUserId)
    // ------------------------------------------------------
    fun cacheNotifications(
        viewerUserId: Long,
        list: List<Notification>,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (viewerUserId <= 0L) return
        notificationsCache[viewerUserId] = CacheEntry(list, nowMs, NOTIFICATIONS_TTL_MS)
    }

    fun getNotifications(viewerUserId: Long): CacheEntry<List<Notification>>? {
        if (viewerUserId <= 0L) return null
        return notificationsCache[viewerUserId]
    }

    // ------------------------------------------------------
    // Screen Freshness Tracking (Feed, MarketPoint, Events, Groups, Music, Reels)
    // ------------------------------------------------------
    fun markScreenRefreshed(screenKey: String, nowMs: Long = System.currentTimeMillis()) {
        screenFreshness[screenKey] = nowMs
        markVisited("screen:$screenKey")
    }

    fun isScreenFresh(
        screenKey: String,
        ttlMs: Long = SCREEN_TTL_MS,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val last = screenFreshness[screenKey] ?: return false
        return (nowMs - last) <= ttlMs
    }

    // ------------------------------------------------------
    // Privacy / Logout Cleanup
    // ------------------------------------------------------
    fun clearUserScopedCaches() {
        notificationsCache.clear()
        profilePostsCache.clear()
        profileCache.clear()
        postCache.clear()
        screenFreshness.clear()
        visitedDestinations.clear()
        inFlightPrefetches.clear()
        CommentsCache.clearAll()
    }

    // ------------------------------------------------------
    // Lightweight Prefetching (Thumbnails & Metadata Only)
    // ------------------------------------------------------
    fun prefetchThumbnail(context: Context, imageUrl: String?, maxSizePx: Int = 220) {
        val clean = imageUrl?.trim().orEmpty()
        if (clean.isBlank() || clean.equals("null", ignoreCase = true)) return
        val key = "img:$clean:$maxSizePx"
        if (inFlightPrefetches.putIfAbsent(key, true) != null) return
        try {
            val request = ImageRequest.Builder(context.applicationContext)
                .data(clean)
                .size(Size(maxSizePx, maxSizePx))
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .allowHardware(false)
                .listener(
                    onSuccess = { _, _ -> inFlightPrefetches.remove(key) },
                    onError = { _, _ -> inFlightPrefetches.remove(key) },
                    onCancel = { inFlightPrefetches.remove(key) }
                )
                .build()
            context.applicationContext.imageLoader.enqueue(request)
        } catch (_: Exception) {
            inFlightPrefetches.remove(key)
        }
    }

    fun prefetchProfile(
        context: Context,
        scope: CoroutineScope,
        userId: Long,
        knownUser: User? = null
    ) {
        if (userId <= 0L) return
        knownUser?.let {
            if (it.profileImageUrl.isNotBlank()) {
                prefetchThumbnail(context, it.profileImageUrl, 160)
            }
        }
        val cachedPosts = profilePostsCache[userId]
        val cachedUser = profileCache[userId]
        if (cachedPosts != null && cachedPosts.isFresh() && cachedUser != null && cachedUser.isFresh()) {
            return
        }
        val key = "prefetch_profile:$userId"
        if (inFlightPrefetches.putIfAbsent(key, true) != null) return
        scope.launch(Dispatchers.IO) {
            try {
                if (cachedUser == null || !cachedUser.isFresh()) {
                    val user = knownUser ?: UserApiService.fetchUserById(userId)
                    if (user != null) {
                        cacheProfile(user)
                        prefetchThumbnail(context, user.profileImageUrl, 160)
                    }
                }
                if (cachedPosts == null || !cachedPosts.isFresh()) {
                    val posts = PostsApiService.fetchUserPosts(userId)
                    if (posts.isNotEmpty()) {
                        cacheProfilePosts(userId, posts)
                    }
                }
            } catch (_: Exception) {
            } finally {
                inFlightPrefetches.remove(key)
            }
        }
    }

    fun prefetchPost(
        context: Context,
        scope: CoroutineScope,
        post: Post,
        viewerId: Long
    ) {
        if (post.id <= 0L) return
        cachePost(post)
        if (post.author.profileImageUrl.isNotBlank()) {
            prefetchThumbnail(context, post.author.profileImageUrl, 128)
        }
        if (post.commentsCount > 0) {
            val itemType = CommentsCache.resolveItemType(post)
            if (CommentsCache.getCachedComments(itemType, post.id) == null) {
                val key = "prefetch_comments:${itemType}:${post.id}"
                if (inFlightPrefetches.putIfAbsent(key, true) == null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            CommentsCache.ensurePreviewComment(post, viewerId)
                        } catch (_: Exception) {
                        } finally {
                            inFlightPrefetches.remove(key)
                        }
                    }
                }
            }
        }
    }

    fun prefetchProduct(context: Context, product: Product) {
        if (product.id <= 0L) return
        cacheProduct(product)
        val thumb = product.images.firstOrNull().orEmpty().ifBlank { product.imageUrl }
        prefetchThumbnail(context, thumb, 360)
    }
}
