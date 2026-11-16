package com.unera.social.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

/**
 * Database-backed background record fetched from GET /api/backgrounds/animated.
 * Used by the Reality background category (both static images and animated WebP/GIF).
 */
data class RemotePostBackground(
    val id: String,
    val name: String,
    val url: String,
    val thumbnailUrl: String = "",
    val posterUrl: String = "",
    val isAnimated: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val dominantColor: String? = null,
    val textColor: String? = null,
    val overlayColor: String? = null,
    val overlayOpacity: Float = 0.25f,
    val category: String = "reality",
    val sortOrder: Int = 0
) {
    val effectiveThumbnailUrl: String
        get() = thumbnailUrl.ifBlank { posterUrl }.ifBlank { url }

    val effectivePosterUrl: String
        get() = posterUrl.ifBlank { thumbnailUrl }.ifBlank { url }

    val styleId: String
        get() = "reality:$id"

    val backgroundTypeValue: String
        get() = if (isAnimated) "animated_image" else "static_image"

    fun parsedDominantColor(): Color? = parseCssOrHexColor(dominantColor)

    fun parsedTextColor(): Color? = parseCssOrHexColor(textColor)

    fun parsedOverlayColor(): Color = parseCssOrHexColor(overlayColor) ?: Color.Black

    /**
     * Computes effective background color after applying database overlay_color + overlay_opacity
     * over dominant_color, so automatic text contrast works accurately.
     */
    fun blendedBaseColor(): Color? {
        val dom = parsedDominantColor() ?: return null
        val ov = parsedOverlayColor()
        val a = overlayOpacity.coerceIn(0f, 0.9f)
        val r = dom.red * (1f - a) + ov.red * a
        val g = dom.green * (1f - a) + ov.green * a
        val b = dom.blue * (1f - a) + ov.blue * a
        return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), 1f)
    }

    /**
     * Resolves recommended initial text color from database metadata (`text_color`)
     * or automatically computes high-contrast light/dark color from `dominant_color` + overlay.
     */
    fun resolvedDefaultTextColor(): Color {
        parsedTextColor()?.let { return it }
        val base = blendedBaseColor()
        return if (base != null && base.luminance() > 0.62f) {
            Color(0xFF0F172A)
        } else {
            Color.White
        }
    }

    /**
     * Computes subtle overlay opacity respecting database metadata while boosting contrast
     * slightly if the chosen text color is close in luminance to the dominant image color.
     */
    fun effectiveOverlayAlpha(chosenTextColor: Color): Float {
        val dbAlpha = overlayOpacity.coerceIn(0f, 0.85f)
        val dom = parsedDominantColor()
        val ov = parsedOverlayColor()
        val textIsLight = chosenTextColor.luminance() >= 0.45f
        val overlayIsDark = ov.luminance() < 0.35f

        if (dom == null) {
            return if (dbAlpha > 0f) dbAlpha else if (textIsLight) 0.24f else 0.14f
        }

        val lumDiff = kotlin.math.abs(chosenTextColor.luminance() - dom.luminance())
        return when {
            lumDiff < 0.28f && textIsLight && overlayIsDark -> max(dbAlpha, 0.36f)
            lumDiff < 0.28f && !textIsLight && !overlayIsDark -> max(dbAlpha, 0.34f)
            dbAlpha > 0f -> dbAlpha
            else -> if (textIsLight) 0.20f else 0.12f
        }
    }

    fun normalizedSubCategory(): String {
        val raw = category.trim()
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return "All"
        return raw.replace('_', ' ')
            .replace('-', ' ')
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.equals("3d", ignoreCase = true)) "3D"
                else if (word.equals("ai", ignoreCase = true)) "AI"
                else word.replaceFirstChar { it.uppercase() }
            }
            .ifBlank { "All" }
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("url", url)
        put("thumbnail_url", thumbnailUrl)
        put("poster_url", posterUrl)
        put("is_animated", isAnimated)
        put("width", width)
        put("height", height)
        if (!dominantColor.isNullOrBlank()) put("dominant_color", dominantColor)
        if (!textColor.isNullOrBlank()) put("text_color", textColor)
        if (!overlayColor.isNullOrBlank()) put("overlay_color", overlayColor)
        put("overlay_opacity", overlayOpacity.toDouble())
        put("category", category)
        put("sort_order", sortOrder)
    }

    companion object {
        fun parseCssOrHexColor(raw: String?): Color? {
            if (raw.isNullOrBlank()) return null
            val clean = raw.trim()
            if (clean.equals("null", ignoreCase = true) || clean.equals("undefined", ignoreCase = true)) return null
            return try {
                if (clean.startsWith("#")) {
                    val hex = clean.removePrefix("#")
                    when (hex.length) {
                        3 -> {
                            val r = hex[0].toString().repeat(2).toInt(16)
                            val g = hex[1].toString().repeat(2).toInt(16)
                            val b = hex[2].toString().repeat(2).toInt(16)
                            Color(r, g, b)
                        }
                        6 -> {
                            val parsed = java.lang.Long.parseLong(hex, 16)
                            Color(0xFF000000L or parsed)
                        }
                        8 -> {
                            val parsed = java.lang.Long.parseLong(hex, 16)
                            Color(parsed)
                        }
                        else -> null
                    }
                } else if (clean.startsWith("rgb", ignoreCase = true)) {
                    val nums = Regex("""[\d.]+""").findAll(clean).mapNotNull { it.value.toFloatOrNull() }.toList()
                    if (nums.size >= 3) {
                        val r = (nums[0].toInt()).coerceIn(0, 255)
                        val g = (nums[1].toInt()).coerceIn(0, 255)
                        val b = (nums[2].toInt()).coerceIn(0, 255)
                        val a = if (nums.size >= 4) {
                            val rawA = nums[3]
                            if (rawA <= 1f) (rawA * 255f).toInt().coerceIn(0, 255) else rawA.toInt().coerceIn(0, 255)
                        } else 255
                        Color(r, g, b, a)
                    } else null
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

object BackgroundImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun isLowEndDevice(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am == null) false
            else am.isLowRamDevice || am.memoryClass <= 128
        } catch (_: Exception) {
            false
        }
    }

    fun get(context: Context): ImageLoader {
        val current = instance
        if (current != null) return current
        return synchronized(this) {
            instance ?: buildLoader(context.applicationContext).also { instance = it }
        }
    }

    private fun buildLoader(appContext: Context): ImageLoader {
        val lowEnd = isLowEndDevice(appContext)
        return ImageLoader.Builder(appContext)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(if (lowEnd) 0.10 else 0.18)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, "feathered_bg_cache"))
                    .maxSizeBytes(if (lowEnd) 30L * 1024 * 1024 else 64L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(180)
            .build()
    }
}

