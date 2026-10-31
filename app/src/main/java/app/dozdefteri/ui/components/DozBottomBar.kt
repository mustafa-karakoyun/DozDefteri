package app.dozdefteri.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews

data class BottomTab(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Bugün · İlaçlarım · Geçmiş · Ayarlar, with a pill indicator behind the selected icon. */
@Composable
fun DozBottomBar(tabs: List<BottomTab>) {
    val c = Doz.colors
    NavigationBar(containerColor = c.surfaceContainer, contentColor = c.textSecondary) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.selected,
                onClick = tab.onClick,
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = Doz.type.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.onPrimaryContainer,
                    selectedTextColor = c.textPrimary,
                    indicatorColor = c.primaryContainer,
                    unselectedIconColor = c.textSecondary,
                    unselectedTextColor = c.textSecondary
                )
            )
        }
    }
}

@DozPreviews
@Composable
private fun BottomBarPreview() {
    DozDefteriTheme {
        DozBottomBar(
            listOf(
                BottomTab("Bugün", DozNavIcons.Today, true) {},
                BottomTab("İlaçlarım", DozNavIcons.Medications, false) {},
                BottomTab("Geçmiş", DozNavIcons.History, false) {},
                BottomTab("Ayarlar", DozNavIcons.Settings, false) {}
            )
        )
    }
}
