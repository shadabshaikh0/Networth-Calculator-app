package com.shadabshaikh.networth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's custom design tokens, ported from the web app's DARK_VARS /
 * LIGHT_VARS. Carried through the tree via [LocalNwColors] so any composable can
 * read `nwColors.card`, `nwColors.text2`, etc.
 */
data class NwColors(
    val bg: Color,
    val card: Color,
    val cardBorder: Color,
    val hair: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val muted: Color,
    val inputBg: Color,
    val inputBorder: Color,
    val chipBorder: Color,
    val track: Color,
    val grid: Color,
    val ctaBg: Color,
    val ctaTx: Color,
    val topbar: Color,
    val topBorder: Color,
    val gold: Color,
    val green: Color,
    val red: Color,
    val iconGrey: Color,
    val legend: Color,
    val isDark: Boolean,
)

private fun hex(s: String): Color = Color(("ff" + s.removePrefix("#")).toLong(16))

val DarkNwColors = NwColors(
    bg = hex("#0B0B0B"), card = hex("#141414"), cardBorder = hex("#242424"), hair = hex("#202020"),
    text = hex("#FFFFFF"), text2 = hex("#B1B1B1"), text3 = hex("#8A8A8A"), muted = hex("#6E6E6E"),
    inputBg = hex("#0E0E0E"), inputBorder = hex("#2E2E2E"), chipBorder = hex("#2A2A2A"),
    track = hex("#202020"), grid = hex("#1E1E1E"), ctaBg = hex("#FFFFFF"), ctaTx = hex("#000000"),
    topbar = hex("#0B0B0B").copy(alpha = 0.86f), topBorder = hex("#1C1C1C"),
    gold = hex("#D5B475"), green = hex("#8BF1A7"), red = hex("#FE817B"),
    iconGrey = hex("#5E5E5E"), legend = hex("#CFCFCF"), isDark = true,
)

val LightNwColors = NwColors(
    bg = hex("#F1F1EE"), card = hex("#FFFFFF"), cardBorder = hex("#E8E8E4"), hair = hex("#EDEDEA"),
    text = hex("#101010"), text2 = hex("#4B4B4B"), text3 = hex("#7E7E7E"), muted = hex("#9A9A9A"),
    inputBg = hex("#F7F7F5"), inputBorder = hex("#DCDCD8"), chipBorder = hex("#DCDCD8"),
    track = hex("#E8E8E4"), grid = hex("#EAEAE6"), ctaBg = hex("#101010"), ctaTx = hex("#FFFFFF"),
    topbar = hex("#F1F1EE").copy(alpha = 0.9f), topBorder = hex("#E5E5E1"),
    gold = hex("#8A6A2E"), green = hex("#178A3E"), red = hex("#C0392B"),
    iconGrey = hex("#A0A0A0"), legend = hex("#3A3A3A"), isDark = false,
)

val LocalNwColors = staticCompositionLocalOf { DarkNwColors }

/** Convenient accessor: `nwColors.text`, `nwColors.card`, ... */
val nwColors: NwColors
    @Composable @ReadOnlyComposable get() = LocalNwColors.current

/** Parse a "#RRGGBB" category/member color string into a Compose Color. */
fun hexToColor(s: String): Color = hex(s)

/** A faint fill of [color] — the web's tintFor() (rgba at 0.14 alpha). */
fun tintFor(color: Color, alpha: Float = 0.14f): Color = color.copy(alpha = alpha)

@Composable
fun NetworthTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkNwColors else LightNwColors
    val scheme = if (dark) {
        darkColorScheme(
            background = c.bg, surface = c.card, surfaceContainer = c.card,
            surfaceContainerHigh = c.card, surfaceContainerHighest = c.inputBg,
            onBackground = c.text, onSurface = c.text, primary = c.ctaBg, onPrimary = c.ctaTx,
            outline = c.cardBorder, scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            background = c.bg, surface = c.card, surfaceContainer = c.card,
            surfaceContainerHigh = c.card, surfaceContainerHighest = c.inputBg,
            onBackground = c.text, onSurface = c.text, primary = c.ctaBg, onPrimary = c.ctaTx,
            outline = c.cardBorder, scrim = Color.Black,
        )
    }
    CompositionLocalProvider(LocalNwColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
