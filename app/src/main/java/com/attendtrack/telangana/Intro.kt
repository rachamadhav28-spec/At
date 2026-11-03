package com.attendtrack.telangana

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val LINE1 = "APP BY"
private const val LINE2 = "RACHA MADHAV"

/**
 * Typewriter intro: "APP BY" then "RACHA MADHAV", one keyboard click per letter,
 * with a blinking cursor. Tap anywhere to skip.
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    var typed1 by remember { mutableStateOf(0) }
    var typed2 by remember { mutableStateOf(0) }
    var cursorOn by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    fun finish() {
        if (!done) {
            done = true
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        if (Motion.level == 0) {
            delay(500)
            finish()
            return@LaunchedEffect
        }
        delay(600)
        for (i in 1..LINE1.length) {
            typed1 = i
            IntroSound.key(LINE1[i - 1])
            delay(if (LINE1[i - 1] == ' ') 190L else 120L)
        }
        delay(550) // cursor blinks after "APP BY"
        for (i in 1..LINE2.length) {
            typed2 = i
            IntroSound.key(LINE2[i - 1])
            delay(if (LINE2[i - 1] == ' ') 200L else 105L)
        }
        finished = true
        delay(1300)
        finish()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            cursorOn = !cursorOn
        }
    }

    val cursor = if (cursorOn) "|" else " "
    val onSecondLine = typed2 > 0 || (typed1 == LINE1.length && finished)

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { finish() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                LINE1.take(typed1) + if (!onSecondLine) cursor else "",
                color = Color.White,
                fontSize = 30.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                LINE2.take(typed2) + if (onSecondLine) cursor else "",
                color = AppAccentSoft,
                fontSize = 34.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }
    }
}
