@file:OptIn(ExperimentalMaterial3Api::class)

package app.dozdefteri.ui.shell

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.core.format.Formats
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.ui.components.ProfileAvatar
import app.dozdefteri.ui.components.TextAction
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import java.time.LocalTime

/** "Kimin ilaçları?" bottom sheet (screen 7b). */
@Composable
fun ProfileSwitcherSheet(
    summaries: List<ProfileSummary>,
    activeId: Long?,
    onSelect: (ProfileEntity) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = DozShapes.sheet,
        containerColor = Doz.colors.surfaceDefault
    ) {
        ProfileSwitcherContent(summaries, activeId, onSelect, onManage)
    }
}

@Composable
fun ProfileSwitcherContent(
    summaries: List<ProfileSummary>,
    activeId: Long?,
    onSelect: (ProfileEntity) -> Unit,
    onManage: () -> Unit
) {
    val c = Doz.colors
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.s16, vertical = Space.s8),
        verticalArrangement = Arrangement.spacedBy(Space.s4)
    ) {
        Text(
            stringResource(R.string.switcher_title),
            style = Doz.type.titleLarge,
            color = c.textPrimary,
            modifier = Modifier
                .padding(bottom = Space.s8)
                .semantics { heading() }
        )
        summaries.forEach { summary ->
            val active = summary.profile.id == activeId
            val detail = when {
                summary.nextTime != null -> pluralStringResource(
                    R.plurals.switcher_next,
                    summary.nextCount,
                    Formats.time(summary.nextTime, is24Hour),
                    summary.nextCount
                )
                summary.allDone -> stringResource(R.string.today_all_done)
                else -> stringResource(R.string.switcher_no_doses)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s16),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .background(if (active) c.primaryContainer else c.surfaceDefault, DozShapes.card)
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(summary.profile) }
                    .padding(horizontal = Space.s16, vertical = Space.s12)
            ) {
                ProfileAvatar(summary.profile.name, summary.profile.colorIndex, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(summary.profile.name, style = Doz.type.titleMedium, color = c.textPrimary)
                    Text(detail, style = Doz.type.bodyMedium, color = c.textSecondary)
                }
                if (active) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.primary)
            }
        }
        TextAction(
            stringResource(R.string.switcher_manage),
            onClick = onManage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = Space.s8)
        )
    }
}

@DozPreviews
@Composable
private fun SwitcherPreview() {
    val profiles = listOf(
        ProfileSummary(ProfileEntity(1, "Ayşe", 0, 0), LocalTime.of(20, 0), 2, false),
        ProfileSummary(ProfileEntity(2, "Mehmet", 1, 0), LocalTime.of(21, 0), 1, false),
        ProfileSummary(ProfileEntity(3, "Zeynep", 2, 0), null, 0, true)
    )
    DozDefteriTheme { ProfileSwitcherContent(profiles, 1, {}, {}) }
}
