package com.attendtrack.telangana

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * A colour theme is a pair: [strong] (the vivid or dark colour) and [light] (the highlight colour),
 * taken from the palette cards. Dark "strong" colours become the background and the light colour the
 * accent; vivid "strong" colours become the accent on a softly tinted dark background.
 */
data class AppTheme(val name: String, val strong: Color, val light: Color)

object Themes {
    val default = AppTheme("Midnight Indigo", Color(0xFF5B6CFF), Color(0xFF8C98FF))

    val all: List<AppTheme> = listOf(
        default,
        AppTheme("Neon Orange", Color(0xFFFF6115), Color(0xFFFFFCF4)),
        AppTheme("Inkberry", Color(0xFF1A0B2E), Color(0xFFFFB7A5)),
        AppTheme("Forest Graphite", Color(0xFF18251D), Color(0xFFB7FF72)),
        AppTheme("Signal Violet", Color(0xFF7A35FF), Color(0xFFF0F2F5)),
        AppTheme("Clay Brown", Color(0xFF6B352A), Color(0xFFFFF1A6)),
        AppTheme("Quantum Blue", Color(0xFF2356FC), Color(0xFFDCF5FF)),
        AppTheme("Cyber Teal", Color(0xFF003039), Color(0xFF8DFEE0)),
        AppTheme("Raspberry", Color(0xFFC31657), Color(0xFFDFF0FC)),
        AppTheme("Prompt Blue", Color(0xFF0B132B), Color(0xFF39FF88)),
        AppTheme("Blueberry", Color(0xFF233A93), Color(0xFFFDEFC8))
    )

    var current by mutableStateOf(default)
        private set

    fun load(ctx: Context) {
        val name = ctx.getSharedPreferences("attendtrack", 0).getString("theme", default.name)
        current = all.firstOrNull { it.name == name } ?: default
    }

    fun save(ctx: Context, theme: AppTheme) {
        current = theme
        ctx.getSharedPreferences("attendtrack", 0).edit().putString("theme", theme.name).apply()
    }

    private val base = Color(0xFF0E1116)

    private fun luminance(c: Color) = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

    private val isDark: Boolean get() = current !== default && luminance(current.strong) < 0.12f

    val bg: Color
        get() = when {
            current === default -> base
            isDark -> lerp(current.strong, Color.Black, 0.35f)
            else -> lerp(base, current.strong, 0.06f)
        }

    val card: Color
        get() = when {
            current === default -> Color(0xFF171B22)
            isDark -> lerp(current.strong, Color.White, 0.08f)
            else -> lerp(base, current.strong, 0.12f)
        }

    val border: Color
        get() = when {
            current === default -> Color(0xFF2A303B)
            isDark -> lerp(current.strong, Color.White, 0.2f)
            else -> lerp(base, current.strong, 0.28f)
        }

    val accent: Color get() = if (isDark) current.light else current.strong

    val accentSoft: Color get() = current.light

    /** Text/icon colour that stays readable on top of [accent]. */
    val onAccent: Color get() = if (luminance(accent) > 0.55f) Color(0xFF111111) else Color.White

    val heroTop: Color
        get() = when {
            current === default -> Color(0xFF2B3470)
            isDark -> lerp(current.strong, Color.White, 0.16f)
            else -> lerp(base, current.strong, 0.45f)
        }

    val heroBottom: Color
        get() = when {
            current === default -> Color(0xFF151A2B)
            isDark -> lerp(current.strong, Color.Black, 0.3f)
            else -> lerp(base, current.strong, 0.1f)
        }

    val heroBorder: Color
        get() = when {
            current === default -> Color(0xFF3A4392)
            isDark -> lerp(current.strong, current.light, 0.45f)
            else -> lerp(base, current.strong, 0.65f)
        }
}
