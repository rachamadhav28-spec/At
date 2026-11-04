package com.attendtrack.telangana

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

data class OcrLine(val text: String, val box: Rect)

class OcrPage(val bitmap: Bitmap, val lines: List<OcrLine>)

class Outcome<T>(val value: T?, val error: String?)

data class TimetableResult(
    val week: Map<DayOfWeek, List<String?>>,
    val daysRead: Int,
    val unread: List<String>
)

data class AttendanceResult(
    val rowText: String,
    val perSubject: Map<String, Pair<Int, Int>>,
    val overall: Pair<Int, Int>?,
    val percent: Double?
)

/**
 * Reads PDFs by drawing each page to an image and running on-device text recognition on it.
 * That works the same for digital PDFs and scanned ones, and needs no internet.
 */
object PdfImport {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // ---------- reading ----------

    private suspend fun recognize(bmp: Bitmap): List<OcrLine> =
        suspendCancellableCoroutine { c ->
            recognizer.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { t ->
                    c.resume(
                        t.textBlocks.flatMap { it.lines }
                            .mapNotNull { l -> l.boundingBox?.let { OcrLine(l.text, it) } }
                    )
                }
                .addOnFailureListener { c.resumeWithException(it) }
        }

    private fun render(r: PdfRenderer, index: Int): Bitmap {
        val page = r.openPage(index)
        try {
            val scale = 2800f / maxOf(page.width, page.height)
            val bmp = Bitmap.createBitmap(
                (page.width * scale).toInt().coerceAtLeast(1),
                (page.height * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            bmp.eraseColor(AColor.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp
        } finally {
            page.close()
        }
    }

    private suspend fun <T> withRenderer(
        ctx: Context,
        uri: Uri,
        block: suspend (PdfRenderer) -> T
    ): T {
        val file = withContext(Dispatchers.IO) {
            val f = File(ctx.cacheDir, "import.pdf")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                f.outputStream().use { input.copyTo(it) }
            } ?: error("Cannot open the file")
            f
        }
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        try {
            return block(renderer)
        } finally {
            renderer.close()
            pfd.close()
        }
    }

    suspend fun readTimetable(ctx: Context, uri: Uri): Outcome<TimetableResult> = try {
        withRenderer(ctx, uri) { r ->
            var best: TimetableResult? = null
            for (i in 0 until minOf(r.pageCount, 3)) {
                val bmp = withContext(Dispatchers.Default) { render(r, i) }
                val lines = recognize(bmp)
                val res = withContext(Dispatchers.Default) { parseTimetable(OcrPage(bmp, lines)) }
                bmp.recycle()
                if (res != null && (best == null || res.daysRead > best.daysRead)) best = res
                if (best != null && best.daysRead == 6) break
            }
            if (best == null) {
                Outcome<TimetableResult>(
                    null,
                    "Could not find a timetable grid (day names with time headers) in this PDF."
                )
            } else Outcome<TimetableResult>(best, null)
        }
    } catch (e: Exception) {
        Outcome<TimetableResult>(null, "Could not read this file as a PDF.")
    }

    suspend fun readAttendance(ctx: Context, uri: Uri, roll: String): Outcome<AttendanceResult> = try {
        withRenderer(ctx, uri) { r ->
            var headers: List<Pair<String, Int>> = emptyList()
            var found: AttendanceResult? = null
            for (i in 0 until minOf(r.pageCount, 20)) {
                val bmp = withContext(Dispatchers.Default) { render(r, i) }
                val lines = recognize(bmp)
                bmp.recycle()
                val pageHeaders = subjectHeaders(lines, Int.MAX_VALUE)
                if (pageHeaders.size >= 2) headers = pageHeaders
                val res = withContext(Dispatchers.Default) { parseAttendance(lines, roll, headers) }
                if (res != null) {
                    found = res
                    break
                }
            }
            if (found == null) {
                Outcome<AttendanceResult>(null, "S.No / roll number $roll was not found in this PDF.")
            } else Outcome<AttendanceResult>(found, null)
        }
    } catch (e: Exception) {
        Outcome<AttendanceResult>(null, "Could not read this file as a PDF.")
    }

    // ---------- subject matching ----------

    fun matchSubject(text: String): String? {
        val t = text.lowercase()
        return when {
            "graphic" in t -> Timetable.GRAPH
            "dld" in t || "digital" in t || "logic" in t -> Timetable.DLD
            "math" in t -> Timetable.MATHS
            "chem" in t && "lab" in t -> Timetable.CLAB
            "english" in t && "lab" in t -> Timetable.ELAB
            "chem" in t -> Timetable.CHEM
            "english" in t || t.trim().startsWith("eng") -> Timetable.ENG
            else -> null
        }
    }

    // ---------- timetable ----------

    private val dayNames = mapOf(
        "MON" to DayOfWeek.MONDAY, "MONDAY" to DayOfWeek.MONDAY,
        "TUE" to DayOfWeek.TUESDAY, "TUES" to DayOfWeek.TUESDAY, "TUESDAY" to DayOfWeek.TUESDAY,
        "WED" to DayOfWeek.WEDNESDAY, "WEDNESDAY" to DayOfWeek.WEDNESDAY,
        "THU" to DayOfWeek.THURSDAY, "THUR" to DayOfWeek.THURSDAY, "THURS" to DayOfWeek.THURSDAY,
        "THURSDAY" to DayOfWeek.THURSDAY,
        "FRI" to DayOfWeek.FRIDAY, "FRIDAY" to DayOfWeek.FRIDAY,
        "SAT" to DayOfWeek.SATURDAY, "SATURDAY" to DayOfWeek.SATURDAY
    )

    private val timeRange =
        Regex("(\\d{1,2})\\s*[:.]\\s*(\\d{2})\\s*[-–—]\\s*(\\d{1,2})\\s*[:.]\\s*(\\d{2})")

    private fun periodOfHour(h: Int): Int? = when (h) {
        10 -> 0
        11 -> 1
        12 -> 2
        2, 14 -> 3
        3, 15 -> 4
        4, 16 -> 5
        else -> null
    }

    private fun verticalLines(bmp: Bitmap, y0: Int, y1: Int): List<Int> {
        val top = y0.coerceIn(0, bmp.height - 1)
        val bottom = y1.coerceIn(top + 1, bmp.height)
        val w = bmp.width
        val dark = IntArray(w)
        val row = IntArray(w)
        for (y in top until bottom) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val p = row[x]
                val lum = (AColor.red(p) * 299 + AColor.green(p) * 587 + AColor.blue(p) * 114) / 1000
                if (lum < 150) dark[x]++
            }
        }
        val need = (bottom - top) * 0.85f
        val xs = ArrayList<Int>()
        var x = 0
        while (x < w) {
            if (dark[x] >= need) {
                var e = x
                while (e + 1 < w && dark[e + 1] >= need) e++
                xs.add((x + e) / 2)
                x = e + 1
            } else x++
        }
        return xs
    }

