package app.dozdefteri.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Every component and screen is previewed in light, dark and at 200 % font scale (plan section 0.7). */
@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFFCFAF6)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF141210,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(name = "Font 200%", showBackground = true, backgroundColor = 0xFFFCFAF6, fontScale = 2f)
annotation class DozPreviews
