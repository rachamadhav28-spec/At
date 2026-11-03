package com.attendtrack.telangana

import androidx.compose.ui.graphics.Color

private val subjectPalette = listOf(
    Color(0xFF7C8CFF),
    Color(0xFF3DDC97),
    Color(0xFFFFB547),
    Color(0xFFFF6B8A),
    Color(0xFF4FC3F7),
    Color(0xFFC084FC),
    Color(0xFFFF8A5B)
)

fun subjectColor(name: String?): Color {
    if (name == null) return Color(0xFF3A4250)
    val i = when (name) {
        "Engineering Mathematics-I" -> 0
        "Engineering Chemistry" -> 1
        "Communicative English" -> 2
        "DLD" -> 3
        "Chemistry Lab" -> 4
        "English Lab" -> 5
        "Engineering Graphics" -> 6
        else -> (name.hashCode() and 0x7fffffff) % subjectPalette.size
    }
    return subjectPalette[i]
}
