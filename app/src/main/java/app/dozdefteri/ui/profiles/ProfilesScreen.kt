@file:OptIn(ExperimentalLayoutApi::class)

package app.dozdefteri.ui.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.ui.components.ConfirmDialog
import app.dozdefteri.ui.components.DozTextField
import app.dozdefteri.ui.components.DozTopBar
import app.dozdefteri.ui.components.FieldLabel
import app.dozdefteri.ui.components.OutlinedPillButton
import app.dozdefteri.ui.components.PrimaryButton
import app.dozdefteri.ui.components.ProBadge
import app.dozdefteri.ui.components.ProfileAvatar
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.ProfilePalette
import app.dozdefteri.ui.theme.Space
import java.util.Locale

data class ProfilesActions(
    val onRequestAdd: () -> Boolean = { true },
    val onRequestColor: (Int) -> Boolean = { true },
    val onSave: (ProfileEntity?, String, String, Int) -> Unit = { _, _, _, _ -> },
    val onSetActive: (Long) -> Unit = {},
    val onDelete: (ProfileEntity) -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun ProfilesScreen(
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProfilesEvent.OpenPro -> onOpenPro()
            }
        }
    }
    ProfilesContent(
        state = state,
        actions = ProfilesActions(
            onRequestAdd = viewModel::requestAdd,
            onRequestColor = viewModel::requestColor,
            onSave = viewModel::save,
            onSetActive = viewModel::setActive,
            onDelete = viewModel::delete,
            onBack = onBack
        )
    )
}

/** Which form is open under the list: none, a new profile, or editing [ProfileEntity.id]. */
private const val FORM_NONE = -1L
private const val FORM_NEW = 0L

