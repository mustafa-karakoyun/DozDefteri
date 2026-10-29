package app.dozdefteri.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val StatusTaken = Color(0xFF15803D)
val StatusMissed = Color(0xFFB91C1C)
val StatusPartial = Color(0xFFC2410C)

/** Profile color palette, indexed by `ProfileEntity.colorIndex` (0..7). */
val ProfileColors = listOf(
    Color(0xFF0E7C86),
    Color(0xFF5B5BD6),
    Color(0xFFC2410C),
    Color(0xFF15803D),
    Color(0xFFB91C1C),
    Color(0xFFA16207),
    Color(0xFF7E22CE),
    Color(0xFF475569)
)

fun profileColor(index: Int): Color = ProfileColors[index.mod(ProfileColors.size)]

/** Seed colors of the selectable accents; index 0 is free, 1..3 are Pro. */
val AccentSeeds = listOf(
    Color(0xFF0E7C86),
    Color(0xFF5B5BD6),
    Color(0xFFC2410C),
    Color(0xFF15803D)
)

const val ACCENT_COUNT = 4

private data class AccentTones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color
)

// Hand-tuned tonal values following the Material Theme Builder approach (tone 40/100/90/10 light, 80/20/30/90 dark).
private val lightTones = listOf(
    AccentTones(
        Color(0xFF0E7C86), Color(0xFFFFFFFF), Color(0xFFA2EEF5), Color(0xFF002023),
        Color(0xFF4A6365), Color(0xFFCCE8EA), Color(0xFF051F21),
        Color(0xFF4D5F7C), Color(0xFFD5E3FF), Color(0xFF071C36)
    ),
    AccentTones(
        Color(0xFF5B5BD6), Color(0xFFFFFFFF), Color(0xFFE1DFFF), Color(0xFF13105E),
        Color(0xFF5D5C72), Color(0xFFE2E0F9), Color(0xFF1A1A2C),
        Color(0xFF79536A), Color(0xFFFFD8EC), Color(0xFF2E1125)
    ),
    AccentTones(
        Color(0xFFC2410C), Color(0xFFFFFFFF), Color(0xFFFFDBCF), Color(0xFF390C00),
        Color(0xFF77574C), Color(0xFFFFDBCF), Color(0xFF2C160D),
        Color(0xFF6A5F2F), Color(0xFFF3E3A7), Color(0xFF221B00)
    ),
    AccentTones(
        Color(0xFF15803D), Color(0xFFFFFFFF), Color(0xFFA6F4B5), Color(0xFF00210B),
        Color(0xFF506352), Color(0xFFD2E8D2), Color(0xFF0E1F12),
        Color(0xFF3A646F), Color(0xFFBEEAF6), Color(0xFF001F26)
    )
)

private val darkTones = listOf(
    AccentTones(
        Color(0xFF85D2DA), Color(0xFF00363B), Color(0xFF004F55), Color(0xFFA2EEF5),
        Color(0xFFB1CBCE), Color(0xFF324B4D), Color(0xFFCCE8EA),
        Color(0xFFB5C7E9), Color(0xFF354763), Color(0xFFD5E3FF)
    ),
    AccentTones(
        Color(0xFFC2C1FF), Color(0xFF26248F), Color(0xFF4342BD), Color(0xFFE1DFFF),
        Color(0xFFC6C4DD), Color(0xFF454559), Color(0xFFE2E0F9),
        Color(0xFFE9B9D4), Color(0xFF5F3C52), Color(0xFFFFD8EC)
    ),
    AccentTones(
        Color(0xFFFFB59C), Color(0xFF5C1900), Color(0xFF842B00), Color(0xFFFFDBCF),
        Color(0xFFE7BDB0), Color(0xFF5D4036), Color(0xFFFFDBCF),
        Color(0xFFD6C78D), Color(0xFF51471A), Color(0xFFF3E3A7)
    ),
    AccentTones(
        Color(0xFF8AD79A), Color(0xFF003918), Color(0xFF005226), Color(0xFFA6F4B5),
        Color(0xFFB6CCB7), Color(0xFF384B3B), Color(0xFFD2E8D2),
        Color(0xFFA2CEDA), Color(0xFF204C56), Color(0xFFBEEAF6)
    )
)

fun accentColorScheme(accentIndex: Int, dark: Boolean): ColorScheme {
    val index = accentIndex.coerceIn(0, ACCENT_COUNT - 1)
    return if (dark) {
        val t = darkTones[index]
        darkColorScheme(
            primary = t.primary,
            onPrimary = t.onPrimary,
            primaryContainer = t.primaryContainer,
            onPrimaryContainer = t.onPrimaryContainer,
            secondary = t.secondary,
            secondaryContainer = t.secondaryContainer,
            onSecondaryContainer = t.onSecondaryContainer,
            tertiary = t.tertiary,
            tertiaryContainer = t.tertiaryContainer,
            onTertiaryContainer = t.onTertiaryContainer,
            background = Color(0xFF0F1415),
            surface = Color(0xFF0F1415),
            onBackground = Color(0xFFDEE3E4),
            onSurface = Color(0xFFDEE3E4),
            surfaceVariant = Color(0xFF3F484A),
            onSurfaceVariant = Color(0xFFBFC8CA)
        )
    } else {
        val t = lightTones[index]
        lightColorScheme(
            primary = t.primary,
            onPrimary = t.onPrimary,
            primaryContainer = t.primaryContainer,
            onPrimaryContainer = t.onPrimaryContainer,
            secondary = t.secondary,
            secondaryContainer = t.secondaryContainer,
            onSecondaryContainer = t.onSecondaryContainer,
            tertiary = t.tertiary,
            tertiaryContainer = t.tertiaryContainer,
            onTertiaryContainer = t.onTertiaryContainer,
            background = Color(0xFFFAFDFD),
            surface = Color(0xFFFAFDFD),
            onBackground = Color(0xFF191C1D),
            onSurface = Color(0xFF191C1D),
            surfaceVariant = Color(0xFFDBE4E6),
            onSurfaceVariant = Color(0xFF3F484A)
        )
    }
}
