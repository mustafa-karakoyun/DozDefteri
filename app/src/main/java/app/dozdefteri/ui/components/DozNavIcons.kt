package app.dozdefteri.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import app.dozdefteri.ui.theme.DozIcons

/** Bottom navigation icons as in the design (house, capsule, calendar, sliders). */
object DozNavIcons {
    val Today: ImageVector get() = Icons.Outlined.Home
    val Medications: ImageVector get() = DozIcons.Capsule
    val History: ImageVector get() = Icons.Outlined.CalendarToday
    val Settings: ImageVector get() = Icons.Outlined.Tune
}
