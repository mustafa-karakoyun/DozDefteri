package app.dozdefteri.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A background/foreground pair used by chips and tinted boxes. */
@Immutable
data class ColorPair(val bg: Color, val fg: Color)

/** Semantic color tokens from the Figma "Color" collection (`light/…` and `dark/…`). */
@Immutable
data class DozColors(
    val bgApp: Color,
    val surfaceDefault: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceInverse: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val textInverse: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val taken: ColorPair,
    val skipped: ColorPair,
    val snoozed: ColorPair,
    /** Missed doses use earthy clay, deliberately not the alarming red of [danger]. */
    val missed: ColorPair,
    val pendingFg: Color,
    val warning: ColorPair,
    val danger: ColorPair,
    val pro: ColorPair,
    val isDark: Boolean
)

val LightDozColors = DozColors(
    bgApp = Sand.c25,
    surfaceDefault = Sand.c0,
    surfaceContainer = Sand.c50,
    surfaceContainerHigh = Sand.c100,
    surfaceInverse = Sand.c900,
    textPrimary = Sand.c900,
    textSecondary = Sand.c600,
    textOnPrimary = Sand.c0,
    textInverse = Sand.c50,
    primary = Teal.c600,
    primaryContainer = Teal.c50,
    onPrimaryContainer = Teal.c800,
    borderDefault = Sand.c200,
    borderStrong = Sand.c400,
    taken = ColorPair(Teal.c50, Teal.c700),
    skipped = ColorPair(Slate.c100, Slate.c700),
    snoozed = ColorPair(Amber.c100, Amber.c700),
    missed = ColorPair(Clay.c100, Clay.c700),
    pendingFg = Sand.c600,
    warning = ColorPair(Amber.c100, Amber.c700),
    danger = ColorPair(Red.c100, Red.c700),
    pro = ColorPair(Violet.c100, Violet.c700),
    isDark = false
)

val DarkDozColors = DozColors(
    bgApp = Sand.c950,
    surfaceDefault = Sand.c900,
    surfaceContainer = Sand.c850,
    surfaceContainerHigh = Sand.c800,
    surfaceInverse = Sand.c50,
    textPrimary = Sand.c50,
    textSecondary = Sand.c300,
    textOnPrimary = Teal.c900,
    textInverse = Sand.c900,
    primary = Teal.c200,
    primaryContainer = Teal.c800,
    onPrimaryContainer = Teal.c100,
    borderDefault = Sand.c700,
    borderStrong = Sand.c500,
    taken = ColorPair(Teal.c800, Teal.c100),
    skipped = ColorPair(Slate.c900, Slate.c200),
    snoozed = ColorPair(Amber.c900, Amber.c200),
    missed = ColorPair(Clay.c900, Clay.c200),
    pendingFg = Sand.c300,
    warning = ColorPair(Amber.c900, Amber.c200),
    danger = ColorPair(Red.c900, Red.c300),
    pro = ColorPair(Violet.c900, Violet.c200),
    isDark = true
)

val LocalDozColors = staticCompositionLocalOf { LightDozColors }

/** Profile colors (mode independent). Index 0 (blue) is free; the others are the Pro "profile colors". */
@Immutable
data class ProfileColor(val color: Color, val on: Color)

val ProfilePalette = listOf(
    ProfileColor(Color(0xFF1F5FA8), Color.White),
    ProfileColor(Color(0xFFE69F00), Color(0xFF1C1A17)),
    ProfileColor(Color(0xFF00805C), Color.White),
    ProfileColor(Color(0xFFB4508E), Color.White),
    ProfileColor(Color(0xFF56B4E9), Color(0xFF1C1A17)),
    ProfileColor(Color(0xFFC24E00), Color.White),
    ProfileColor(Color(0xFF6A4C93), Color.White),
    ProfileColor(Color(0xFFE8D44D), Color(0xFF1C1A17))
)

fun profileColorOf(index: Int): ProfileColor = ProfilePalette[index.mod(ProfilePalette.size)]

/** Material scheme derived from the tokens so stock Material components match the design. */
fun DozColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = textOnPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = textOnPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        background = bgApp,
        onBackground = textPrimary,
        surface = bgApp,
        onSurface = textPrimary,
        surfaceVariant = surfaceContainerHigh,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surfaceDefault,
        surfaceContainerLow = surfaceDefault,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHigh,
        surfaceBright = surfaceDefault,
        surfaceDim = surfaceContainer,
        inverseSurface = surfaceInverse,
        inverseOnSurface = textInverse,
        inversePrimary = if (isDark) Teal.c600 else Teal.c200,
        outline = borderStrong,
        outlineVariant = borderDefault,
        error = danger.fg,
        onError = if (isDark) Red.c900 else Sand.c0,
        errorContainer = danger.bg,
        onErrorContainer = danger.fg,
        scrim = Color.Black
    )
}
