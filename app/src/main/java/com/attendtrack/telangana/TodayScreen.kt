package com.attendtrack.telangana

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val todaySlots = listOf(
    "10:00 – 11:00", "11:00 – 12:00", "12:00 – 1:00",
    "2:00 – 3:00", "3:00 – 4:00", "4:00 – 5:00"
)
private val todayMuted = Color(0xFFA9B0BC)

@Composable
private fun Entrance(content: @Composable () -> Unit) {
    val e = remember { Animatable(if (Motion.level == 0) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (Motion.level > 0) e.animateTo(1f, tween(450))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = e.value
                translationY = (1f - e.value) * 32.dp.toPx()
            }
    ) { content() }
}

@Composable
private fun TodayRing(fraction: Float, ringSize: Dp, label: String, color: Color) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    val tint by animateColorAsState(color, tween(500), label = "ringColor")
    Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(Color(0x33FFFFFF), 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            drawArc(
                tint, -90f, 360f * anim, false, topLeft, arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-1).sp
            )
        )
    }
}

@Composable
private fun TodayChip(filled: Boolean, text: String, color: Color = Color.White) {
    val shape = RoundedCornerShape(50)
    val pop = remember { Animatable(if (Motion.level == 0) 1f else 0.6f) }
    LaunchedEffect(Unit) {
        if (Motion.level > 0) pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Text(
        text,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .background(if (filled) color else Color.Transparent, shape)
            .border(1.dp, color, shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = if (filled) Color.Black else color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    )
}

@Composable
private fun HeroStat(value: String, label: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = color)
        Text(label, fontSize = 11.sp, letterSpacing = 1.sp, color = todayMuted)
    }
}

@Composable
private fun PopIcon(icon: ImageVector, selected: Boolean) {
    val s by animateFloatAsState(
        if (selected) 1.35f else 1f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "pop"
    )
    Icon(
        icon, null,
        Modifier.size(18.dp).graphicsLayer {
            scaleX = s
            scaleY = s
        }
    )
}

