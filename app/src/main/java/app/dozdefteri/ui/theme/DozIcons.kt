package app.dozdefteri.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MedicationLiquid
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sanitizer
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.dozdefteri.domain.model.DoseStatus
import app.dozdefteri.domain.model.MedicationForm

/**
 * Icon mapping (plan 1.5). Material Symbols "pill"-style shapes that are missing from the Material icon set
 * (tablet, capsule, cream tube) are drawn here as outlined 24 dp vectors.
 */
object DozIcons {

    val Tablet: ImageVector by lazy {
        outlined("Tablet") {
            // Round tablet seen at an angle, with a score line.
            moveTo(12f, 5f)
            curveTo(16.97f, 5f, 21f, 7.24f, 21f, 10f)
            verticalLineTo(14f)
            curveTo(21f, 16.76f, 16.97f, 19f, 12f, 19f)
            curveTo(7.03f, 19f, 3f, 16.76f, 3f, 14f)
            verticalLineTo(10f)
            curveTo(3f, 7.24f, 7.03f, 5f, 12f, 5f)
            close()
            moveTo(3f, 10f)
            curveTo(3f, 12.76f, 7.03f, 15f, 12f, 15f)
            curveTo(16.97f, 15f, 21f, 12.76f, 21f, 10f)
            moveTo(8f, 7.6f)
            lineTo(16f, 12.4f)
        }
    }

    val Capsule: ImageVector by lazy {
        outlined("Capsule") {
            // Capsule rotated 45°: rounded rectangle from (4,20) to (20,4) with a divider.
            moveTo(10.5f, 20.5f)
            lineTo(20.5f, 10.5f)
            curveTo(22.43f, 8.57f, 22.43f, 5.43f, 20.5f, 3.5f)
            curveTo(18.57f, 1.57f, 15.43f, 1.57f, 13.5f, 3.5f)
            lineTo(3.5f, 13.5f)
            curveTo(1.57f, 15.43f, 1.57f, 18.57f, 3.5f, 20.5f)
            curveTo(5.43f, 22.43f, 8.57f, 22.43f, 10.5f, 20.5f)
            close()
            moveTo(8.5f, 8.5f)
            lineTo(15.5f, 15.5f)
        }
    }

    val Tube: ImageVector by lazy {
        outlined("Tube") {
            // Cream tube: cap on top, crimped bottom.
            moveTo(10f, 2.5f)
            horizontalLineTo(14f)
            verticalLineTo(5.5f)
            horizontalLineTo(10f)
            close()
            moveTo(8f, 5.5f)
            horizontalLineTo(16f)
            lineTo(17f, 19.5f)
            horizontalLineTo(7f)
            close()
            moveTo(7f, 21.5f)
            horizontalLineTo(17f)
        }
    }

    fun forForm(form: MedicationForm): ImageVector = when (form) {
        MedicationForm.TABLET -> Tablet
        MedicationForm.CAPSULE -> Capsule
        MedicationForm.LIQUID -> Icons.Rounded.MedicationLiquid
        MedicationForm.DROPS -> Icons.Rounded.WaterDrop
        MedicationForm.INJECTION -> Icons.Rounded.Vaccines
        MedicationForm.INHALER -> Icons.Rounded.Sanitizer
        MedicationForm.CREAM -> Tube
        MedicationForm.OTHER -> Tablet
    }

    fun forStatus(status: DoseStatus): ImageVector = when (status) {
        DoseStatus.PENDING -> Icons.Rounded.RadioButtonUnchecked
        DoseStatus.TAKEN -> Icons.Rounded.Check
        DoseStatus.SNOOZED -> Icons.Rounded.Schedule
        DoseStatus.SKIPPED -> Icons.AutoMirrored.Rounded.Redo
        DoseStatus.MISSED -> Icons.Rounded.ErrorOutline
    }

    val Morning: ImageVector get() = Icons.Rounded.WbTwilight
    val Noon: ImageVector get() = Icons.Rounded.WbSunny
    val Evening: ImageVector get() = Icons.Rounded.Bedtime

    private fun outlined(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        ).build()
}
