package app.dozdefteri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import app.dozdefteri.ui.theme.profileColorOf
import java.util.Locale

/** Round avatar with the profile's first letter on its profile color. */
@Composable
fun ProfileAvatar(name: String, colorIndex: Int, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color = profileColorOf(colorIndex)
    Box(
        modifier = modifier
            .size(size)
            .background(color.color, DozShapes.pill),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase(Locale.getDefault()),
            style = if (size >= 48.dp) Doz.type.titleLarge else Doz.type.titleMedium,
            color = color.on
        )
    }
}

/** Active profile shown on every main screen: avatar + name + ▾; tapping opens the profile switcher. */
@Composable
fun ProfileChip(name: String, colorIndex: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.profile_chip_a11y, name)
    Surface(
        onClick = onClick,
        shape = DozShapes.pill,
        color = Doz.colors.surfaceContainerHigh,
        contentColor = Doz.colors.textPrimary,
        modifier = modifier
            .heightIn(min = Doz.touch.min)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            modifier = Modifier
                .padding(start = Space.s4, end = Space.s12, top = Space.s4, bottom = Space.s4)
                .clearAndSetSemantics { }
        ) {
            ProfileAvatar(name, colorIndex)
            Text(name, style = Doz.type.labelLarge)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@DozPreviews
@Composable
private fun ProfileChipPreview() {
    DozDefteriTheme { ProfileChip("Ayşe", 0, {}) }
}
