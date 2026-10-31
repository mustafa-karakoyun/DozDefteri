package app.dozdefteri.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/** Small label above a form field ("İlaç adı", "Miktar"). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Doz.type.labelMedium,
        color = Doz.colors.textSecondary,
        modifier = modifier.padding(bottom = Space.s8)
    )
}

@Composable
fun dozFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Doz.colors.primary,
    unfocusedBorderColor = Doz.colors.borderStrong,
    errorBorderColor = Doz.colors.danger.fg,
    focusedContainerColor = Doz.colors.surfaceDefault,
    unfocusedContainerColor = Doz.colors.surfaceDefault,
    errorContainerColor = Doz.colors.surfaceDefault,
    errorSupportingTextColor = Doz.colors.danger.fg
)

/** Outlined text field in the design's style with an optional error line under it. */
@Composable
fun DozTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = Doz.type.bodyLarge,
        singleLine = singleLine,
        isError = error != null,
        placeholder = textSlot(placeholder, isCaption = false),
        supportingText = textSlot(error, isCaption = true),
        keyboardOptions = if (numeric) keyboardOptions.copy(keyboardType = KeyboardType.Decimal) else keyboardOptions,
        shape = DozShapes.field,
        colors = dozFieldColors()
    )
}

private fun textSlot(text: String?, isCaption: Boolean): (@Composable () -> Unit)? =
    if (text == null) {
        null
    } else {
        { Text(text, style = if (isCaption) Doz.type.caption else Doz.type.bodyLarge) }
    }

/** Read-only field that opens a menu of [options] ("Birim"). */
@Composable
fun <T> DozDropdownField(
    selected: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = Doz.type.bodyLarge,
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null) },
            shape = DozShapes.field,
            colors = dozFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.DropdownList) { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option), style = Doz.type.bodyLarge) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

/** Single-select chip (form chips, time chips, day chips). Selected = `primary.container` + 2 dp primary border. */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showCheck: Boolean = false,
    role: Role = Role.RadioButton
) {
    val c = Doz.colors
    Surface(
        shape = DozShapes.field,
        color = if (selected) c.primaryContainer else c.surfaceDefault,
        contentColor = if (selected) c.onPrimaryContainer else c.textPrimary,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.borderStrong),
        modifier = modifier
            .heightIn(min = Doz.touch.min)
            .selectable(selected = selected, role = role, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            modifier = Modifier.padding(horizontal = Space.s16, vertical = Space.s12)
        ) {
            if (showCheck && selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(text, style = Doz.type.labelLarge)
        }
    }
}

/** Chip with a dashed outline ("+ Başka saat"). */
@Composable
fun DashedChip(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = Doz.colors.borderStrong
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s8),
        modifier = modifier
            .heightIn(min = Doz.touch.min)
            .dashedBorder(color, 12.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.s16, vertical = Space.s12)
    ) {
        Icon(icon, contentDescription = null, tint = Doz.colors.primary, modifier = Modifier.size(20.dp))
        Text(text, style = Doz.type.labelLarge, color = Doz.colors.primary)
    }
}

fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.5.dp): Modifier = drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
    )
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(radius.toPx()))
}

/** Radio card with title and description ("Her gün / Günde bir veya birkaç kez"). */
@Composable
fun RadioCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val c = Doz.colors
    Surface(
        shape = DozShapes.field,
        color = if (selected) c.primaryContainer else c.surfaceDefault,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.borderDefault),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s12),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                    .padding(horizontal = Space.s12, vertical = Space.s12)
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = c.primary, unselectedColor = c.borderStrong)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = Doz.type.titleMedium,
                        color = if (selected) c.onPrimaryContainer else c.textPrimary
                    )
                    Text(
                        description,
                        style = Doz.type.bodyMedium,
                        color = if (selected) c.onPrimaryContainer else c.textSecondary
                    )
                }
            }
            AnimatedVisibility(visible = selected) {
                Column(Modifier.padding(start = Space.s16, end = Space.s16, bottom = Space.s16), content = content)
            }
        }
    }
}

/** Collapsible row that shows a summary subtitle while closed ("Süre — Bugün başlar · 30 gün"). */
@Composable
fun CollapsibleSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Doz.colors
    val state = subtitle
    Surface(
        shape = DozShapes.field,
        color = c.surfaceDefault,
        border = BorderStroke(1.dp, c.borderDefault),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = state }
                    .padding(horizontal = Space.s16, vertical = Space.s12)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = Doz.type.titleMedium, color = c.textPrimary)
                    Text(subtitle, style = Doz.type.bodyMedium, color = c.textSecondary)
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = c.textSecondary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(start = Space.s16, end = Space.s16, bottom = Space.s16),
                    verticalArrangement = Arrangement.spacedBy(Space.s12),
                    content = content
                )
            }
        }
    }
}
