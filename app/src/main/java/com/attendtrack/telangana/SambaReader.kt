package com.attendtrack.telangana

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the attendance PDF with a SambaNova model. These models only read text, so each page is first
 * turned into text rows by the on-device text reader, then the model picks out the student's row.
 */
object SambaReader {
    private val models = listOf("Meta-Llama-3.3-70B-Instruct", "gpt-oss-120b", "DeepSeek-V3.1")

    suspend fun readAttendance(ctx: Context, uri: Uri, serial: String): Outcome<AttendanceResult> {
        val key = BuildConfig.SAMBANOVA_API_KEY
        if (key.isBlank()) {
            return Outcome(null, "This build has no SambaNova key. Add the SAMBANOVA_API_KEY secret on GitHub and rebuild.")
        }
        val rows = PdfImport.readPageRows(ctx, uri, 6)
        val pages = rows.value ?: return Outcome(null, rows.error ?: "Could not read the PDF")
        val sheet = pages.joinToString("\n\n").take(30000)
        val prompt = GeminiReader.prompt(serial).replace(
            "This PDF is a class attendance sheet.",
            "Below is a class attendance sheet read by OCR (one table row per line, columns separated by spaces; " +
                "OCR may slightly misread characters, so use the column order)."
        ) + "\n\nSHEET TEXT:\n" + sheet
        return withContext(Dispatchers.IO) {
            try {
                val body = { model: String ->
                    JSONObject()
                        .put("model", model)
                        .put("temperature", 0)
                        .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                        .toString()
                }
                var code = 0
                var text = ""
                outer@ for (model in models) {
                    for (attempt in 0 until 2) {
                        val conn = URL("https://api.sambanova.ai/v1/chat/completions")
                            .openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.connectTimeout = 20000
                        conn.readTimeout = 90000
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $key")
                        conn.outputStream.use { it.write(body(model).toByteArray()) }
                        code = conn.responseCode
                        text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                            ?.bufferedReader()?.use { it.readText() } ?: ""
                        if (code in 200..299) break@outer
                        if (code == 404 || code == 400) continue@outer // model not available: try the next
                        if (code != 429 && code != 503 && code != 500) break@outer
                        delay(1500L * (attempt + 1))
                    }
                }
                if (code !in 200..299) {
                    val reason = runCatching {
                        val e = JSONObject(text).opt("error")
                        if (e is JSONObject) e.getString("message") else e.toString()
                    }.getOrDefault("HTTP $code")
                    return@withContext Outcome<AttendanceResult>(null, "SambaNova error: $reason")
                }
                val reply = JSONObject(text).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
                val json = reply.substring(reply.indexOf('{'), reply.lastIndexOf('}') + 1)
                GeminiReader.parse(JSONObject(json), serial)
            } catch (e: java.io.IOException) {
                Outcome(null, "No internet connection, or SambaNova could not be reached.")
            } catch (e: Exception) {
                Outcome(null, "Could not understand the AI reply. Try again.")
            }
        }
    }
}
