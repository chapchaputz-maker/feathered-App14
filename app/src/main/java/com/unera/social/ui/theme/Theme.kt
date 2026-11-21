package com.unera.social.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {
    private const val PREFS_NAME = "feathered_theme_prefs"
    private const val KEY_LIGHT_MODE = "is_light_mode"

    private val _isLightModeFlow = MutableStateFlow(false)
    val isLightModeFlow: StateFlow<Boolean> = _isLightModeFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isLightModeFlow.value = prefs.getBoolean(KEY_LIGHT_MODE, false)
    }

    fun setLightMode(context: Context, enabled: Boolean) {
        _isLightModeFlow.value = enabled
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIGHT_MODE, enabled)
            .apply()
    }
}

@Immutable
data class FeatheredColors(
    val isLight: Boolean,
    // App Header (TopBar / Screen Top Bars)
    val headerBg: Color,
    val headerBgEnd: Color,
    val headerText: Color,
    val headerSubtext: Color,
    val headerIconTint: Color,
    val headerIconBg: Color,
    val headerIconBgActive: Color,
    val headerIconBorder: Color,
    val headerDivider: Color,
    val headerBadgeBorder: Color,
    // Main Screens & Surfaces
    val bgScreen: Color,
    val bgSurface: Color,
    val bgCard: Color,
    val bgCardElevated: Color,
    val bgCardDeep: Color,
    val bgInput: Color,
    val bgChip: Color,
    val bgBlueTint: Color,
    // Borders & Dividers
    val border: Color,
    val borderStrong: Color,
    val sectionDivider: Color,
    // Text
    val textPrimary: Color,
    val textBody: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Accents & Navigation
    val primaryBlue: Color,
    val primaryBlueHover: Color,
    val navBg: Color,
    val navBorder: Color,
    val navActive: Color,
    val navInactive: Color,
    val navPostBorder: Color,
    val avatarBorder: Color,
    // Semantic / Status
    val dangerBg: Color,
    val dangerText: Color,
    val successText: Color,
    val scrim: Color
)

val DarkFeatheredColors = FeatheredColors(
    isLight = false,
    headerBg = Color(0xFF0B1120),
    headerBgEnd = Color(0xFF0B1120),
    headerText = Color(0xFFFFFFFF),
    headerSubtext = Color(0xFF94A3B8),
    headerIconTint = Color(0xFFFFFFFF),
    headerIconBg = Color(0xFF131D33),
    headerIconBgActive = Color(0xFF1E2D4A),
    headerIconBorder = Color.Transparent,
    headerDivider = Color(0xFF1E293B),
    headerBadgeBorder = Color(0xFF0B1120),
    bgScreen = Color(0xFF050B18),
    bgSurface = Color(0xFF0B1120),
    bgCard = Color(0xFF0F172A),
    bgCardElevated = Color(0xFF162137),
    bgCardDeep = Color(0xFF131D33),
    bgInput = Color(0xFF1E293B),
    bgChip = Color(0xFF1E293B),
    bgBlueTint = Color(0x291877F2),
    border = Color(0xFF1E293B),
    borderStrong = Color(0xFF334155),
    sectionDivider = Color(0xFF1E293B),
    textPrimary = Color(0xFFF8FAFC),
    textBody = Color(0xFFE4E6EB),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    primaryBlue = Color(0xFF1877F2),
    primaryBlueHover = Color(0xFF166FE5),
    navBg = Color(0xFF0B1120),
    navBorder = Color(0xFF1E293B),
    navActive = Color(0xFF1877F2),
    navInactive = Color(0xFF94A3B8),
    navPostBorder = Color(0xFF050B18),
    avatarBorder = Color(0xFF334155),
    dangerBg = Color(0x4D7F1D1D),
    dangerText = Color(0xFFF87171),
    successText = Color(0xFF10B981),
    scrim = Color(0x73000000)
)

