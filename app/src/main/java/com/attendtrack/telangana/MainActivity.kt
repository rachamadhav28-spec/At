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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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

class MainActivity : ComponentActivity() {
    private val vm: AttendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Motion.load(this)
        setContent {
            GlassTheme {
                var showIntro by rememberSaveable { mutableStateOf(true) }
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
        containerColor = AppBg,
        bottomBar = {
            NavigationBar(containerColor = AppCard) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            val pop by animateFloatAsState(
                                if (tab == i) 1.25f else 1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "tabIcon"
                            )
                            Icon(
                                t.icon, t.label,
                                Modifier.graphicsLayer {
                                    scaleX = pop
                                    scaleY = pop
                                }
                            )
                        },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = AppAccent,
                            unselectedIconColor = Color(0xFF9A9A9A),
                            unselectedTextColor = Color(0xFF9A9A9A)
                        )
                    )
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
