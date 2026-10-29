package app.dozdefteri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.dozdefteri.domain.model.ThemeMode

/** App theme. Dynamic color is intentionally disabled for brand consistency. */
@Composable
fun DozDefteriTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = accentColorScheme(accentIndex, dark),
        typography = AppTypography,
        content = content
    )
}