val LightFeatheredColors = FeatheredColors(
    isLight = true,
    headerBg = Color(0xFF1877F2),
    headerBgEnd = Color(0xFF1464D0),
    headerText = Color(0xFFFFFFFF),
    headerSubtext = Color(0xFFE7F3FF),
    headerIconTint = Color(0xFFFFFFFF),
    headerIconBg = Color(0x2EFFFFFF),
    headerIconBgActive = Color(0x4DFFFFFF),
    headerIconBorder = Color(0x3DFFFFFF),
    headerDivider = Color(0xFF135EC4),
    headerBadgeBorder = Color(0xFF1877F2),
    bgScreen = Color(0xFFF0F2F5),
    bgSurface = Color(0xFFFFFFFF),
    bgCard = Color(0xFFFFFFFF),
    bgCardElevated = Color(0xFFF4F7FB),
    bgCardDeep = Color(0xFFEAF1FA),
    bgInput = Color(0xFFF0F2F5),
    bgChip = Color(0xFFE4E8EF),
    bgBlueTint = Color(0xFFE7F3FF),
    border = Color(0xFFE4E6EB),
    borderStrong = Color(0xFFCBD5E1),
    sectionDivider = Color(0xFFDFE3EA),
    textPrimary = Color(0xFF050505),
    textBody = Color(0xFF1C1E21),
    textSecondary = Color(0xFF5F6672),
    textMuted = Color(0xFF8A8D91),
    primaryBlue = Color(0xFF1877F2),
    primaryBlueHover = Color(0xFF166FE5),
    navBg = Color(0xFFFFFFFF),
    navBorder = Color(0xFFE4E6EB),
    navActive = Color(0xFF1877F2),
    navInactive = Color(0xFF65676B),
    navPostBorder = Color(0xFFFFFFFF),
    avatarBorder = Color(0xFFD0D7E2),
    dangerBg = Color(0xFFFEE2E2),
    dangerText = Color(0xFFDC2626),
    successText = Color(0xFF059669),
    scrim = Color(0x55000000)
)

val LocalFeatheredColors = staticCompositionLocalOf { DarkFeatheredColors }
val LocalIsLightMode = staticCompositionLocalOf { false }

val AppColors: FeatheredColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFeatheredColors.current

private val FeatheredDarkColorScheme = darkColorScheme(
    primary = UneraBlue,
    onPrimary = TextPrimary,
    primaryContainer = UneraBlueHover,
    onPrimaryContainer = TextPrimary,
    secondary = VerifiedBlue,
    onSecondary = TextPrimary,
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy800,
    onSurface = TextPrimary,
    surfaceVariant = Navy700,
    onSurfaceVariant = TextSecondary,
    outline = Navy600
)

private val FeatheredLightColorScheme = lightColorScheme(
    primary = Color(0xFF1877F2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F3FF),
    onPrimaryContainer = Color(0xFF0E4EB0),
    secondary = Color(0xFF1877F2),
    onSecondary = Color.White,
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF050505),
    surface = Color.White,
    onSurface = Color(0xFF050505),
    surfaceVariant = Color(0xFFF4F7FB),
    onSurfaceVariant = Color(0xFF65676B),
    outline = Color(0xFFD0D7E2)
)

@Composable
fun FeatheredTheme(
    isLightMode: Boolean? = null,
    content: @Composable () -> Unit
) {
    val persistedLightMode by ThemePreferences.isLightModeFlow.collectAsState()
    val useLightMode = isLightMode ?: persistedLightMode
    val featheredColors = if (useLightMode) LightFeatheredColors else DarkFeatheredColors
    val colorScheme = if (useLightMode) FeatheredLightColorScheme else FeatheredDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = featheredColors.headerBg.toArgb()
                window.navigationBarColor = featheredColors.navBg.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = useLightMode
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = InterTypography
    ) {
        CompositionLocalProvider(
            LocalFeatheredColors provides featheredColors,
            LocalIsLightMode provides useLightMode,
            LocalTextStyle provides LocalTextStyle.current.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                color = featheredColors.textPrimary
            ),
            content = content
        )
    }
}
