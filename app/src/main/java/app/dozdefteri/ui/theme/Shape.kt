package app.dozdefteri.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Dimensions from the Figma "Dimension" variable collection.

object Space {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp
}

object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 28.dp
    val full = 999.dp
}

/** Touch target sizes. "Large text" mode raises the primary height to 64 dp (plan section 5). */
@Immutable
data class Touch(val min: Dp = 48.dp, val primary: Dp = 56.dp)

val LocalTouch = staticCompositionLocalOf { Touch() }

object DozShapes {
    val card = RoundedCornerShape(Radius.lg)
    val field = RoundedCornerShape(Radius.md)
    val small = RoundedCornerShape(Radius.sm)
    val sheet = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl)
    val pill = RoundedCornerShape(Radius.full)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl)
)
