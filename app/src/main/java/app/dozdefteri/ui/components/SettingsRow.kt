package app.dozdefteri.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space

/** What a [SettingsRow] shows at its end. */
sealed interface RowTrailing {
    data object Chevron : RowTrailing
    data object Check : RowTrailing
    data class Toggle(val checked: Boolean, val onChange: (Boolean) -> Unit) : RowTrailing
    data class Action(val label: String, val onClick: () -> Unit) : RowTrailing
    data object ProBadge : RowTrailing
    data object None : RowTrailing
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Doz.colors.textSecondary,
    trailing: RowTrailing = RowTrailing.Chevron,
    showChevronAfterBadge: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val c = Doz.colors
    val clickModifier = when {
        trailing is RowTrailing.Toggle ->
            Modifier.clickable(role = Role.Switch) { trailing.onChange(!trailing.checked) }
        onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
        else -> Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s16),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(clickModifier)
            .padding(horizontal = Space.s16, vertical = Space.s12)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = iconTint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            Text(title, style = Doz.type.bodyLarge, color = c.textPrimary)
            if (subtitle != null) Text(subtitle, style = Doz.type.bodyMedium, color = c.textSecondary)
        }
        when (trailing) {
            RowTrailing.Chevron ->
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
            RowTrailing.Check -> Icon(Icons.Rounded.Check, contentDescription = null, tint = c.primary)
            is RowTrailing.Toggle -> Switch(
                checked = trailing.checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.textOnPrimary)
            )
            is RowTrailing.Action -> TonalButton(trailing.label, onClick = trailing.onClick)
            RowTrailing.ProBadge -> {
                ProBadge()
                if (showChevronAfterBadge) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = c.textSecondary
                    )
                }
            }
            RowTrailing.None -> Unit
        }
    }
}

@DozPreviews
@Composable
private fun SettingsRowPreview() {
    DozDefteriTheme {
        Column {
            SettingsRow("Dil", subtitle = "Türkçe", onClick = {})
            SettingsRow(
                "Büyük yazı",
                subtitle = "Metinler ve butonlar büyür",
                trailing = RowTrailing.Toggle(true) {}
            )
            SettingsRow("Profiller", subtitle = "Birden fazla kişinin ilaçları", trailing = RowTrailing.ProBadge)
        }
    }
}