    fun parseTimetable(page: OcrPage): TimetableResult? {
        val lines = page.lines
        // day labels (left-most candidate for each day)
        val dayLines = HashMap<DayOfWeek, OcrLine>()
        for (l in lines) {
            val key = l.text.uppercase().filter { it.isLetter() }
            val d = dayNames[key] ?: continue
            val old = dayLines[d]
            if (old == null || l.box.left < old.box.left) dayLines[d] = l
        }
        if (dayLines.size < 4) return null
        val firstDayTop = dayLines.values.minOf { it.box.centerY() }

        // time headers above the first row
        val headerCx = arrayOfNulls<Int>(6)
        var lunch: Rect? = null
        for (l in lines) {
            if (l.box.centerY() >= firstDayTop) continue
            for (m in timeRange.findAll(l.text)) {
                val h = m.groupValues[1].toInt()
                if (h == 1 || h == 13) {
                    lunch = l.box
                    continue
                }
                val p = periodOfHour(h) ?: continue
                if (headerCx[p] == null) headerCx[p] = l.box.centerX()
            }
        }
        if (headerCx.count { it != null } < 5) return null
        // fill one missing header by interpolation
        if (headerCx.count { it == null } == 1) {
            val i = headerCx.indexOfFirst { it == null }
            val a = headerCx.getOrNull(i - 1)
            val b = headerCx.getOrNull(i + 1)
            headerCx[i] = when {
                a != null && b != null -> (a + b) / 2
                a != null && i > 1 -> a + (a - (headerCx[i - 2] ?: a))
                b != null -> b - ((headerCx[i + 2] ?: b) - b)
                else -> null
            }
        }
        if (headerCx.any { it == null }) return null
        val hx = headerCx.map { it!! }

        val orderedDays = dayLines.entries.sortedBy { it.value.box.centerY() }
        val centers = orderedDays.map { it.value.box.centerY() }
        val rowH = if (centers.size > 1) (centers.last() - centers.first()) / (centers.size - 1) else 80
        val dayRight = dayLines.values.maxOf { it.box.right }
        val tableTop = firstDayTop - rowH
        val tableBottom = centers.last() + rowH

        val week = LinkedHashMap<DayOfWeek, MutableList<String?>>()
        for (d in Timetable.days) week[d] = MutableList(6) { null }
        val unread = LinkedHashSet<String>()

        val vlines = orderedDays.map { (_, lbl) ->
            verticalLines(page.bitmap, lbl.box.centerY() - rowH / 4, lbl.box.centerY() + rowH / 4)
        }

        for (l in lines) {
            val cy = l.box.centerY()
            val cx = l.box.centerX()
            if (cy < tableTop || cy > tableBottom || l.box.left <= dayRight) continue
            if (dayNames.containsKey(l.text.uppercase().filter { it.isLetter() })) continue
            val lu = lunch
            if (lu != null && cx in (lu.left - lu.width() / 6)..(lu.right + lu.width() / 6)) continue
            // row = the nearest day label
            var ri = 0
            for (i in centers.indices) if (abs(centers[i] - cy) < abs(centers[ri] - cy)) ri = i
            val day = orderedDays[ri].key
            val subject = matchSubject(l.text)
            if (subject == null) {
                if (l.text.count { it.isLetter() } >= 4) unread.add(l.text.trim())
                continue
            }
            val xs = vlines[ri]
            val left = xs.lastOrNull { it < cx } ?: 0
            val right = xs.firstOrNull { it > cx } ?: page.bitmap.width
            var covered = hx.indices.filter { hx[it] in (left + 1) until right }
            if (covered.isEmpty()) {
                covered = listOf(hx.indices.minByOrNull { abs(hx[it] - cx) } ?: 0)
            }
            for (p in covered) week[day]!![p] = subject
        }

        // days that were not found keep the timetable currently in use
        val finalWeek = LinkedHashMap<DayOfWeek, List<String?>>()
        for (d in Timetable.days) {
            finalWeek[d] = if (dayLines.containsKey(d)) week[d]!! else Timetable.week[d] ?: List(6) { null }
        }
        return TimetableResult(finalWeek, dayLines.size, unread.take(6))
    }

