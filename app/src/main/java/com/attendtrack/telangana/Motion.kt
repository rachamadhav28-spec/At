package com.attendtrack.telangana

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object Motion {
    // 0 = Off, 1 = Normal, 2 = Max
    var level by mutableStateOf(2)

    fun load(ctx: Context) {
        level = ctx.getSharedPreferences("attendtrack", 0).getInt("motion", 2)
    }

    fun save(ctx: Context, value: Int) {
        level = value
        ctx.getSharedPreferences("attendtrack", 0).edit().putInt("motion", value).apply()
    }
}

fun <T> tween(
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing
): FiniteAnimationSpec<T> =
    if (Motion.level == 0) snap()
    else androidx.compose.animation.core.tween(durationMillis, delayMillis, easing)

fun <T> spring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium
): FiniteAnimationSpec<T> =
    if (Motion.level == 0) snap()
    else androidx.compose.animation.core.spring(dampingRatio, stiffness)

@Composable
fun Stagger(index: Int, content: @Composable () -> Unit) {
    val a = remember { Animatable(if (Motion.level == 0) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (Motion.level > 0) {
            a.animateTo(1f, tween(380, (index * 70).coerceAtMost(420)))
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = a.value
                translationY = (1f - a.value) * 24.dp.toPx()
            }
    ) { content() }
}

@Composable
fun rememberShake(trigger: Int): Modifier {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0 && Motion.level >= 2) {
            for (v in listOf(-14f, 12f, -8f, 5f, 0f)) {
                x.animateTo(v, tween(60))
            }
        }
    }
    return Modifier.graphicsLayer { translationX = x.value }
}

@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    val p = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0 && Motion.level >= 2) {
            p.snapTo(0f)
            p.animateTo(1f, tween(1400))
        }
    }
    if (p.value < 1f) {
        Canvas(modifier) {
            val colors = listOf(
                Color(0xFF3DDC97), Color(0xFF5B6CFF), Color(0xFFFFB547),
                Color(0xFFFF6B8A), Color.White
            )
            for (i in 0 until 28) {
                val ang = (i * 360f / 28f) * (Math.PI.toFloat() / 180f)
                val speed = 0.35f + (i % 5) * 0.12f
                val dist = size.minDimension * speed * p.value
                val x = size.width / 2 + cos(ang) * dist * 1.6f
                val y = size.height / 2 + sin(ang) * dist + 220f * p.value * p.value
                drawCircle(
                    colors[i % colors.size].copy(alpha = 1f - p.value),
                    radius = 6f + (i % 3) * 2f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
