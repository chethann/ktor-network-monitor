package io.github.chethann.network.monitor.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Central theme file for Network Monitor UI. Applies a Material (M2) baseline theme plus
 * extended semantic colors we rely on throughout the library. Host apps can wrap the monitor
 * UI in [NetworkMonitorTheme] to get a cohesive professional look.
 */

// Base palette
private val PrimaryLight = Color(0xFF1565C0)
private val PrimaryDark = Color(0xFF90CAF9)
private val PrimaryVariant = Color(0xFF003C8F)
private val SecondaryLight = Color(0xFF00897B)
private val SecondaryDark = Color(0xFF4DB6AC)
private val Error = Color(0xFFEF5350)
private val Warning = Color(0xFFFFA000)
private val Success = Color(0xFF43A047)
private val Info = Color(0xFF0288D1)

private val LightColorPalette = lightColors(
    primary = PrimaryLight,
    primaryVariant = PrimaryVariant,
    secondary = SecondaryLight,
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    error = Error,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F2933),
    onSurface = Color(0xFF1F2933),
    onError = Color.White
)

private val DarkColorPalette = darkColors(
    primary = PrimaryDark,
    primaryVariant = PrimaryVariant,
    secondary = SecondaryDark,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Error,
    onPrimary = Color(0xFF0D1117),
    onSecondary = Color(0xFF0D1117),
    onBackground = Color(0xFFE6E8EA),
    onSurface = Color(0xFFE6E8EA),
    onError = Color.Black
)

@Immutable
class ExtendedColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val pending: Color,
    val chipBackground: Color,
    val chipContent: Color,
    val highlight: Color,
    val focusHighlight: Color,
    val codeBackground: Color,
    val divider: Color
)

private val LightExtended = ExtendedColors(
    success = Success,
    warning = Warning,
    info = Info,
    pending = Color(0xFFEF6C00),
    chipBackground = Color(0xFFF1F3F5),
    chipContent = Color(0xFF374151),
    highlight = Color(0xFFFFF59D),
    focusHighlight = Color(0xFFFFB74D),
    codeBackground = Color(0xFFF0F4F8),
    divider = Color(0xFFE2E8F0)
)

private val DarkExtended = ExtendedColors(
    success = Success.copy(alpha = 0.85f),
    warning = Warning.copy(alpha = 0.85f),
    info = Info.copy(alpha = 0.85f),
    pending = Color(0xFFFF9800),
    chipBackground = Color(0xFF2A2F35),
    chipContent = Color(0xFFE6E8EA),
    highlight = Color(0xFF5E4300),
    focusHighlight = Color(0xFFFF9800),
    codeBackground = Color(0xFF1E242A),
    divider = Color(0xFF31373D)
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

object NetworkMonitorThemeDefaults {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun NetworkMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: Colors? = null,
    content: @Composable () -> Unit
) {
    val materialColors = colors ?: if (darkTheme) DarkColorPalette else LightColorPalette
    val extended = if (darkTheme) DarkExtended else LightExtended

    val rememberedExtended = remember(darkTheme) { extended }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides rememberedExtended
    ) {
        MaterialTheme(
            colors = materialColors,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}

// Convenient extension accessors
val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
