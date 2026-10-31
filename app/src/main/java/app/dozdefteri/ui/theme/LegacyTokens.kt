package app.dozdefteri.ui.theme

import androidx.compose.ui.graphics.Color

// Temporary aliases for screens that are rebuilt in later UI phases. Removed once nothing uses them.

val StatusTaken: Color = Teal.c600
val StatusMissed: Color = Clay.c700
val StatusPartial: Color = Amber.c700
val ProfileColors: List<Color> = ProfilePalette.map { it.color }

fun profileColor(index: Int): Color = profileColorOf(index).color
