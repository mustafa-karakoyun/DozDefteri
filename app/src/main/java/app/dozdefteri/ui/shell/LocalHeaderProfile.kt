package app.dozdefteri.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import app.dozdefteri.ui.components.HeaderProfile

/** Active profile chip data for every main-screen header, provided by the nav host. */
val LocalHeaderProfile = staticCompositionLocalOf<HeaderProfile?> { null }