    // ---------- attendance ----------

    private val numberToken = Regex("^\\d+(\\.\\d+)?%?$")

    private fun norm(s: String) = s.uppercase().filter { it.isLetterOrDigit() }

    /** Subject names found in header text, with their horizontal centre. */
    private fun subjectHeaders(lines: List<OcrLine>, belowY: Int): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        for (l in lines) {
            if (l.box.centerY() >= belowY) continue
            val s = matchSubject(l.text) ?: continue
            if (l.text.any { it.isDigit() } && l.text.count { it.isLetter() } < 3) continue
            out.add(s to l.box.centerX())
        }
        return out.distinct()
    }

    private fun pairFrom(ints: List<Int>, pct: Double?): Pair<Int, Int>? {
        val cands = ArrayList<Pair<Int, Int>>()
        for (a in ints) for (h in ints) if (h > 0 && a <= h && !(a == h && ints.size > 2)) cands.add(a to h)
        if (cands.isEmpty()) return null
        if (pct != null) {
            val best = cands.minByOrNull { abs(it.first * 100.0 / it.second - pct) } ?: return null
            return if (abs(best.first * 100.0 / best.second - pct) <= 1.0) best else null
        }
        return if (ints.size == 2) ints.min() to ints.max() else null
    }

    fun parseAttendance(
        lines: List<OcrLine>,
        roll: String,
        headers: List<Pair<String, Int>>
    ): AttendanceResult? {
        val target = norm(roll)
        if (target.length < 3) return null
        // A short all-digit value is an S.No: match the cell in the first (left-most) column exactly.
        val serial = target.all { it.isDigit() } && target.length <= 3
        val rollLine = if (serial) {
            val minL = lines.minOfOrNull { it.box.left } ?: 0
            val maxR = lines.maxOfOrNull { it.box.right } ?: 1
            val edge = minL + (maxR - minL) * 0.18f
            lines.filter { it.box.left <= edge && norm(it.text) == target }
                .minByOrNull { it.box.left }
        } else lines.firstOrNull { norm(it.text).contains(target) }
        if (rollLine == null) return null
        val cy = rollLine.box.centerY()
        val tol = rollLine.box.height() * 0.7f
        val row = lines.filter { abs(it.box.centerY() - cy) <= tol }.sortedBy { it.box.left }

        // numeric tokens to the right of the roll number
        class Tok(val text: String, val x: Int)
        val toks = ArrayList<Tok>()
        var rollSeen = false
        for (l in row) {
            val parts = l.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            parts.forEachIndexed { idx, p ->
                val x = l.box.left + ((idx + 0.5f) / parts.size * l.box.width()).toInt()
                val isKey = if (serial) l === rollLine
                else norm(p).contains(target) || (parts.size == 1 && norm(l.text).contains(target))
                if (isKey) {
                    rollSeen = true
                    return@forEachIndexed
                }
                val clean = p.replace(',', '.')
                if (rollSeen && numberToken.matches(clean)) toks.add(Tok(clean, x))
            }
        }
        val rowText = row.joinToString("  ") { it.text.trim() }

        val pctTok = toks.lastOrNull { it.text.endsWith("%") || it.text.contains('.') }
        val pct = pctTok?.text?.removeSuffix("%")?.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }
        val intToks = toks.filter { !it.text.endsWith("%") && !it.text.contains('.') }

        // per subject, grouping numbers under the nearest header
        val perSubject = LinkedHashMap<String, Pair<Int, Int>>()
        val hdr = headers
        if (hdr.size >= 2) {
            val groups = HashMap<String, MutableList<Tok>>()
            for (t in toks) {
                val h = hdr.minByOrNull { abs(it.second - t.x) } ?: continue
                groups.getOrPut(h.first) { ArrayList() }.add(t)
            }
            for ((name, g) in groups) {
                val gp = g.lastOrNull { it.text.endsWith("%") || it.text.contains('.') }
                    ?.text?.removeSuffix("%")?.toDoubleOrNull()
                val gi = g.filter { !it.text.endsWith("%") && !it.text.contains('.') }
                    .mapNotNull { it.text.toIntOrNull() }
                val pr = pairFrom(gi, gp)
                if (pr != null) perSubject[name] = pr
            }
        }

        val overall = if (perSubject.isEmpty()) {
            pairFrom(intToks.mapNotNull { it.text.toIntOrNull() }.takeLast(4), pct)
        } else null

        return AttendanceResult(rowText, perSubject, overall, pct)
    }
}
