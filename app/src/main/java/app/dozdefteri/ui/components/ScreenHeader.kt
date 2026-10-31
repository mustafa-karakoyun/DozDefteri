package app.dozdefteri.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.dozdefteri.R
import app.dozdefteri.ui.theme.Doz
import app.dozdefteri.ui.theme.DozDefteriTheme
import app.dozdefteri.ui.theme.DozPreviews
import app.dozdefteri.ui.theme.Space

/** Active profile for [ScreenHeader]; null hides the chip (e.g. before the first profile exists). */
data class HeaderProfile(val name: String, val colorIndex: Int, val onClick: () -> Unit)

/** Main-screen header: Headline title, optional subtitle (date), profile chip on the right. */
@Composable
fun ScreenHeader(
    title: String,
    profile: HeaderProfile?,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s12),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Space.s16, end = Space.s16, top = Space.s16, bottom = Space.s8)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = Doz.type.headline,
                color = Doz.colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) Text(subtitle, style = Doz.type.bodyMedium, color = Doz.colors.textSecondary)
        }
        if (profile != null) ProfileChip(profile.name, profile.colorIndex, profile.onClick)
    }
}

enum class NavIcon { Back, Close }

/** Pushed-screen top bar: ← or ✕, Title/Large title and an optional trailing slot. */
@Composable
fun DozTopBar(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    navIcon: NavIcon = NavIcon.Back,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Space.s4, vertical = Space.s8)
    ) {
        IconButton(onClick = onNavigate) {
            Icon(
                if (navIcon == NavIcon.Back) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                contentDescription = stringResource(if (navIcon == NavIcon.Back) R.string.back else R.string.close),
                tint = Doz.colors.textPrimary
            )
        }
        Text(
            title,
            style = Doz.type.titleLarge,
            color = Doz.colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = Space.s8)
                .semantics { heading() }
        )
        trailing()
    }
}

@DozPreviews
@Composable
private fun HeaderPreview() {
    DozDefteriTheme {
        Column {
            ScreenHeader("Bugün", HeaderProfile("Ayşe", 0) {}, subtitle = "Pazar, 1 Kasım")
            DozTopBar("Rapor ve yedek", onNavigate = {})
        }
    }
}
