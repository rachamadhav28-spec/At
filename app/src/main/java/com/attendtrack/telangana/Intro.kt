package com.attendtrack.telangana

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val IntroGold = Color(0xFFD9BF85)

@Composable
fun IntroScreen(onDone: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val cam = remember { Animatable(1f) }
    val bars = remember { Animatable(0f) }
    val card = remember { Animatable(0f) }
    val beatAlpha = remember { Animatable(0f) }
    val slam = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val shock = remember { Animatable(1f) }
    val streak = remember { Animatable(0f) }
    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val sheen = remember { Animatable(0f) }
    val credit = remember { Animatable(0f) }
    val time = remember { Animatable(0f) }
    var beatText by remember { mutableStateOf("") }
    var impact by remember { mutableStateOf(0) }
    val shake = rememberShake(impact)

    LaunchedEffect(Unit) {
        if (Motion.level == 0) {
            delay(700)
            onDone()
            return@LaunchedEffect
        }
        launch { time.animateTo(1f, tween(6500, easing = LinearEasing)) }
        launch { cam.animateTo(1.08f, tween(6500, easing = LinearEasing)) }
        launch { bars.animateTo(1f, tween(600)) }
        delay(500)
        card.animateTo(1f, tween(500))
        delay(500)
        card.animateTo(0f, tween(300))
        delay(100)
        for (t in listOf("EVERY CLASS.", "EVERY PERIOD.", "EVERY PERCENT.")) {
            beatText = t
            impact++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            slam.snapTo(0f)
            beatAlpha.snapTo(1f)
            launch {
                flash.snapTo(0.55f)
                flash.animateTo(0f, tween(250))
            }
            slam.animateTo(1f, tween(180))
            delay(350)
            beatAlpha.animateTo(0f, tween(100))
            delay(60)
        }
        impact++
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(500))
        }
        launch {
            shock.snapTo(0f)
            shock.animateTo(1f, tween(900))
        }
        launch { logo.animateTo(1f, tween(800, easing = LinearEasing)) }
        launch {
            streak.snapTo(0f)
            streak.animateTo(1f, tween(800))
        }
        delay(200)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(150)
        launch { title.animateTo(1f, tween(700)) }
        launch { sheen.animateTo(1f, tween(1500, easing = LinearEasing)) }
        delay(700)
        credit.animateTo(1f, tween(500))
        delay(1000)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDone() }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(shake)
                .graphicsLayer {
                    scaleX = cam.value
                    scaleY = cam.value
                }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF141A30), Color(0xFF03040A)),
                        radius = 1300f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 36) {
                    val x = ((i * 53) % 100) / 100f * size.width
                    val speed = 0.4f + (i % 5) * 0.15f
                    val ph = ((i * 37) % 100) / 100f
                    val y = size.height * (1f - ((time.value * speed + ph) % 1f))
                    val tw = 0.35f + 0.65f * abs(sin(time.value * 6f + i))
                    drawCircle(
                        Color.White.copy(alpha = 0.35f * tw),
                        radius = 1.5f + (i % 3),
                        center = Offset(x, y)
                    )
                }
            }

            Column(
                Modifier.graphicsLayer { alpha = card.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "RACHA MADHAV",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = IntroGold,
                    letterSpacing = 8.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "PRESENTS",
                    fontSize = 11.sp,
                    color = Color(0x99FFFFFF),
                    letterSpacing = 6.sp
                )
            }

            Text(
                beatText,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 6.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = beatAlpha.value
                    val s = 1f + 0.6f * (1f - slam.value)
                    scaleX = s
                    scaleY = s
                }
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(120.dp)
                        .graphicsLayer { alpha = if (logo.value > 0f) 1f else 0f }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val k = size.width / 108f
                        fun p(x: Float, y: Float) = Offset(x * k, y * k)
                        fun mix(a: Offset, b: Offset, t: Float) =
                            Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                        val v = logo.value
                        val t1 = (v / 0.4f).coerceIn(0f, 1f)
                        val t2 = ((v - 0.4f) / 0.4f).coerceIn(0f, 1f)
                        val t3 = ((v - 0.8f) / 0.2f).coerceIn(0f, 1f)
                        val foot1 = p(32.54f, 72.27f)
                        val apex = p(54f, 27.03f)
                        val foot2 = p(75.46f, 72.27f)
                        if (t1 > 0f) {
                            drawLine(
                                Color.White, foot1, mix(foot1, apex, t1),
                                strokeWidth = 8f * k, cap = StrokeCap.Round
                            )
                        }
                        if (t2 > 0f) {
                            drawLine(
                                Color.White, apex, mix(apex, foot2, t2),
                                strokeWidth = 8f * k, cap = StrokeCap.Round
                            )
                        }
                        if (t3 > 0f) {
                            val a = p(43f, 58.35f)
                            drawLine(
                                Color(0xFF3DDC97), a, mix(a, p(65f, 58.35f), t3),
                                strokeWidth = 6.4f * k, cap = StrokeCap.Round
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "ATTENDTRACK",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFB9C0D0), Color.White, Color(0xFFB9C0D0)),
                            startX = -250f + 1500f * sheen.value,
                            endX = 250f + 1500f * sheen.value
                        ),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 10.sp,
                        shadow = Shadow(
                            color = Color(0xFF5B6CFF),
                            offset = Offset.Zero,
                            blurRadius = 28f
                        )
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = title.value
                        val s = 1.4f - 0.4f * title.value
                        scaleX = s
                        scaleY = s
                    }
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "APP BY RACHA MADHAV",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = IntroGold,
                    letterSpacing = 5.sp,
                    modifier = Modifier.graphicsLayer { alpha = credit.value }
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (shock.value < 1f) {
                drawCircle(
                    Color.White.copy(alpha = (1f - shock.value) * 0.8f),
                    radius = shock.value * maxOf(w, h) * 0.75f,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 5f * (1f - shock.value) + 1f)
                )
            }
            if (streak.value > 0f && streak.value < 1f) {
                val half = w / 2f * streak.value
                val sa = 1f - abs(2f * streak.value - 1f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent, Color(0xFF8C98FF), Color.White,
                            Color(0xFF8C98FF), Color.Transparent
                        ),
                        startX = w / 2f - half,
                        endX = w / 2f + half
                    ),
                    topLeft = Offset(w / 2f - half, h / 2f - 1.5f),
                    size = Size(half * 2f, 3f),
                    alpha = sa
                )
            }
            if (flash.value > 0f) {
                drawRect(Color.White.copy(alpha = flash.value))
            }
            val bh = h * 0.11f * bars.value
            drawRect(Color.Black, Offset(0f, 0f), Size(w, bh))
            drawRect(Color.Black, Offset(0f, h - bh), Size(w, bh))
        }
    }
}
