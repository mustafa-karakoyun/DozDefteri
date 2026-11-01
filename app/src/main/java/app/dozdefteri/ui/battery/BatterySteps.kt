@file:OptIn(ExperimentalLayoutApi::class)

package app.dozdefteri.ui.battery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.dozdefteri.R
import app.dozdefteri.ui.components.SelectChip
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space
import java.util.Locale

/** Manufacturer families with their own battery-saving settings; OTHER covers everything else. */
enum class Manufacturer(val brands: Set<String>, val label: Int, val steps: List<Int>, val details: Int) {
    XIAOMI(
        setOf("xiaomi", "redmi", "poco"),
        R.string.brand_xiaomi,
        listOf(R.string.battery_step_xiaomi_1, R.string.battery_step_xiaomi_2, R.string.battery_step_xiaomi_3),
        R.string.battery_steps_xiaomi
    ),
    SAMSUNG(
        setOf("samsung"),
        R.string.brand_samsung,
        listOf(R.string.battery_step_samsung_1, R.string.battery_step_samsung_2, R.string.battery_step_samsung_3),
        R.string.battery_steps_samsung
    ),
    HUAWEI(
        setOf("huawei", "honor"),
        R.string.brand_huawei,
        listOf(R.string.battery_step_huawei_1, R.string.battery_step_huawei_2, R.string.battery_step_huawei_3),
        R.string.battery_steps_huawei
    ),
    OPPO(
        setOf("oppo", "realme", "oneplus"),
        R.string.brand_oppo,
        listOf(R.string.battery_step_oppo_1, R.string.battery_step_oppo_2, R.string.battery_step_oppo_3),
        R.string.battery_steps_oppo
    ),
    OTHER(
        emptySet(),
        R.string.brand_other,
        listOf(R.string.battery_step_other_1, R.string.battery_step_other_2, R.string.battery_step_other_3),
        R.string.battery_steps_generic
    );

    companion object {
        fun from(manufacturer: String): Manufacturer {
            val value = manufacturer.lowercase(Locale.ROOT)
            return entries.firstOrNull { it.brands.any { brand -> value.contains(brand) } } ?: OTHER
        }
    }
}

/** Brand chips plus a three-step visual guide for the selected brand (screen 1c). */
@Composable
fun BatterySteps(selected: Manufacturer, onSelect: (Manufacturer) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s12)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s8)
        ) {
            Manufacturer.entries.forEach { brand ->
                SelectChip(
                    text = stringResource(brand.label),
                    selected = brand == selected,
                    onClick = { onSelect(brand) },
                    showCheck = true
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            selected.steps.forEachIndexed { index, step ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                    StepIllustration(highlightRow = index + 1)
                    Text(
                        stringResource(R.string.battery_step_numbered, index + 1, stringResource(step)),
                        style = Doz.type.caption,
                        color = Doz.colors.textSecondary
                    )
                }
            }
        }
    }
}

/** Abstract phone-settings sketch: grey rows with one highlighted row. Decorative only. */
@Composable
private fun StepIllustration(highlightRow: Int) {
    val c = Doz.colors
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .background(c.surfaceContainerHigh, DozShapes.small)
            .padding(Space.s12)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (0..3).forEach { row ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(if (row == highlightRow) c.primary else c.borderDefault, DozShapes.pill)
            )
        }
    }
}
