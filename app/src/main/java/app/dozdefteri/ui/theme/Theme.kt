package app.dozdefteri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.dozdefteri.domain.model.ThemeMode

/** Extra in-app scale applied on top of the system font scale when "Large text" is on. */
const val LARGE_TEXT_SCALE = 1.15f

/**
 * App theme built from the Figma tokens. Dynamic color is intentionally off.
 * [largeText] multiplies the font scale by [LARGE_TEXT_SCALE] and raises primary buttons to 64 dp.
 */
@Composable
fun DozDefteriTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    largeText: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkDozColors else LightDozColors
    val textStyles = DozTextStyles()
    val density = LocalDensity.current
    val scaledDensity = if (largeText) {
        Density(density.density, density.fontScale * LARGE_TEXT_SCALE)
    } else {
        density
    }
    CompositionLocalProvider(
        LocalDozColors provides colors,
        LocalDozTextStyles provides textStyles,
        LocalTouch provides if (largeText) Touch(primary = 64.dp) else Touch(),
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = textStyles.toTypography(),
            shapes = AppShapes,
            content = content
        )
    }
}

/** Shortcuts: `Doz.colors`, `Doz.type`, `Doz.touch`. */
object Doz {
    val colors: DozColors
        @Composable @ReadOnlyComposable
        get() = LocalDozColors.current

    val type: DozTextStyles
        @Composable @ReadOnlyComposable
        get() = LocalDozTextStyles.current

    val touch: Touch
        @Composable @ReadOnlyComposable
        get() = LocalTouch.current
}
