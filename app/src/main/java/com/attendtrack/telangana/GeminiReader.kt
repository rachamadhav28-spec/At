package com.attendtrack.telangana

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Reads the attendance PDF with Google's Gemini model and returns the student's numbers. */
object GeminiReader {
    private val models = listOf("gemini-flash-latest", "gemini-3.8-flash", "gemini-flash-lite-latest")

    private val keys = listOf(
        "maths" to Timetable.MATHS,
        "chem" to Timetable.CHEM,
        "comm_english" to Timetable.ENG,
        "dld" to Timetable.DLD,
        "chem_lab" to Timetable.CLAB,
        "english_lab" to Timetable.ELAB,
        "eng_graphics_lab" to Timetable.GRAPH
    )

    internal fun prompt(serial: String) = """
This PDF is a class attendance sheet. Each page has a header row, then a row "No of Classes" that gives the
classes held per column, then one row per student: S.No, Roll No, Name, one attended-count per subject, Total, %.
The sheet may contain several sections (e.g. CSE-A then CSE-B) that each restart S.No at 1. Use the FIRST
section in which S.No $serial appears.

Find the student whose S.No is $serial. Subject columns (header text -> key):
Eng Maths-I -> maths; Eng Chem -> chem; Comm English -> comm_english; DLD -> dld; Chem Lab -> chem_lab;
ENGLISH Lab -> english_lab; Eng. Grap Lab -> eng_graphics_lab.
For each subject return attended (the student's number) and held (the "No of Classes" value of that column
on the same page). Also return the Total column (attended and held) and the % value.

Reply with ONLY this JSON, integers for counts, null if not found:
{"found":true,"name":"","attended":{"maths":0,"chem":0,"comm_english":0,"dld":0,"chem_lab":0,"english_lab":0,"eng_graphics_lab":0},
"held":{"maths":0,"chem":0,"comm_english":0,"dld":0,"chem_lab":0,"english_lab":0,"eng_graphics_lab":0},
"total_attended":0,"total_held":0,"percent":0.0}
If the S.No does not exist reply {"found":false}.
""".trimIndent()

    suspend fun readAttendance(ctx: Context, uri: Uri, serial: String): Outcome<AttendanceResult> =
        withContext(Dispatchers.IO) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank()) {
                return@withContext Outcome<AttendanceResult>(
                    null,
                    "This build has no Gemini key. Add the GEMINI_API_KEY secret on GitHub and rebuild."
                )
            }
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Outcome<AttendanceResult>(null, "Could not open the file.")
                if (bytes.size > 15_000_000) {
                    return@withContext Outcome<AttendanceResult>(null, "PDF is too large (over 15 MB).")
                }
                val body = JSONObject()
                    .put(
                        "contents", JSONArray().put(
                            JSONObject().put(
                                "parts", JSONArray()
                                    .put(JSONObject().put("text", prompt(serial)))
                                    .put(
                                        JSONObject().put(
                                            "inline_data", JSONObject()
                                                .put("mime_type", "application/pdf")
                                                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                                        )
                                    )
                            )
                        )
                    )
                    .put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0))

                // Google sometimes answers "high demand" (503) or "slow down" (429): retry, then try a lighter model.
                var code = 0
                var text = ""
                outer@ for (model in models) {
                    for (attempt in 0 until 2) {
                        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                            .openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.connectTimeout = 20000
                        conn.readTimeout = 90000
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("x-goog-api-key", key)
                        conn.outputStream.use { it.write(body.toString().toByteArray()) }
                        code = conn.responseCode
                        text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                            ?.bufferedReader()?.use { it.readText() } ?: ""
                        if (code in 200..299) break@outer
                        if (code != 503 && code != 429 && code != 500) break@outer
                        kotlinx.coroutines.delay(1500L * (attempt + 1))
                    }
                }
                if (code !in 200..299) {
                    val reason = runCatching { JSONObject(text).getJSONObject("error").getString("message") }
                        .getOrDefault("HTTP $code")
                    return@withContext Outcome<AttendanceResult>(null, "Gemini error: $reason")
                }
                val out = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                parse(JSONObject(out), serial)
            } catch (e: java.io.IOException) {
                Outcome(null, "No internet connection, or Gemini could not be reached.")
            } catch (e: Exception) {
                Outcome(null, "Could not understand the AI reply. Try again.")
            }
        }

    internal fun parse(o: JSONObject, serial: String): Outcome<AttendanceResult> {
        if (!o.optBoolean("found", false)) {
            return Outcome(null, "S.No $serial was not found in this PDF.")
        }
        val att = o.optJSONObject("attended")
        val held = o.optJSONObject("held")
        val per = LinkedHashMap<String, Pair<Int, Int>>()
        for ((k, name) in keys) {
            if (att == null || held == null || att.isNull(k) || held.isNull(k)) continue
            val a = att.optInt(k, -1)
            val h = held.optInt(k, -1)
            if (a >= 0 && h > 0 && a <= h) per[name] = a to h
        }
        var overall: Pair<Int, Int>? = null
        if (!o.isNull("total_attended") && !o.isNull("total_held")) {
            val a = o.optInt("total_attended", -1)
            val h = o.optInt("total_held", -1)
            if (a >= 0 && h > 0 && a <= h) overall = a to h
        }
        val pct = if (o.isNull("percent")) null else o.optDouble("percent").takeIf { !it.isNaN() }
        return Outcome(AttendanceResult(o.optString("name"), per, overall, pct), null)
    }
}
