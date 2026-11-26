package com.unera.social

import com.unera.social.data.CommentsCache
import com.unera.social.data.SmartNavigationCache
import com.unera.social.model.Comment
import com.unera.social.model.Notification
import com.unera.social.model.NotificationType
import com.unera.social.model.Post
import com.unera.social.model.Product
import com.unera.social.model.User
import com.unera.social.ui.components.ScrollRestorationController
import com.unera.social.ui.navigation.DestinationType
import com.unera.social.ui.navigation.NavigationLoadingState
import com.unera.social.ui.navigation.NavigationPhase
import com.unera.social.ui.navigation.SmartNavigationController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive verification suite covering all 15 Smart Navigation & Signature Loading scenarios:
 * 1. First-time user profile opening
 * 2. Second-time cached user profile opening (no loader)
 * 3. First-time post opening
 * 4. Reopening previously visited post (cached instant open)
 * 5. Opening comments (cached vs uncached + invalidation)
 * 6. Opening MarketPoint listing (cached vs fetch by ID)
 * 7. Opening conversation (cached instant vs uncached fetch)
 * 8. Opening notifications (cached per-user + background refresh)
 * 9. Rapidly tapping multiple destinations (single-flight cancellation)
 * 10. Returning to Home via Back & exact scroll restoration
 * 11. Failed network request resets loader cleanly
 * 12. Slow network progressive ring never hits 100% prematurely
 * 13. Ultra-fast response (< perceptible delay) avoids loader flash
 * 14. Offline access to cached content
 * 15. Privacy cleanup on logout, deletion cache eviction, and non-blocking state guarantees
 */
class SmartNavigationTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        SmartNavigationCache.clearUserScopedCaches()
        CommentsCache.clearAll()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        SmartNavigationCache.clearUserScopedCaches()
        CommentsCache.clearAll()
        scope.cancel()
    }

    private fun sampleUser(id: Long = 101L) = User(
        id = id,
        name = "Amina Juma",
        username = "@amina",
        profileImageUrl = "https://cdn.example.com/u$id.jpg"
    )

    private fun samplePost(id: Long = 201L, comments: List<Comment> = emptyList()) = Post(
        id = id,
        userId = 101L,
        author = sampleUser(101L),
        content = "Hello Feathered",
        timestamp = "Just now",
        comments = comments,
        commentsCount = comments.size.coerceAtLeast(1)
    )

    private fun sampleProduct(id: Long = 301L) = Product(
        id = id,
        title = "Studio Headphones",
        price = 85.0,
        category = "Electronics",
        location = "Nairobi",
        description = "Clean studio audio",
        imageUrl = "https://cdn.example.com/p$id.jpg",
        seller = sampleUser(101L)
    )

    // 1. First-time user profile opening
    // 12. Slow network: smooth progress < 1f until ready, glow near completion, finishing phase at 1f
    @Test
    fun firstProfileOpen_andSlowNetwork_showsProgressNeverReports100BeforeReady() = runBlocking {
        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 40L,
            finishingHoldMs = 50L,
            requestTimeoutMs = 5_000L
        )
        val states = mutableListOf<NavigationLoadingState>()
        val collector = launch { controller.state.collect { states.add(it) } }

        val gate = CompletableDeferred<User>()
        var committedUser: User? = null
        val commitDone = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.PROFILE,
            hasCachedData = false,
            onFetchData = { reportProgress ->
                reportProgress(0.42f)
                val fetched = gate.await()
                reportProgress(0.84f)
                fetched
            },
            onCommit = { user ->
                committedUser = user
                commitDone.complete(Unit)
            }
        )

        // Wait past perceptibleDelayMs while network gate is still open
        delay(220L)

        val duringLoad = states.filter { it.phase == NavigationPhase.LOADING }
        assertTrue("Ring should become visible during slow network load", duringLoad.any { it.isVisible })
        assertTrue(
            "Progress must NEVER reach 1.0f before destination data is ready",
            duringLoad.all { it.progress < 1.0f }
        )
        assertNull("Profile must not commit before fetch completes", committedUser)

        // Complete slow fetch
        gate.complete(sampleUser(101L))
        commitDone.await()
        delay(30L)
        collector.cancel()

        val finishingStates = states.filter { it.phase == NavigationPhase.FINISHING }
        assertTrue("Finishing phase should animate to 1.0f with glow", finishingStates.any { it.progress == 1.0f && it.isFinishingGlow })
        assertEquals(101L, committedUser?.id)
        assertEquals(NavigationPhase.IDLE, controller.state.value.phase)
        assertFalse(controller.state.value.isVisible)
    }

    // 2. Second-time cached user profile opening + background stale refresh
    @Test
    fun secondProfileOpen_withCachedData_opensImmediatelyWithZeroLoader() = runBlocking {
        val cachedUser = sampleUser(101L)
        SmartNavigationCache.cacheProfile(cachedUser, nowMs = System.currentTimeMillis() - 10 * 60_000L)
        assertFalse(SmartNavigationCache.getProfile(101L)!!.isFresh())

        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 30L,
            finishingHoldMs = 40L
        )
        val observedVisible = mutableListOf<Boolean>()
        val collector = launch { controller.state.collect { observedVisible.add(it.isVisible) } }

        var committedName: String? = null
        val refreshedDone = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.PROFILE,
            hasCachedData = true,
            onCachedCommit = {
                committedName = SmartNavigationCache.getProfile(101L)?.data?.name
            },
            onBackgroundRefresh = {
                delay(30L)
                SmartNavigationCache.cacheProfile(cachedUser.copy(name = "Amina Updated"))
                refreshedDone.complete(Unit)
            },
            onFetchData = { error("Should not block on foreground fetch when cached") },
            onCommit = { }
        )

        // Immediate commit happens synchronously before background refresh
        assertEquals("Amina Juma", committedName)
        refreshedDone.await()
        delay(20L)
        collector.cancel()

        assertTrue("Second open must never show loading ring", observedVisible.none { it })
        assertEquals("Amina Updated", SmartNavigationCache.getProfile(101L)?.data?.name)
    }

    // 3 & 4. First-time post opening and reopening previously visited post
    @Test
    fun postOpening_firstFetchThenCachedReopen() = runBlocking {
        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 35L,
            finishingHoldMs = 35L
        )
        var firstCommitted: Post? = null
        val firstDone = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.POST,
            hasCachedData = false,
            onFetchData = { reportProgress ->
                delay(80L)
                reportProgress(0.85f)
                samplePost(201L)
            },
            onCommit = { post ->
                SmartNavigationCache.cachePost(post)
                firstCommitted = post
                firstDone.complete(Unit)
            }
        )
        firstDone.await()
        delay(20L)
        assertNotNull(firstCommitted)
        assertNotNull(SmartNavigationCache.getPost(201L))

        // Reopen cached post: zero loader
        val reopenStates = mutableListOf<NavigationLoadingState>()
        val collector = launch { controller.state.collect { reopenStates.add(it) } }
        var reopenedPost: Post? = null

        controller.navigate(
            destinationType = DestinationType.POST,
            hasCachedData = true,
            onCachedCommit = { reopenedPost = SmartNavigationCache.getPost(201L)?.data },
            onFetchData = { null },
            onCommit = { }
        )
        delay(20L)
        collector.cancel()

        assertEquals(201L, reopenedPost?.id)
        assertTrue(reopenStates.none { it.isVisible })
    }

    // 5. Opening comments (cached vs uncached + TTL + invalidation)
    @Test
    fun commentsCache_storesRetrievesAndInvalidatesCleanly() = runBlocking {
        val c1 = Comment(id = 1L, author = sampleUser(10L), text = "Great post!", timestamp = "1m")
        CommentsCache.setCachedComments("post", 201L, listOf(c1))

        val cached = CommentsCache.getCachedComments("post", 201L)
        assertNotNull(cached)
        assertEquals(1, cached!!.size)
        assertTrue(CommentsCache.isFresh("post", 201L))

        // Evict deleted/hidden post across caches
        SmartNavigationCache.cachePost(samplePost(201L, listOf(c1)))
        SmartNavigationCache.invalidatePost(201L)
        assertNull(SmartNavigationCache.getPost(201L))
        assertNull(CommentsCache.getCachedComments("post", 201L))
    }

    // 6. Opening MarketPoint listing
    @Test
    fun marketPointItem_loadsUncachedAndOpensCachedInstantly() = runBlocking {
        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 30L,
            finishingHoldMs = 30L
        )
        var loadedProduct: Product? = null
        val done = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.MARKETPLACE_ITEM,
            hasCachedData = false,
            onFetchData = { reportProgress ->
                delay(70L)
                reportProgress(0.88f)
                sampleProduct(301L)
            },
            onCommit = { p ->
                SmartNavigationCache.cacheProduct(p)
                loadedProduct = p
                done.complete(Unit)
            }
        )
        done.await()
        assertEquals(301L, loadedProduct?.id)

        var secondHit: Product? = null
        controller.navigate(
            destinationType = DestinationType.MARKETPLACE_ITEM,
            hasCachedData = SmartNavigationCache.getProduct(301L) != null,
            onCachedCommit = { secondHit = SmartNavigationCache.getProduct(301L)?.data },
            onFetchData = { null },
            onCommit = { }
        )
        assertEquals("Studio Headphones", secondHit?.title)
        assertFalse(controller.state.value.isVisible)
    }

    // 7. Opening conversation
    @Test
    fun conversationOpening_usesMessagesDestinationAndInstantCache() = runBlocking {
        val controller = SmartNavigationController(scope = scope)
        var openedConvId: String? = null
        controller.navigate(
            destinationType = DestinationType.MESSAGES,
            hasCachedData = true,
            onCachedCommit = { openedConvId = "conv_77" },
            onFetchData = { null },
            onCommit = { }
        )
        assertEquals("conv_77", openedConvId)
        assertEquals(DestinationType.MESSAGES, controller.lastTransitionType.value)
        assertFalse(controller.state.value.isVisible)
    }

    // 8. Opening notifications + viewer-scoped caching
    @Test
    fun notificationsCache_isViewerScopedAndLoadsUnvisitedWithRing() = runBlocking {
        val notif = Notification(
            id = "n1",
            user = sampleUser(12L),
            content = "liked your photo",
            type = NotificationType.LIKE
        )
        SmartNavigationCache.cacheNotifications(viewerUserId = 10L, list = listOf(notif))
        assertNotNull(SmartNavigationCache.getNotifications(10L))
        assertNull("Other viewer must never inherit cached notifications", SmartNavigationCache.getNotifications(99L))

        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 30L,
            finishingHoldMs = 30L
        )
        val done = CompletableDeferred<Unit>()
        var committedSize = 0
        controller.navigate(
            destinationType = DestinationType.NOTIFICATIONS,
            hasCachedData = false,
            onFetchData = { reportProgress ->
                delay(65L)
                reportProgress(0.8f)
                listOf(notif)
            },
            onCommit = { list ->
                committedSize = list.size
                done.complete(Unit)
            }
        )
        done.await()
        delay(20L)
        assertEquals(1, committedSize)
        assertEquals(NavigationPhase.IDLE, controller.state.value.phase)
    }

    // 9. Rapidly tapping multiple destinations -> single-flight cancellation, only last wins
    @Test
    fun rapidTaps_cancelSupersededRequestsAndOnlyLatestCommits() = runBlocking {
        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 30L,
            finishingHoldMs = 30L
        )
        val committedDestinations = mutableListOf<String>()
        val finalDone = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.PROFILE,
            hasCachedData = false,
            onFetchData = {
                delay(250L)
                "profile_1"
            },
            onCommit = { committedDestinations.add(it) }
        )
        delay(25L)

        controller.navigate(
            destinationType = DestinationType.POST,
            hasCachedData = false,
            onFetchData = {
                delay(200L)
                "post_2"
            },
            onCommit = { committedDestinations.add(it) }
        )
        delay(25L)

        controller.navigate(
            destinationType = DestinationType.MARKETPLACE_ITEM,
            hasCachedData = false,
            onFetchData = { reportProgress ->
                delay(70L)
                reportProgress(0.90f)
                "product_3"
            },
            onCommit = {
                committedDestinations.add(it)
                finalDone.complete(Unit)
            }
        )

        finalDone.await()
        delay(320L) // Wait longer than cancelled tasks to ensure stale commits never fire

        assertEquals(listOf("product_3"), committedDestinations)
        assertEquals(NavigationPhase.IDLE, controller.state.value.phase)
        assertFalse(controller.state.value.isVisible)
    }

    // 10. Returning to Home via Back & exact scroll restoration
    @Test
    fun scrollRestoration_preservesExactFeedAndCommentPositionsOnBack() = runBlocking {
        val scrollController = ScrollRestorationController()
        scrollController.saveListPosition("FEED", index = 14, offset = 285)
        scrollController.saveGridPosition("MARKETPLACE", index = 8, offset = 120)
        scrollController.saveListPosition("COMMENTS_201", index = 5, offset = 40)

        // Verify returning to FEED does not wipe saved coordinates
        assertEquals(ScrollRestorationController.ListPosition(14, 285), scrollController.getSavedListPosition("FEED"))
        assertEquals(ScrollRestorationController.ListPosition(8, 120), scrollController.getSavedGridPosition("MARKETPLACE"))
        assertEquals(ScrollRestorationController.ListPosition(5, 40), scrollController.getSavedListPosition("COMMENTS_201"))

        // Tapping active tab explicitly scrolls to top
        scrollController.scrollToTop("FEED", animate = false)
        assertEquals(ScrollRestorationController.ListPosition(0, 0), scrollController.getSavedListPosition("FEED"))
        // Other routes stay intact
        assertEquals(ScrollRestorationController.ListPosition(8, 120), scrollController.getSavedGridPosition("MARKETPLACE"))
    }

    // 11. Failed network request resets loader cleanly and calls fallback
    @Test
    fun failedNetworkRequest_resetsRingToIdleAndInvokesFallback() = runBlocking {
        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 25L,
            finishingHoldMs = 30L
        )
        var fallbackInvoked = false
        val fallbackDone = CompletableDeferred<Unit>()

        controller.navigate<User>(
            destinationType = DestinationType.PROFILE,
            hasCachedData = false,
            onFetchData = {
                delay(60L)
                throw IllegalStateException("Network 500")
            },
            onCommit = { error("Commit must not run on failure") },
            onUnavailableOrError = {
                fallbackInvoked = true
                fallbackDone.complete(Unit)
            }
        )

        fallbackDone.await()
        delay(30L)
        assertTrue(fallbackInvoked)
        assertEquals(NavigationPhase.IDLE, controller.state.value.phase)
        assertFalse(controller.state.value.isVisible)
        assertEquals(0f, controller.state.value.progress, 0.001f)
    }

    // 13. Fast response (< perceptible delay) -> avoids flashing loading ring
    @Test
    fun ultraFastResponse_commitsWithoutFlashingRing() = runBlocking {
        val controller = SmartNavigationController(
            scope = scope,
            perceptibleDelayMs = 150L,
            finishingHoldMs = 150L
        )
        val states = mutableListOf<NavigationLoadingState>()
        val collector = launch { controller.state.collect { states.add(it) } }
        val done = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.PROFILE,
            hasCachedData = false,
            onFetchData = {
                delay(15L)
                sampleUser(505L)
            },
            onCommit = { done.complete(Unit) }
        )

        done.await()
        delay(30L)
        collector.cancel()

        assertTrue("Ring must remain hidden on ultra-fast load (< perceptible delay)", states.none { it.isVisible })
        assertEquals(NavigationPhase.IDLE, controller.state.value.phase)
    }

    // 14. Offline access to cached content
    @Test
    fun offlineAccess_opensCachedDestinationEvenWhenBackgroundRefreshFails() = runBlocking {
        SmartNavigationCache.cacheProfile(sampleUser(707L))
        val controller = SmartNavigationController(scope = scope)

        var openedOfflineUser: User? = null
        val refreshAttempted = CompletableDeferred<Unit>()

        controller.navigate(
            destinationType = DestinationType.PROFILE,
            hasCachedData = true,
            onCachedCommit = {
                openedOfflineUser = SmartNavigationCache.getProfile(707L)?.data
            },
            onBackgroundRefresh = {
                refreshAttempted.complete(Unit)
                throw java.io.IOException("Offline")
            },
            onFetchData = { null },
            onCommit = { }
        )

        refreshAttempted.await()
        delay(30L)
        assertEquals(707L, openedOfflineUser?.id)
        assertEquals(NavigationPhase.IDLE, controller.state.value.phase)
    }

    // 15. Logout clears all user-scoped caches & scroll states cleanly without affecting navigation readiness
    @Test
    fun logoutPrivacyCleanup_clearsAllUserScopedCachesAndScrollPositions() = runBlocking {
        SmartNavigationCache.cacheProfile(sampleUser(10L))
        SmartNavigationCache.cachePost(samplePost(20L))
        SmartNavigationCache.cacheNotifications(10L, listOf(Notification(id = "n1", user = sampleUser(2L), content = "hi", type = NotificationType.LIKE)))
        CommentsCache.setCachedComments("post", 20L, listOf(Comment(id = 1L, author = sampleUser(2L), text = "hi", timestamp = "now")))

        val scrollController = ScrollRestorationController()
        scrollController.saveListPosition("FEED", 9, 100)

        SmartNavigationCache.clearUserScopedCaches()
        scrollController.clearAll()

        assertNull(SmartNavigationCache.getProfile(10L))
        assertNull(SmartNavigationCache.getPost(20L))
        assertNull(SmartNavigationCache.getNotifications(10L))
        assertNull(CommentsCache.getCachedComments("post", 20L))
        assertEquals(ScrollRestorationController.ListPosition(0, 0), scrollController.getSavedListPosition("FEED"))
    }
}
