package com.attendtrack.telangana

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val moreMuted = Color(0xFFA9B0BC)
private val fmtFull = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val fmtShort = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val fmtDay = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

private fun msToDate(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()

private fun rangeText(h: Holiday): String {
    val s = LocalDate.parse(h.startDate)
    val e = LocalDate.parse(h.endDate)
    return if (s == e) s.format(fmtDay) else "${s.format(fmtShort)} – ${e.format(fmtFull)}"
}

private fun whenText(h: Holiday, today: LocalDate): String {
    val s = LocalDate.parse(h.startDate)
    val e = LocalDate.parse(h.endDate)
    return when {
        today > e -> "Past"
        today >= s -> "Happening now"
        else -> {
            val n = ChronoUnit.DAYS.between(today, s)
            if (n == 1L) "Tomorrow" else "In $n days"
        }
    }
}

@Composable
private fun MoreChip(filled: Boolean, text: String) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        modifier = Modifier
            .background(if (filled) Color.White else Color.Transparent, shape)
            .border(1.dp, Color.White, shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = if (filled) Color.Black else Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    )
}

@Composable
private fun HolidayCard(
    h: Holiday,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(h.name, style = MaterialTheme.typography.titleMedium)
                Text(rangeText(h), style = MaterialTheme.typography.bodyLarge, color = moreMuted)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoreChip(!h.provisional, if (h.provisional) "PROVISIONAL" else "OFFICIAL")
                    Text(whenText(h, today), fontSize = 13.sp, color = moreMuted)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete ${h.name}", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HolidaysScreen(vm: AttendViewModel) {
    val hols by vm.holidays.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var deleting by remember { mutableStateOf<Holiday?>(null) }
    var addStep by remember { mutableStateOf(0) }
    var newName by remember { mutableStateOf("") }

    val upcoming = hols.filter { LocalDate.parse(it.endDate) >= today }
    val past = hols.filter { LocalDate.parse(it.endDate) < today }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HOLIDAYS", style = MaterialTheme.typography.labelLarge, color = moreMuted)
                    Text("Days off", style = MaterialTheme.typography.titleLarge)
                }
                Button(onClick = { addStep = 1 }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add", fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            Text(
                "Holidays never count as absences. Provisional ones came from a list, so check them against your college calendar.",
                style = MaterialTheme.typography.bodyLarge,
                color = moreMuted
            )
        }
        if (upcoming.isNotEmpty()) {
            item { Text("UPCOMING", style = MaterialTheme.typography.labelLarge, color = moreMuted) }
            items(upcoming, key = { "u" + it.id }) { h ->
                HolidayCard(h, today, Modifier.animateItemPlacement()) { deleting = h }
            }
        }
        if (past.isNotEmpty()) {
            item { Text("PAST", style = MaterialTheme.typography.labelLarge, color = moreMuted) }
            items(past, key = { "p" + it.id }) { h ->
                Box(Modifier.animateItemPlacement().alpha(0.55f)) {
                    HolidayCard(h, today) { deleting = h }
                }
            }
        }
    }

    deleting?.let { h ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete holiday?") },
            text = { Text("${h.name} will be removed from your calendar.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteHoliday(h); deleting = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }

    if (addStep == 1) {
        AlertDialog(
            onDismissRequest = { addStep = 0 },
            title = { Text("New holiday") },
            text = { OutlinedTextField(newName, { newName = it }, label = { Text("Name") }) },
            confirmButton = {
                TextButton(onClick = { if (newName.isNotBlank()) addStep = 2 }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { addStep = 0 }) { Text("Cancel") } }
        )
    }

    if (addStep == 2) {
        val rs = rememberDateRangePickerState(yearRange = 2026..2027)
        DatePickerDialog(
            onDismissRequest = { addStep = 0 },
            confirmButton = {
                TextButton(onClick = {
                    val s = rs.selectedStartDateMillis
                    val e = rs.selectedEndDateMillis ?: s
                    if (s != null && e != null) {
                        vm.addHoliday(newName.trim(), msToDate(s), msToDate(e))
                        newName = ""
                        addStep = 0
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { addStep = 0 }) { Text("Cancel") } }
        ) { DateRangePicker(state = rs, modifier = Modifier.height(500.dp)) }
    }
}

@Composable
private fun MoreCard(title: String, body: String, content: @Composable RowScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = moreMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
private fun MotionSwitch() {
    val context = LocalContext.current
    val labels = listOf("Off", "Normal", "Max")
    val description = when (Motion.level) {
        0 -> "Everything is instant, with no movement. Best for low battery."
        1 -> "Smooth animations, without confetti or the shake."
        else -> "All animations, including confetti and the shake."
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Animation level", style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyLarge, color = moreMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, label ->
                    val sel = Motion.level == i
                    val shape = RoundedCornerShape(50)
                    val bg by animateColorAsState(
                        if (sel) AppAccent else Color.Transparent, tween(250), label = "segBg"
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(bg, shape)
                            .border(1.dp, if (sel) AppAccent else AppBorder, shape)
                            .clickable { Motion.save(context, i) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Medium, color = if (sel) AppOnAccent else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePicker() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Colour theme", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pick a colour pair for the whole app.",
                style = MaterialTheme.typography.bodyLarge, color = moreMuted
            )
            Themes.all.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        val sel = Themes.current == t
                        val shape = RoundedCornerShape(16.dp)
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(shape)
                                .border(if (sel) 2.dp else 1.dp, if (sel) AppAccent else AppBorder, shape)
                                .clickable { Themes.save(context, t) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row {
                                Box(Modifier.size(22.dp).background(t.strong, RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)))
                                Box(Modifier.size(22.dp).background(t.light, RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)))
                            }
                            Text(t.name, fontSize = 13.sp, maxLines = 2)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun shortName(n: String?): String = when (n) {
    null -> "Free"
    Timetable.MATHS -> "Maths"
    Timetable.CHEM -> "Chem"
    Timetable.ENG -> "English"
    Timetable.DLD -> "DLD"
    Timetable.CLAB -> "Chem lab"
    Timetable.ELAB -> "Eng lab"
    Timetable.GRAPH -> "Graphics"
    else -> n
}

@Composable
fun MoreScreen(vm: AttendViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var ttPreview by remember { mutableStateOf<TimetableResult?>(null) }
    var attPreview by remember { mutableStateOf<AttendanceResult?>(null) }
    var roll by remember {
        mutableStateOf(context.getSharedPreferences("attendtrack", 0).getString("roll_no", "") ?: "")
    }

    val ttPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            busy = true
            msg = "Reading timetable..."
            scope.launch {
                val o = PdfImport.readTimetable(context, uri)
                busy = false
                if (o.value != null) {
                    ttPreview = o.value
                    msg = ""
                } else msg = o.error ?: "Could not read the PDF"
            }
        }
    }
    val attPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            busy = true
            msg = "Reading attendance..."
            scope.launch {
                val o = PdfImport.readAttendance(context, uri, roll.trim())
                busy = false
                if (o.value != null) {
                    attPreview = o.value
                    msg = ""
                } else msg = o.error ?: "Could not read the PDF"
            }
        }
    }

    val csvMaker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = vm.buildCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                msg = "CSV saved"
            }
        }
    }
    val backupMaker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = vm.buildBackup()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                msg = "Backup saved"
            }
        }
    }
    val restorer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                msg = if (vm.restoreBackup(text)) "Restore complete" else "Restore failed: wrong file"
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("MORE", style = MaterialTheme.typography.labelLarge, color = moreMuted)
                Text("Settings and data", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (msg.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visibleState = remember(msg) {
                        MutableTransitionState(false).apply { targetState = true }
                    },
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it }
                ) { MoreChip(true, msg.uppercase()) }
            }
        }
        item { MotionSwitch() }
        item { ThemePicker() }
        item {
            MoreCard(
                "Timetable from PDF",
                "Upload your class timetable PDF. You will see what was read before it replaces the current timetable."
            ) {
                Button(onClick = { if (!busy) ttPicker.launch(arrayOf("application/pdf")) }) {
                    Text("Upload timetable")
                }
                OutlinedButton(onClick = {
                    Timetable.reset(context)
                    msg = "Timetable reset to the built-in one"
                }) { Text("Reset") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Attendance from PDF", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Type your S.No (the serial number in the class list), then upload the attendance PDF. Your row is found by that number and the percentage is filled in.",
                        style = MaterialTheme.typography.bodyLarge, color = moreMuted
                    )
                    OutlinedTextField(
                        value = roll,
                        onValueChange = {
                            roll = it
                            context.getSharedPreferences("attendtrack", 0).edit()
                                .putString("roll_no", it).apply()
                        },
                        label = { Text("Your S.No (or roll number)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        if (roll.isBlank()) msg = "Type your S.No first"
                        else if (!busy) attPicker.launch(arrayOf("application/pdf"))
                    }) { Text("Upload attendance PDF") }
                }
            }
        }
        item {
            MoreCard(
                "Backup",
                "Save everything (subjects, attendance, holidays) to a file you can keep or move to a new phone."
            ) { Button(onClick = { backupMaker.launch("attendtrack-backup.json") }) { Text("Save backup") } }
        }
        item {
            MoreCard(
                "Restore",
                "Load a backup file. This replaces all the data currently in the app."
            ) { OutlinedButton(onClick = { confirmRestore = true }) { Text("Restore from file") } }
        }
        item {
            MoreCard(
                "Export CSV",
                "A spreadsheet of every period you marked, for Excel or Google Sheets."
            ) { OutlinedButton(onClick = { csvMaker.launch("attendtrack.csv") }) { Text("Export CSV") } }
        }
        item {
            Text(
                "AttendTrack  ·  A app by Racha Madhav",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = moreMuted,
                fontSize = 13.sp
            )
        }
    }

    ttPreview?.let { r ->
        AlertDialog(
            onDismissRequest = { ttPreview = null },
            title = { Text("Use this timetable?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Read ${r.daysRead} of 6 days. Periods are 10, 11, 12, 2, 3, 4.", color = moreMuted, fontSize = 13.sp)
                    Timetable.days.forEach { d ->
                        val list = r.week[d] ?: emptyList()
                        Text(
                            d.name.take(3) + ": " + list.joinToString(", ") { shortName(it) },
                            fontSize = 13.sp
                        )
                    }
                    if (r.unread.isNotEmpty()) {
                        Text("Not recognised: " + r.unread.joinToString("; "), color = moreMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Timetable.save(context, r.week)
                    ttPreview = null
                    msg = "Timetable updated"
                }) { Text("Use it") }
            },
            dismissButton = { TextButton(onClick = { ttPreview = null }) { Text("Cancel") } }
        )
    }

    attPreview?.let { r ->
        val hasData = r.perSubject.isNotEmpty() || r.overall != null
        AlertDialog(
            onDismissRequest = { attPreview = null },
            title = { Text(if (hasData) "Fill attendance?" else "Row found, numbers unclear") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (r.perSubject.isNotEmpty()) {
                        r.perSubject.forEach { (n, p) ->
                            val pc = if (p.second == 0) 0.0 else p.first * 100.0 / p.second
                            Text("$n: ${p.first} of ${p.second} (${"%.1f".format(pc)}%)", fontSize = 13.sp)
                        }
                    } else if (r.overall != null) {
                        val (a, h) = r.overall
                        Text("Overall: $a attended of $h held (${"%.1f".format(a * 100.0 / h)}%)")
                        Text("Subject columns were not detected, so this total is spread across your subjects.", color = moreMuted, fontSize = 12.sp)
                    } else {
                        Text("Read from your row: ${r.rowText}", fontSize = 13.sp)
                        Text("Nothing was changed. Send a sample of this PDF so the reader can be tuned to its layout.", color = moreMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                if (hasData) {
                    TextButton(onClick = {
                        vm.applyAttendance(r) { m -> msg = m }
                        attPreview = null
                    }) { Text("Fill it") }
                } else TextButton(onClick = { attPreview = null }) { Text("OK") }
            },
            dismissButton = {
                if (hasData) TextButton(onClick = { attPreview = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace all data?") },
            text = { Text("Restoring overwrites everything currently in the app with the backup file.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restorer.launch(arrayOf("*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } }
        )
    }
}
