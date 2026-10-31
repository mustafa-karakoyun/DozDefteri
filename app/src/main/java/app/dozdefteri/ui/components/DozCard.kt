package app.dozdefteri.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozShapes
import app.dozdefteri.ui.theme.Space

/** Flat card: 16 dp radius, 1 dp border (cards never use shadows). */
@Composable
fun DozCard(
    modifier: Modifier = Modifier,
    color: Color = Doz.colors.surfaceDefault,
    borderColor: Color? = Doz.colors.borderDefault,
    borderWidth: Int = 1,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DozShapes.card,
        color = color,
        contentColor = Doz.colors.textPrimary,
        border = borderColor?.let { BorderStroke(borderWidth.dp, it) }
    ) {
        Column(Modifier.padding(Space.s16), content = content)
    }
}