object BackgroundsApiService {
    private const val BASE_URL = "https://featheredsocial.site"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val PREFS_NAME = "feathered_backgrounds_cache"
    private const val KEY_CACHE_JSON = "animated_backgrounds_json"

    private val fetchMutex = Mutex()
    @Volatile
    private var cacheInitialized = false
    @Volatile
    private var hasFetchedFromNetwork = false

    private val _backgrounds = MutableStateFlow<List<RemotePostBackground>>(emptyList())
    val backgrounds: StateFlow<List<RemotePostBackground>> = _backgrounds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun initCache(context: Context) {
        if (cacheInitialized) return
        synchronized(this) {
            if (cacheInitialized) return
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val cachedJson = prefs.getString(KEY_CACHE_JSON, null)
                if (!cachedJson.isNullOrBlank()) {
                    val parsed = parseArray(JSONArray(cachedJson))
                    if (parsed.isNotEmpty() && _backgrounds.value.isEmpty()) {
                        _backgrounds.value = parsed
                    }
                }
            } catch (_: Exception) {
            } finally {
                cacheInitialized = true
            }
        }
    }

    suspend fun ensureLoaded(context: Context) {
        initCache(context)
        if (!hasFetchedFromNetwork) {
            fetchBackgrounds(context = context, forceRefresh = false)
        }
    }

    suspend fun fetchBackgrounds(
        context: Context? = null,
        forceRefresh: Boolean = false
    ): List<RemotePostBackground> = withContext(Dispatchers.IO) {
        if (context != null) initCache(context)

        fetchMutex.withLock {
            if (!forceRefresh && hasFetchedFromNetwork && _backgrounds.value.isNotEmpty()) {
                return@withLock _backgrounds.value
            }

            _isLoading.value = true
            _errorMessage.value = null
            try {
                var parsed = requestEndpoint("$BASE_URL/api/backgrounds/animated")
                if (parsed.isEmpty()) {
                    parsed = requestEndpoint("$BASE_URL/api/backgrounds/animated?category=reality")
                }
                if (parsed.isEmpty()) {
                    parsed = requestEndpoint("$BASE_URL/api/backgrounds/animated?all=true")
                }

                if (parsed.isNotEmpty()) {
                    hasFetchedFromNetwork = true
                    _backgrounds.value = parsed
                    if (context != null) {
                        saveToCache(context.applicationContext, parsed)
                    }
                } else if (_backgrounds.value.isEmpty()) {
                    _errorMessage.value = "No backgrounds available right now."
                }
            } catch (e: Exception) {
                if (_backgrounds.value.isEmpty()) {
                    _errorMessage.value = e.message ?: "Unable to load backgrounds."
                }
            } finally {
                _isLoading.value = false
            }
            _backgrounds.value
        }
    }

    private fun requestEndpoint(endpoint: String): List<RemotePostBackground> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (conn.responseCode in 200..299) {
                val raw = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                parseResponse(raw)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseResponse(raw: String): List<RemotePostBackground> {
        if (raw.isBlank()) return emptyList()
        return when {
            raw.startsWith("[") -> parseArray(JSONArray(raw))
            raw.startsWith("{") -> {
                val root = JSONObject(raw)
                val arr = root.optJSONArray("backgrounds")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("items")
                    ?: root.optJSONArray("records")
                    ?: root.optJSONArray("results")
                if (arr != null) parseArray(arr) else emptyList()
            }
            else -> emptyList()
        }
    }

    private fun parseArray(arr: JSONArray): List<RemotePostBackground> {
        val list = mutableListOf<RemotePostBackground>()
        val seenIds = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val parsed = parseItem(obj, i) ?: continue
            if (seenIds.add(parsed.id)) {
                list.add(parsed)
            }
        }
        return list.sortedWith(compareBy<RemotePostBackground> { it.sortOrder }.thenBy { it.name.lowercase() })
    }

    private fun parseItem(obj: JSONObject, index: Int): RemotePostBackground? {
        val rawId = obj.opt("id")?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        val rawUrl = resolveMediaUrl(obj.optString("url"))
        if (rawUrl.isBlank()) return null

        val name = obj.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: "Reality Scene"
        val thumb = resolveMediaUrl(obj.optString("thumbnail_url")).ifBlank {
            resolveMediaUrl(obj.optString("poster_url"))
        }.ifBlank { rawUrl }
        val poster = resolveMediaUrl(obj.optString("poster_url")).ifBlank { thumb }

        val lowerUrl = rawUrl.lowercase()
        val explicitAnimated = when {
            obj.has("is_animated") && !obj.isNull("is_animated") -> {
                val rawVal = obj.opt("is_animated")
                when (rawVal) {
                    is Boolean -> rawVal
                    is Number -> rawVal.toInt() != 0
                    is String -> rawVal.equals("true", ignoreCase = true) || rawVal == "1"
                    else -> false
                }
            }
            else -> lowerUrl.contains(".gif") || lowerUrl.contains("animated")
        }

        val width = obj.optInt("width", 0).coerceAtLeast(0)
        val height = obj.optInt("height", 0).coerceAtLeast(0)
        val dominantColor = obj.optString("dominant_color").takeIf { it.isNotBlank() && it != "null" }
        val textColor = obj.optString("text_color").takeIf { it.isNotBlank() && it != "null" }
        val overlayColor = obj.optString("overlay_color").takeIf { it.isNotBlank() && it != "null" }

        val rawOpacity = if (obj.has("overlay_opacity") && !obj.isNull("overlay_opacity")) {
            val v = obj.optDouble("overlay_opacity", 0.25)
            if (v.isNaN()) 0.25f else if (v > 1.0) (v / 100.0).toFloat() else v.toFloat()
        } else 0.25f

        val category = obj.optString("category").takeIf { it.isNotBlank() && it != "null" } ?: "reality"
        val sortOrder = obj.optInt("sort_order", index)

        return RemotePostBackground(
            id = rawId,
            name = name,
            url = rawUrl,
            thumbnailUrl = thumb,
            posterUrl = poster,
            isAnimated = explicitAnimated,
            width = width,
            height = height,
            dominantColor = dominantColor,
            textColor = textColor,
            overlayColor = overlayColor,
            overlayOpacity = min(0.85f, max(0f, rawOpacity)),
            category = category,
            sortOrder = sortOrder
        )
    }

    private fun resolveMediaUrl(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        if (trimmed == "null" || trimmed == "undefined") return ""
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "$BASE_URL$trimmed"
            else -> "$BASE_URL/$trimmed"
        }
    }

    private fun saveToCache(appContext: Context, list: List<RemotePostBackground>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                arr.put(item.toJsonObject())
            }
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHE_JSON, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    /**
     * Resolves a post's backgroundStyle string (e.g. "reality:14|bgtype:animated_image|...")
     * or raw background ID / URL against the loaded database records.
     */
    fun findRemoteBackground(styleOrId: String?): RemotePostBackground? {
        if (styleOrId.isNullOrBlank()) return null
        val clean = styleOrId.trim().removeSurrounding("\"").replace("\\\"", "\"")
        val firstPart = clean.substringBefore("|").trim()
        val list = _backgrounds.value

        val extractedId = when {
            firstPart.startsWith("reality:", ignoreCase = true) -> firstPart.substringAfter(":").trim()
            firstPart.startsWith("db:", ignoreCase = true) -> firstPart.substringAfter(":").trim()
            firstPart.startsWith("remote:", ignoreCase = true) -> firstPart.substringAfter(":").trim()
            firstPart.startsWith("bg:", ignoreCase = true) -> firstPart.substringAfter(":").trim()
            firstPart.startsWith("realistic_db:", ignoreCase = true) -> firstPart.substringAfter(":").trim()
            else -> firstPart
        }

        if (extractedId.isNotBlank()) {
            list.find { it.id.equals(extractedId, ignoreCase = true) }?.let { return it }
        }

        val extractedUrl = when {
            firstPart.startsWith("url:", ignoreCase = true) -> firstPart.substringAfter(":").trim()
            firstPart.startsWith("http://", ignoreCase = true) || firstPart.startsWith("https://", ignoreCase = true) -> firstPart
            else -> null
        }
        if (!extractedUrl.isNullOrBlank()) {
            list.find {
                it.url.equals(extractedUrl, ignoreCase = true) ||
                    it.thumbnailUrl.equals(extractedUrl, ignoreCase = true) ||
                    it.posterUrl.equals(extractedUrl, ignoreCase = true)
            }?.let { return it }

            return RemotePostBackground(
                id = extractedUrl.hashCode().toString(),
                name = "Reality",
                url = extractedUrl,
                thumbnailUrl = extractedUrl,
                posterUrl = extractedUrl,
                isAnimated = clean.contains("animated_image", ignoreCase = true) ||
                    extractedUrl.lowercase().let { it.contains(".gif") || it.contains(".webp") }
            )
        }

        return null
    }

    fun isRealityStyle(styleOrId: String?): Boolean {
        if (styleOrId.isNullOrBlank()) return false
        val clean = styleOrId.trim().removeSurrounding("\"").replace("\\\"", "\"")
        val firstPart = clean.substringBefore("|").trim().lowercase()
        return firstPart.startsWith("reality:") ||
            firstPart.startsWith("db:") ||
            firstPart.startsWith("remote:") ||
            firstPart.startsWith("bg:") ||
            firstPart.startsWith("realistic_db:") ||
            firstPart.startsWith("url:") ||
            firstPart.startsWith("http://") ||
            firstPart.startsWith("https://") ||
            _backgrounds.value.any { it.id.equals(firstPart, ignoreCase = true) }
    }
}
