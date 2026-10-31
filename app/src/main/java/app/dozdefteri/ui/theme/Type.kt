package app.dozdefteri.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Roboto = FontFamily.Default

private fun style(size: Int, line: Int, weight: FontWeight, tabular: Boolean = false) = TextStyle(
    fontFamily = Roboto,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    fontFeatureSettings = if (tabular) "tnum" else null
)

/** Text styles from the Figma text styles (size / line height). */
@Immutable
data class DozTextStyles(
    val display: TextStyle = style(36, 44, FontWeight.Medium),
    val headline: TextStyle = style(28, 36, FontWeight.Medium),
    val titleLarge: TextStyle = style(22, 28, FontWeight.Medium),
    val titleMedium: TextStyle = style(18, 24, FontWeight.Medium),
    val bodyLarge: TextStyle = style(18, 26, FontWeight.Normal),
    val bodyMedium: TextStyle = style(16, 24, FontWeight.Normal),
    val labelLarge: TextStyle = style(16, 20, FontWeight.Medium),
    val labelMedium: TextStyle = style(14, 20, FontWeight.Medium),
    val caption: TextStyle = style(13, 18, FontWeight.Normal),
    /** Large clock values (widget, time picker) with tabular digits. */
    val numericTime: TextStyle = style(24, 28, FontWeight.Medium, tabular = true),
    /** Dose time inside cards ("08:00", "08:04'te"): Body/Medium, medium weight, tabular digits. */
    val doseTime: TextStyle = style(16, 24, FontWeight.Medium, tabular = true)
)

val LocalDozTextStyles = staticCompositionLocalOf { DozTextStyles() }

/** Material typography mapped onto the design styles so stock components use them too. */
fun DozTextStyles.toTypography() = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = titleLarge,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = labelLarge,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = caption,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = caption
)
