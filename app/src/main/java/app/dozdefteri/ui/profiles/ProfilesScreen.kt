@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.dozdefteri.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dozdefteri.R
import app.dozdefteri.data.db.entity.ProfileEntity
import app.dozdefteri.ui.components.ConfirmDialog
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.ProfileColors

data class ProfilesActions(
    val onRequestAdd: () -> Boolean = { true },
    val onAdd: (String, Int) -> Unit = { _, _ -> },
    val onUpdate: (ProfileEntity, String, Int) -> Unit = { _, _, _ -> },
    val onSetActive: (Long) -> Unit = {},
    val onDelete: (ProfileEntity) -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun ProfilesScreen(onBack: () -> Unit, onOpenPro: () -> Unit, viewModel: ProfilesViewModel = hiltViewModel()) {
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
            onAdd = viewModel::add,
            onUpdate = viewModel::update,
            onSetActive = viewModel::setActive,
            onDelete = viewModel::delete,
            onBack = onBack
        )
    )
}

@Composable
fun ProfilesContent(state: ProfilesUiState, actions: ProfilesActions) {
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (actions.onRequestAdd()) adding = true },
                icon = { Icon(if (state.canAdd) Icons.Filled.Add else Icons.Filled.Lock, contentDescription = null) },
                text = { Text(stringResource(R.string.profile_add)) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(state.profiles, key = { it.id }) { profile ->
                ProfileRow(
                    profile = profile,
                    active = profile.id == state.activeId,
                    canDelete = state.canDelete,
                    onSelect = { actions.onSetActive(profile.id) },
                    onEdit = { editing = profile.id },
                    onDelete = { deleting = profile.id }
                )
                HorizontalDivider()
            }
            if (!state.isPro) {
                item {
                    Text(
                        stringResource(R.string.profiles_pro_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (adding) {
        ProfileDialog(
            title = stringResource(R.string.profile_add),
            initialName = "",
            initialColor = state.profiles.size % ProfileColors.size,
            onConfirm = { name, color ->
                adding = false
                actions.onAdd(name, color)
            },
            onDismiss = { adding = false }
        )
    }
    state.profiles.firstOrNull { it.id == editing }?.let { profile ->
        ProfileDialog(
            title = stringResource(R.string.profile_edit),
            initialName = profile.name,
            initialColor = profile.colorIndex,
            onConfirm = { name, color ->
                editing = null
                actions.onUpdate(profile, name, color)
            },
            onDismiss = { editing = null }
        )
    }
    state.profiles.firstOrNull { it.id == deleting }?.let { profile ->
        ConfirmDialog(
            title = stringResource(R.string.profile_delete_title, profile.name),
            text = stringResource(R.string.profile_delete_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                deleting = null
                actions.onDelete(profile)
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileEntity,
    active: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = active, onClick = onSelect, role = Role.RadioButton)
            .padding(start = 16.dp, end = 4.dp)
    ) {
        ColorDot(ProfileColors[profile.colorIndex.mod(ProfileColors.size)])
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
            if (active) {
                Text(
                    stringResource(R.string.profile_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.profile_edit_named, profile.name))
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.profile_delete_named, profile.name))
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean = false, size: Int = 24) {
    Box(
        Modifier
            .size(size.dp)
            .background(color, CircleShape)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ProfileDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var color by rememberSaveable { mutableIntStateOf(initialColor) }
    val trimmed = name.trim()
    val valid = trimmed.isNotEmpty() && trimmed.length <= ProfilesViewModel.NAME_MAX
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                    isError = trimmed.length > ProfilesViewModel.NAME_MAX,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.profile_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileColors.forEachIndexed { index, swatch ->
                        val description = stringResource(R.string.profile_color_option, index + 1)
                        Box(
                            Modifier
                                .size(48.dp)
                                .clickable(role = Role.RadioButton) { color = index }
                                .semantics { contentDescription = description },
                            contentAlignment = Alignment.Center
                        ) {
                            ColorDot(swatch, selected = index == color, size = 36)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed, color) }, enabled = valid) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun ProfilesPreview() {
    DozDefteriTheme {
        ProfilesContent(
            state = ProfilesUiState(
                profiles = listOf(
                    ProfileEntity(id = 1, name = "Me", colorIndex = 0, createdAt = 0),
                    ProfileEntity(id = 2, name = "Mom", colorIndex = 3, createdAt = 0)
                ),
                activeId = 1,
                isPro = true
            ),
            actions = ProfilesActions()
        )
    }
}
