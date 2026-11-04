package com.attendtrack.telangana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

private const val INTRO_MAX_OPENS = 3

class MainActivity : ComponentActivity() {
    private val vm: AttendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Motion.load(this)
        Themes.load(this)
        Sounds.load(this)
        Timetable.load(this)
        // The intro plays only the first 3 times the app is opened.
        val prefs = getSharedPreferences("attendtrack", 0)
        val opens = prefs.getInt("intro_opens", 0)
        val playIntro = savedInstanceState == null && opens < INTRO_MAX_OPENS
        if (savedInstanceState == null && opens < INTRO_MAX_OPENS) {
            prefs.edit().putInt("intro_opens", opens + 1).apply()
        }
        setContent {
            GlassTheme {
                var showIntro by rememberSaveable { mutableStateOf(playIntro) }
                AnimatedContent(
                    targetState = showIntro,
                    transitionSpec = {
                        (fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.94f)) togetherWith
                            fadeOut(tween(450))
                    },
                    label = "intro"
                ) { intro ->
                    if (intro) IntroScreen { showIntro = false } else AppShell(vm)
                }
            }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Today", Icons.Default.Today),
    Tab("Subjects", Icons.Default.School),
    Tab("Holidays", Icons.Default.Event),
    Tab("More", Icons.Default.MoreHoriz)
)

@Composable
fun AppShell(vm: AttendViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        bottomBar = {
            val pill = RoundedCornerShape(32.dp)
            Row(
                Modifier
                    .padding(start = 28.dp, end = 28.dp, bottom = 18.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0x1FFFFFFF), pill)
                    .border(1.dp, Color(0x38FFFFFF), pill)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { i, t ->
                    val sel = tab == i
                    val pop by animateFloatAsState(
                        if (sel) 1.1f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabIcon"
                    )
                    Row(
                        Modifier
                            .weight(if (sel) 2.2f else 1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (sel) Color.White else Color.Transparent)
                            .clickable { tab = i },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            t.icon, t.label,
                            Modifier.size(22.dp).graphicsLayer {
                                scaleX = pop
                                scaleY = pop
                            },
                            tint = if (sel) Color(0xFF0A0C18) else Color(0xFFE8EAF6)
                        )
                        if (sel) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                t.label,
                                color = Color(0xFF0A0C18),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 5 * dir }) togetherWith
                        (fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { -it / 5 * dir })
                },
                label = "tabs"
            ) { page ->
                when (page) {
                    0 -> TodayScreen(vm)
                    1 -> SubjectsScreen(vm)
                    2 -> HolidaysScreen(vm)
                    else -> MoreScreen(vm)
                }
            }
        }
    }
}
