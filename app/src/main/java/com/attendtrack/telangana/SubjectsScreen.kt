package com.attendtrack.telangana

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val subjMuted = Color(0xFFA9B0BC)

@Composable
private fun SubjBar(fraction: Float, color: Color) {
    val barColor by animateColorAsState(color, tween(400), label = "barColor")
    Box(Modifier.fillMaxWidth().height(10.dp)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF262C36), CircleShape))
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.001f, 1f))
                .background(barColor, CircleShape)
        )
        Box(Modifier.fillMaxHeight().fillMaxWidth(0.75f)) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF8A8A8A))
            )
        }
    }
}

@Composable
private fun SubjChip(filled: Boolean, text: String, color: Color) {
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
private fun SubjCard(s: Subject, onEdit: () -> Unit) {
    val pct = Calculator.percent(s.attended, s.held)
    val anim by animateFloatAsState((pct / 100.0).toFloat(), tween(800), label = "bar")
    val shownPct by animateFloatAsState(pct.toFloat(), tween(800), label = "subjPct")
    val tone = if (s.held == 0) AppAccentSoft else statusColor(pct)
    val advice = when {
        s.held == 0 -> "No classes yet"
        pct >= Calculator.TARGET -> {
            val n = Calculator.canSkip(s.attended, s.held)
            "Can skip $n period" + if (n == 1) "" else "s"
        }
        else -> "Attend next ${Calculator.needed(s.attended, s.held)} in a row"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(subjectColor(s.name), CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    s.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit, "Edit ${s.name}",
                        tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f%%".format(shownPct),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${s.attended} of ${s.held} periods",
                    color = subjMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            SubjBar(anim, tone)
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    s.held == 0 -> SubjChip(false, "NO DATA", subjMuted)
                    pct >= Calculator.TARGET -> SubjChip(true, "SAFE", StatusGreen)
                    else -> SubjChip(false, "LOW", statusColor(pct))
                }
                Spacer(Modifier.width(12.dp))
                Text(advice, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SubjectsScreen(vm: AttendViewModel) {
    val list by vm.subjects.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Subject?>(null) }

    val withData = list.filter { it.held > 0 }
    val safe = withData.count { Calculator.percent(it.attended, it.held) >= Calculator.TARGET }
    val low = withData.size - safe

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SUBJECTS", style = MaterialTheme.typography.labelLarge, color = subjMuted)
                Text("Where you stand", style = MaterialTheme.typography.titleLarge)
                if (withData.isEmpty()) {
                    Text(
                        "Mark some classes to see your status here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = subjMuted
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "$safe SAFE",
                            color = StatusGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "$low LOW",
                            color = if (low > 0) StatusRed else subjMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    "The grey line on each bar marks the 75% target.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = subjMuted
                )
            }
        }
        itemsIndexed(list, key = { _, s -> s.id }) { i, s ->
            Stagger(i) { SubjCard(s) { editing = s } }
        }
    }

    editing?.let { s ->
        var name by remember(s.id) { mutableStateOf(s.name) }
        var a by remember(s.id) { mutableStateOf(s.attended.toString()) }
        var h by remember(s.id) { mutableStateOf(s.held.toString()) }
        val num = KeyboardOptions(keyboardType = KeyboardType.Number)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") })
                    OutlinedTextField(a, { a = it }, label = { Text("Attended") }, keyboardOptions = num)
                    OutlinedTextField(h, { h = it }, label = { Text("Total held") }, keyboardOptions = num)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.edit(s, name, a.toIntOrNull() ?: s.attended, h.toIntOrNull() ?: s.held)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}
