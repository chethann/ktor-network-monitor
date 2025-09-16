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

/** Visual styling modes for the monitor. */
enum class NetworkMonitorThemeStyle { Default, Terminal }

// Terminal / developer-console inspired palette (monokai-ish / solarized hybrid)
private val TerminalPrimary = Color(0xFF0DB9D7)      // Cyan accent
private val TerminalPrimaryVariant = Color(0xFF046E82)
private val TerminalSecondary = Color(0xFFFFC857)    // Soft amber
private val TerminalError = Color(0xFFFF5555)        // Vibrant red
private val TerminalWarning = Color(0xFFFFB86C)      // Orange
private val TerminalSuccess = Color(0xFF50FA7B)      // Neon green
private val TerminalInfo = Color(0xFF8BE9FD)         // Light cyan
private val TerminalBackgroundDark = Color(0xFF1E1F22) // Editor gutter dark
private val TerminalSurfaceDark = Color(0xFF26282B)
private val TerminalOnDark = Color(0xFFE6E8EA)
private val TerminalCodeBlock = Color(0xFF2D3033)
private val TerminalDivider = Color(0xFF3A3D41)

private val TerminalDarkMaterial = darkColors(
    primary = TerminalPrimary,
    primaryVariant = TerminalPrimaryVariant,
    secondary = TerminalSecondary,
    background = TerminalBackgroundDark,
    surface = TerminalSurfaceDark,
    error = TerminalError,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TerminalOnDark,
    onSurface = TerminalOnDark,
    onError = Color.Black
)

private val TerminalDarkExtended = ExtendedColors(
    success = TerminalSuccess,
    warning = TerminalWarning,
    info = TerminalInfo,
    pending = TerminalWarning.copy(alpha = 0.85f),
    chipBackground = Color(0xFF34373B),
    chipContent = TerminalOnDark,
    highlight = Color(0xFF3D2E00),        // subdued amber background for highlight
    focusHighlight = TerminalSecondary,   // strong focus highlight
    codeBackground = TerminalCodeBlock,
    divider = TerminalDivider
)

@Composable
fun NetworkMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: Colors? = null,
    style: NetworkMonitorThemeStyle = NetworkMonitorThemeStyle.Default,
    content: @Composable () -> Unit
) {
    val (materialColors, extended) = remember(darkTheme, style, colors) {
        when (style) {
            NetworkMonitorThemeStyle.Terminal -> {
                val mc = TerminalDarkMaterial // only dark variant for now
                mc to TerminalDarkExtended
            }
            NetworkMonitorThemeStyle.Default -> {
                val mc = colors ?: if (darkTheme) DarkColorPalette else LightColorPalette
                val ext = if (darkTheme) DarkExtended else LightExtended
                mc to ext
            }
        }
    }

    val rememberedExtended = remember(materialColors, extended) { extended }

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