@Composable
fun ProfilesContent(state: ProfilesUiState, actions: ProfilesActions) {
    var form by rememberSaveable { mutableStateOf(FORM_NONE) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = state.profiles.firstOrNull { it.id == form }

    Scaffold(
        containerColor = Doz.colors.bgApp,
        topBar = {
            DozTopBar(
                stringResource(R.string.profiles_title),
                onNavigate = actions.onBack,
                trailing = { ProBadge(Modifier.padding(end = Space.s12)) }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s16)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s12)
        ) {
            state.profiles.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    medicationCount = state.medicationCounts[profile.id] ?: 0,
                    active = profile.id == state.activeId,
                    onSelect = { actions.onSetActive(profile.id) },
                    onEdit = { form = profile.id }
                )
            }
            OutlinedPillButton(
                stringResource(R.string.profile_add_plus),
                onClick = { if (actions.onRequestAdd()) form = FORM_NEW },
                modifier = Modifier.fillMaxWidth()
            )
            if (form != FORM_NONE) {
                ProfileForm(
                    key = form,
                    existing = editing,
                    canDelete = editing != null && state.canDelete,
                    canUseColor = state::canUseColor,
                    onRequestColor = actions.onRequestColor,
                    onSave = { name, note, color ->
                        actions.onSave(editing, name, note, color)
                        form = FORM_NONE
                    },
                    onDelete = { deleting = editing?.id }
                )
            }
        }
    }

    state.profiles.firstOrNull { it.id == deleting }?.let { profile ->
        ConfirmDialog(
            title = stringResource(R.string.profile_delete_title, profile.name),
            text = stringResource(R.string.profile_delete_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                deleting = null
                form = FORM_NONE
                actions.onDelete(profile)
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileEntity,
    medicationCount: Int,
    active: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    val c = Doz.colors
    val count = pluralStringResource(R.plurals.medication_count, medicationCount, medicationCount)
    val subtitle = listOfNotNull(profile.note, count).joinToString(" · ")
    Surface(
        shape = DozShapes.card,
        color = c.surfaceDefault,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) c.primary else c.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s16),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .selectable(selected = active, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = Space.s16, vertical = Space.s12)
        ) {
            ProfileAvatar(profile.name, profile.colorIndex, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(profile.name, style = Doz.type.titleMedium, color = c.textPrimary)
                Text(subtitle, style = Doz.type.bodyMedium, color = c.textSecondary)
            }
            if (active) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s4),
                    modifier = Modifier
                        .background(c.taken.bg, DozShapes.pill)
                        .padding(horizontal = Space.s12, vertical = Space.s4)
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = c.taken.fg,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(stringResource(R.string.profile_active), style = Doz.type.labelMedium, color = c.taken.fg)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.profile_edit_named, profile.name),
                    tint = c.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ProfileForm(
    key: Long,
    existing: ProfileEntity?,
    canDelete: Boolean,
    canUseColor: (Int) -> Boolean,
    onRequestColor: (Int) -> Boolean,
    onSave: (String, String, Int) -> Unit,
    onDelete: () -> Unit
) {
    val c = Doz.colors
    var name by rememberSaveable(key) { mutableStateOf(existing?.name.orEmpty()) }
    var note by rememberSaveable(key) { mutableStateOf(existing?.note.orEmpty()) }
    var color by rememberSaveable(key) { mutableIntStateOf(existing?.colorIndex ?: 0) }
    val trimmed = name.trim()
    val nameError = if (trimmed.length > ProfilesViewModel.NAME_MAX) {
        stringResource(R.string.error_too_long, ProfilesViewModel.NAME_MAX)
    } else {
        null
    }
    Text(
        stringResource(if (existing == null) R.string.profile_new else R.string.profile_edit),
        style = Doz.type.titleMedium,
        color = c.textSecondary,
        modifier = Modifier
            .padding(top = Space.s12)
            .semantics { heading() }
    )
    Surface(
        shape = DozShapes.card,
        color = c.surfaceDefault,
        border = BorderStroke(1.dp, c.borderDefault),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            Column {
                FieldLabel(stringResource(R.string.profile_name))
                DozTextField(
                    value = name,
                    onValueChange = { name = it },
                    error = nameError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            }
            Column {
                FieldLabel(stringResource(R.string.profile_note))
                DozTextField(
                    value = note,
                    onValueChange = { if (it.length <= ProfilesViewModel.NOTE_MAX) note = it },
                    placeholder = stringResource(R.string.profile_note_placeholder),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            FieldLabel(stringResource(R.string.profile_color))
            val selectedRing = Modifier.border(3.dp, c.textPrimary, DozShapes.pill)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s8),
                verticalArrangement = Arrangement.spacedBy(Space.s8)
            ) {
                ProfilePalette.forEachIndexed { index, swatch ->
                    val locked = !canUseColor(index)
                    val description = stringResource(R.string.profile_color_option, index + 1) +
                        if (locked) ", " + stringResource(R.string.pro_required) else ""
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable(role = Role.RadioButton) { if (onRequestColor(index)) color = index }
                            .semantics { contentDescription = description }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(swatch.color, DozShapes.pill)
                                .then(if (index == color) selectedRing else Modifier)
                        ) {
                            when {
                                index == color -> Icon(Icons.Rounded.Check, contentDescription = null, tint = swatch.on)
                                locked -> Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = swatch.on,
                                    modifier = Modifier.size(16.dp)
                                )
                                else -> Text(
                                    trimmed.take(1).uppercase(Locale.getDefault()).ifEmpty { "A" },
                                    style = Doz.type.labelLarge,
                                    color = swatch.on
                                )
                            }
                        }
                    }
                }
            }
            PrimaryButton(
                stringResource(R.string.profile_save),
                onClick = { onSave(trimmed, note, color) },
                enabled = trimmed.isNotEmpty() && nameError == null,
                modifier = Modifier.fillMaxWidth()
            )
            if (canDelete) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.profile_delete), style = Doz.type.labelLarge, color = c.danger.fg)
                }
            }
        }
    }
}

@DozPreviews
@Composable
private fun ProfilesPreview() {
    DozDefteriTheme {
        ProfilesContent(
            state = ProfilesUiState(
                profiles = listOf(
                    ProfileEntity(
                        id = 1,
                        name = "Ayşe",
                        colorIndex = 0,
                        createdAt = 0,
                        note = "Bu telefonun sahibi"
                    ),
                    ProfileEntity(id = 2, name = "Mehmet", colorIndex = 1, createdAt = 0, note = "Babam"),
                    ProfileEntity(id = 3, name = "Zeynep", colorIndex = 2, createdAt = 0, note = "Takviyeler")
                ),
                medicationCounts = mapOf(1L to 5, 2L to 3, 3L to 2),
                activeId = 1,
                isPro = true
            ),
            actions = ProfilesActions()
        )
    }
}
