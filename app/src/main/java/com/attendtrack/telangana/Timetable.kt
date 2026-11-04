package com.attendtrack.telangana

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

object Timetable {
    const val MATHS = "Engineering Mathematics-I"
    const val CHEM = "Engineering Chemistry"
    const val ENG = "Communicative English"
    const val DLD = "DLD"
    const val CLAB = "Chemistry Lab"
    const val ELAB = "English Lab"
    const val GRAPH = "Engineering Graphics"

    // Periods: 10-11, 11-12, 12-1, 2-3, 3-4, 4-5 (null = free period)
    // Built-in timetable, effective from 26-10-2026 (CSE-A, B.Tech I-Sem)
    val defaultWeek: Map<DayOfWeek, List<String?>> = mapOf(
        DayOfWeek.MONDAY to listOf(ENG, DLD, MATHS, GRAPH, GRAPH, GRAPH),
        DayOfWeek.TUESDAY to listOf(CHEM, DLD, MATHS, DLD, null, null),
        DayOfWeek.WEDNESDAY to listOf(GRAPH, GRAPH, GRAPH, MATHS, null, null),
        DayOfWeek.THURSDAY to listOf(ELAB, ELAB, CHEM, CLAB, CLAB, CLAB),
        DayOfWeek.FRIDAY to listOf(ENG, ENG, CHEM, CLAB, CLAB, CLAB),
        DayOfWeek.SATURDAY to listOf(null, MATHS, MATHS, CLAB, CLAB, CLAB)
    )

    /** The timetable in use. Replaced when the user imports a PDF; kept in preferences. */
    var week: Map<DayOfWeek, List<String?>> by mutableStateOf(defaultWeek)
        private set

    val days = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    fun load(ctx: Context) {
        val text = ctx.getSharedPreferences("attendtrack", 0).getString("timetable_json", null)
        week = if (text == null) defaultWeek else runCatching { fromJson(text) }.getOrDefault(defaultWeek)
    }

    fun save(ctx: Context, newWeek: Map<DayOfWeek, List<String?>>) {
        week = newWeek
        ctx.getSharedPreferences("attendtrack", 0).edit()
            .putString("timetable_json", toJson(newWeek)).apply()
    }

    fun reset(ctx: Context) {
        week = defaultWeek
        ctx.getSharedPreferences("attendtrack", 0).edit().remove("timetable_json").apply()
    }

    private fun toJson(w: Map<DayOfWeek, List<String?>>): String {
        val o = JSONObject()
        for ((d, list) in w) {
            val a = JSONArray()
            list.forEach { a.put(it ?: JSONObject.NULL) }
            o.put(d.name, a)
        }
        return o.toString()
    }

    private fun fromJson(text: String): Map<DayOfWeek, List<String?>> {
        val o = JSONObject(text)
        val out = LinkedHashMap<DayOfWeek, List<String?>>()
        for (d in days) {
            val a = o.optJSONArray(d.name)
            out[d] = (0 until 6).map { i ->
                if (a == null || i >= a.length() || a.isNull(i)) null else a.getString(i)
            }
        }
        return out
    }
}
