package com.attendtrack.telangana

import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette
val AppBg = Color(0xFF0E1116)
val AppCard = Color(0xFF171B22)
val AppBorder = Color(0xFF2A303B)
val AppAccent = Color(0xFF5B6CFF)
val AppAccentSoft = Color(0xFF8C98FF)
val StatusGreen = Color(0xFF3DDC97)
val StatusAmber = Color(0xFFFFB547)
val StatusRed = Color(0xFFFF6B6B)

fun statusColor(pct: Double): Color = when {
    pct >= 75.0 -> StatusGreen
    pct >= 65.0 -> StatusAmber
    else -> StatusRed
}

private val light = FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
private val medium = FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL))
private val condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = light, fontSize = 64.sp, lineHeight = 72.sp, letterSpacing = (-1.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = condensed, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, letterSpacing = 2.sp
    ),
    titleMedium = TextStyle(fontFamily = medium, fontSize = 18.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = condensed, fontSize = 14.sp, letterSpacing = 1.5.sp),
    bodyLarge = TextStyle(fontFamily = light, fontSize = 16.sp, letterSpacing = 0.3.sp)
)

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppAccentSoft,
            onPrimary = Color.Black,
            background = AppBg,
            surface = AppBg,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB8BEC9),
            surfaceContainerHigh = Color(0xFF1C212B)
        ),
        typography = AppTypography
    ) {
        Box(Modifier.fillMaxSize().background(AppBg)) { content() }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    Column(
        modifier
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 32.dp.toPx()
            }
            .background(AppCard, shape)
            .border(1.dp, AppBorder, shape),
        content = content
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(50)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    val bg by animateColorAsState(
        if (pressed) Color(0xFF4757E0) else AppAccent,
        label = "bg"
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(bg, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(50)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    val bg by animateColorAsState(
        if (pressed) Color(0xFF222833) else Color.Transparent,
        label = "bg"
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(bg, shape)
            .border(1.dp, Color(0xFF3A4250), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}