@Composable
private fun TodayToggle(
    selected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(50)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "togglePress"
    )
    val bg by animateColorAsState(
        if (selected) selectedBg else Color.Transparent, tween(250), label = "toggleBg"
    )
    val edge by animateColorAsState(
        if (selected) selectedBg else Color(0xFF3A4250), tween(250), label = "toggleEdge"
    )
    val fg by animateColorAsState(
        if (selected) Color.Black else Color.White, tween(250), label = "toggleFg"
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, edge, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: AttendViewModel) {
    val list by vm.subjects.collectAsStateWithLifecycle()
    val recs by vm.records.collectAsStateWithLifecycle()
    val dateStr by vm.selectedDate.collectAsStateWithLifecycle()
    val hols by vm.holidays.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var picking by remember { mutableStateOf(false) }

    val date = LocalDate.parse(dateStr)
    val att = list.sumOf { it.attended }
    val held = list.sumOf { it.held }
    val pct = Calculator.percent(att, held)
    val shownPct by animateFloatAsState(pct.toFloat(), tween(900), label = "pct")
    val shownAtt by animateFloatAsState(att.toFloat(), tween(700), label = "att")
    val shownAbsent by animateFloatAsState((held - att).toFloat(), tween(700), label = "absent")
    val shownHeld by animateFloatAsState(held.toFloat(), tween(700), label = "held")
    val ringColor = if (held == 0) AppAccentSoft else statusColor(pct)
    val hol = hols.firstOrNull { dateStr >= it.startDate && dateStr <= it.endDate }
    val periods = if (hol != null) emptyList() else (Timetable.week[date.dayOfWeek] ?: emptyList())
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    val classCount = periods.count { it != null }
    val isFuture = date.isAfter(LocalDate.now())
    val marked = recs.count { r -> periods.getOrNull(r.period - 1) != null }
    val progress by animateFloatAsState(
        if (classCount == 0) 0f else marked.toFloat() / classCount, tween(500), label = "sched"
    )

    var shakeKey by remember { mutableStateOf(0) }
    var confettiKey by remember { mutableStateOf(0) }
    var lastPct by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(att, held) {
        if (held > 0) {
            val prev = lastPct
            if (prev != null) {
                if (prev < Calculator.TARGET && pct >= Calculator.TARGET) confettiKey++
                if (prev >= Calculator.TARGET && pct < Calculator.TARGET) shakeKey++
            }
            lastPct = pct
        }
    }
    val shake = rememberShake(shakeKey)
    val bump = remember { Animatable(1f) }
    LaunchedEffect(att, held) {
        if (Motion.level > 0 && held > 0) {
            bump.animateTo(1.07f, tween(120))
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val go = { d: LocalDate -> if (d.year in 2026..2027) vm.setDate(d.toString()) }
    val isHoliday = { d: LocalDate ->
        val s = d.toString()
        hols.any { s >= it.startDate && s <= it.endDate }
    }

    val advice = when {
        held == 0 -> "Mark your first class to begin"
        pct >= Calculator.TARGET -> {
            val n = Calculator.canSkip(att, held)
            "You can skip $n period" + if (n == 1) "" else "s"
        }
        else -> {
            val n = Calculator.needed(att, held)
            "Attend the next $n period" + (if (n == 1) "" else "s") + " to reach 75%"
        }
    }
    val chipState = when {
        held == 0 -> 0
        pct >= Calculator.TARGET -> 1
        else -> 2
    }

    val heroShape = RoundedCornerShape(28.dp)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = dateStr,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 3 })
                    },
                    label = "dateTitle"
                ) { ds ->
                    val d = LocalDate.parse(ds)
                    Column {
                        Text(
                            if (d == LocalDate.now()) "TODAY" else d.dayOfWeek.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = todayMuted
                        )
                        Text(
                            d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                AnimatedVisibility(
                    visible = date != LocalDate.now(),
                    enter = scaleIn(tween(250), initialScale = 0.5f) + fadeIn(tween(250)),
                    exit = scaleOut(tween(200), targetScale = 0.5f) + fadeOut(tween(200))
                ) {
                    OutlinedButton(onClick = { vm.setDate(LocalDate.now().toString()) }) {
                        Text("Today", fontSize = 13.sp)
                    }
                }
                IconButton(onClick = { picking = true }) {
                    Icon(Icons.Default.DateRange, "Pick a date", tint = Color.White)
                }
            }
        }

        item {
            Entrance {
                Box {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .then(shake)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF2B3470), Color(0xFF151A2B))),
                                heroShape
                            )
                            .border(1.dp, Color(0xFF3A4392), heroShape)
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.graphicsLayer {
                                    scaleX = bump.value
                                    scaleY = bump.value
                                }
                            ) {
                                TodayRing(
                                    (pct / 100.0).toFloat(), 128.dp,
                                    "%.1f%%".format(shownPct), ringColor
                                )
                            }
                            Spacer(Modifier.width(20.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "OVERALL ATTENDANCE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = todayMuted
                                )
                                Crossfade(targetState = chipState, animationSpec = tween(300), label = "chip") { st ->
                                    when (st) {
                                        0 -> TodayChip(false, "NO DATA YET", todayMuted)
                                        1 -> TodayChip(true, "ON TRACK", StatusGreen)
                                        else -> TodayChip(false, "BELOW 75%", statusColor(pct))
                                    }
                                }
                                Crossfade(targetState = advice, animationSpec = tween(350), label = "advice") {
                                    Text(it, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0x33000000), RoundedCornerShape(16.dp))
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HeroStat("${shownAtt.toInt()}", "ATTENDED", StatusGreen)
                            HeroStat(
                                "${shownAbsent.toInt()}", "ABSENT",
                                if (held - att > 0) StatusRed else Color.White
                            )
                            HeroStat("${shownHeld.toInt()}", "HELD")
                        }
                    }
                    ConfettiBurst(confettiKey, Modifier.matchParentSize())
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { go(date.minusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Previous week", tint = Color.White)
                }
                AnimatedContent(
                    targetState = monday,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (targetState.isAfter(initialState)) 1 else -1
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 * dir }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 * dir })
                    },
                    label = "week"
                ) { mon ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0..5) {
                            val d = mon.plusDays(i.toLong())
                            val sel = d == date
                            val shape = RoundedCornerShape(18.dp)
                            val dayBg by animateColorAsState(
                                if (sel) AppAccent else AppCard, tween(300), label = "dayBg"
                            )
                            val dayMuted by animateColorAsState(
                                if (sel) Color(0xFFDDE0FF) else todayMuted, tween(300), label = "dayMuted"
                            )
                            val dayScale by animateFloatAsState(
                                if (sel) 1.07f else 1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dayScale"
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = dayScale
                                        scaleY = dayScale
                                    }
                                    .clip(shape)
                                    .background(dayBg, shape)
                                    .border(1.dp, if (sel) AppAccent else AppBorder, shape)
                                    .clickable { go(d) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    d.dayOfWeek.name.take(3),
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = dayMuted
                                )
                                Text(
                                    d.dayOfMonth.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .graphicsLayer { alpha = if (isHoliday(d)) 1f else 0f }
                                        .background(if (sel) Color.White else StatusAmber, CircleShape)
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = { go(date.plusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowRight, "Next week", tint = Color.White)
                }
            }
        }

        if (hol != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TodayChip(false, "HOLIDAY", AppAccentSoft)
                        Text(hol.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "No classes. Holidays never count as absences.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = todayMuted
                        )
                    }
                }
            }
        } else if (periods.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("No classes on this day", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "SCHEDULE  ·  $marked of $classCount marked",
                            style = MaterialTheme.typography.labelLarge,
                            color = todayMuted,
                            modifier = Modifier.weight(1f)
                        )
                        AnimatedVisibility(
                            visible = !isFuture && marked < classCount,
                            enter = scaleIn(tween(250), initialScale = 0.8f) + fadeIn(tween(250)),
                            exit = scaleOut(tween(200), targetScale = 0.8f) + fadeOut(tween(200))
                        ) {
                            OutlinedButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.markAllPresent()
                            }) { Text("All present", fontSize = 13.sp) }
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0xFF262C36), CircleShape)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0.001f, 1f))
                                .background(StatusGreen, CircleShape)
                        )
                    }
                }
            }
            if (isFuture) {
                item {
                    Stagger(0) {
                        Text(
                            "This day hasn't happened yet, so marking is locked.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = StatusAmber
                        )
                    }
                }
            }
        }

        itemsIndexed(periods) { i, name ->
            val rec = recs.firstOrNull { it.period == i + 1 }
            val tone = subjectColor(name)
            Stagger(i) {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).height(IntrinsicSize.Min)) {
                        Box(
                            Modifier
                                .width(5.dp)
                                .fillMaxHeight()
                                .background(tone, RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "PERIOD ${i + 1}  ·  ${todaySlots[i]}",
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = todayMuted
                            )
                            Text(
                                name ?: "Free period",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (name == null) todayMuted else Color.White
                            )
                            if (name != null) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier.alpha(if (isFuture) 0.4f else 1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TodayToggle(
                                        selected = rec?.present == true,
                                        selectedBg = StatusGreen,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            vm.markPeriod(name, i + 1, true)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PopIcon(Icons.Default.Check, rec?.present == true)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Present", fontWeight = FontWeight.Medium)
                                    }
                                    TodayToggle(
                                        selected = rec?.present == false,
                                        selectedBg = StatusRed,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            vm.markPeriod(name, i + 1, false)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PopIcon(Icons.Default.Close, rec?.present == false)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Absent", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (picking) {
        val state = rememberDatePickerState(
            yearRange = 2026..2027,
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        )
                    }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
