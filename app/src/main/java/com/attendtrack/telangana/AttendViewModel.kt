package com.attendtrack.telangana

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AttendViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val dao = db.dao()

    val selectedDate = MutableStateFlow(LocalDate.now().toString())

    val subjects = dao.all().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val holidays = dao.holidays().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val records = selectedDate.flatMapLatest { dao.forDate(it) }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        viewModelScope.launch {
            if (dao.count() == 0) {
                dao.insertAll(
                    listOf(
                        "Engineering Mathematics-I",
                        "Engineering Chemistry",
                        "Communicative English",
                        "DLD",
                        "Chemistry Lab",
                        "English Lab",
                        "Engineering Graphics"
                    ).map { Subject(name = it) }
                )
            }
            if (dao.holidayCount() == 0) {
                dao.insertHolidays(
                    listOf(
                        Holiday(
                            name = "Dussehra Vacation",
                            startDate = "2026-10-12",
                            endDate = "2026-10-22",
                            provisional = false
                        ),
                        Holiday(
                            name = "Preparation Holidays & Practical Exams",
                            startDate = "2026-12-06",
                            endDate = "2026-12-13",
                            provisional = false
                        )
                    )
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>()
                .getSharedPreferences("attendtrack", 0)
            if (prefs.getBoolean("holidays_v2", false)) return@launch
            fun h(n: String, s: String, e: String = s) =
                Holiday(name = n, startDate = s, endDate = e, provisional = true)
            val have = dao.allHolidays().map { it.startDate }.toSet()
            val list = listOf(
                h("Deepavali (Diwali)", "2026-11-08"),
                h("Guru Nanak Jayanthi / Kartika Purnima", "2026-11-24"),
                h("Christmas", "2026-12-25"),
                h("New Year's Day", "2027-01-01"),
                h("Makara Sankranti / Pongal Vacation", "2027-01-13", "2027-01-17"),
                h("Republic Day", "2027-01-26"),
                h("Maha Shivaratri", "2027-03-06"),
                h("Id-ul-Fitr (Ramzan), tentative", "2027-03-10"),
                h("Holi", "2027-03-23"),
                h("Good Friday", "2027-03-26"),
                h("Ugadi", "2027-04-07"),
                h("Dr. B. R. Ambedkar Jayanti", "2027-04-14"),
                h("Sri Rama Navami", "2027-04-15"),
                h("Id-ul-Zuha (Bakrid), tentative", "2027-05-17"),
                h("Telangana Formation Day", "2027-06-02")
            ).filter { it.startDate !in have }
            dao.insertHolidays(list)
            prefs.edit().putBoolean("holidays_v2", true).apply()
        }
    }    fun holidayOn(date: String): Holiday? =
        holidays.value.firstOrNull { date >= it.startDate && date <= it.endDate }

    fun setDate(date: String) {
        selectedDate.value = date
    }

    fun markPeriod(subjectName: String, period: Int, present: Boolean) {
        viewModelScope.launch {
            val date = selectedDate.value
            if (holidayOn(date) != null) return@launch
            if (LocalDate.parse(date).isAfter(LocalDate.now())) return@launch
            val s = subjects.value.firstOrNull { it.name == subjectName } ?: return@launch
            val old = dao.find(date, period)
            val cur = dao.getSubject(s.id)
            val p = if (present) 1 else 0
            var undo = false
            when {
                old == null -> {
                    dao.insertRecord(
                        AttRecord(subjectId = s.id, date = date, period = period, present = present)
                    )
                    dao.update(cur.copy(attended = cur.attended + p, held = cur.held + 1))
                }
                old.present == present -> {
                    dao.deleteRecord(old)
                    dao.update(cur.copy(attended = cur.attended - p, held = cur.held - 1))
                    undo = true
                }
                else -> {
                    dao.updateRecord(old.copy(present = present))
                    dao.update(cur.copy(attended = cur.attended + if (present) 1 else -1))
                }
            }
            if (!undo) {
                val all = dao.allSubjects()
                val a = all.sumOf { it.attended }
                val h = all.sumOf { it.held }
                val pct = if (h == 0) 0.0 else a * 100.0 / h
                Sounds.play(getApplication<Application>(), present, pct)
            }
        }
    }

        fun markAllPresent() {
        viewModelScope.launch {
            val date = selectedDate.value
            if (holidayOn(date) != null) return@launch
            val day = LocalDate.parse(date)
            if (day.isAfter(LocalDate.now())) return@launch
            val names = Timetable.week[day.dayOfWeek] ?: return@launch
            names.forEachIndexed { i, name ->
                if (name != null && dao.find(date, i + 1) == null) {
                    val s = subjects.value.firstOrNull { it.name == name }
                    if (s != null) {
                        val cur = dao.getSubject(s.id)
                        dao.insertRecord(
                            AttRecord(subjectId = s.id, date = date, period = i + 1, present = true)
                        )
                        dao.update(cur.copy(attended = cur.attended + 1, held = cur.held + 1))
                    }
                }
            }
            val all = dao.allSubjects()
            val a = all.sumOf { it.attended }
            val h = all.sumOf { it.held }
            Sounds.play(getApplication<Application>(), true, if (h == 0) 0.0 else a * 100.0 / h)
        }
    }

    fun edit(s: Subject, name: String, attended: Int, held: Int) {
        viewModelScope.launch {
            dao.update(s.copy(name = name, attended = attended, held = held))
        }
    }

    suspend fun buildCsv(): String {
        val names = subjects.value.associate { it.id to it.name }
        val sb = StringBuilder("date,day,period,subject,status\n")
        for (r in dao.allRecords()) {
            val day = LocalDate.parse(r.date).dayOfWeek
            val status = if (r.present) "Present" else "Absent"
            sb.append("${r.date},$day,${r.period},${names[r.subjectId] ?: ""},$status\n")
        }
        return sb.toString()
    }

    suspend fun buildBackup(): String {
        val root = JSONObject()
        root.put("version", 1)
        val subs = JSONArray()
        for (s in dao.allSubjects()) {
            subs.put(
                JSONObject().put("id", s.id).put("name", s.name)
                    .put("attended", s.attended).put("held", s.held)
            )
        }
        val recs = JSONArray()
        for (r in dao.allRecords()) {
            recs.put(
                JSONObject().put("id", r.id).put("subjectId", r.subjectId)
                    .put("date", r.date).put("period", r.period).put("present", r.present)
            )
        }
        val hols = JSONArray()
        for (h in dao.allHolidays()) {
            hols.put(
                JSONObject().put("id", h.id).put("name", h.name)
                    .put("startDate", h.startDate).put("endDate", h.endDate)
                    .put("provisional", h.provisional)
            )
        }
        root.put("subjects", subs)
        root.put("records", recs)
        root.put("holidays", hols)
        return root.toString()
    }

    suspend fun restoreBackup(text: String): Boolean {
        return try {
            val root = JSONObject(text)
            val sa = root.getJSONArray("subjects")
            val ra = root.getJSONArray("records")
            val ha = root.getJSONArray("holidays")
            val subs = (0 until sa.length()).map {
                val o = sa.getJSONObject(it)
                Subject(o.getInt("id"), o.getString("name"), o.getInt("attended"), o.getInt("held"))
            }
            val recs = (0 until ra.length()).map {
                val o = ra.getJSONObject(it)
                AttRecord(
                    o.getInt("id"), o.getInt("subjectId"), o.getString("date"),
                    o.getInt("period"), o.getBoolean("present")
                )
            }
            val hols = (0 until ha.length()).map {
                val o = ha.getJSONObject(it)
                Holiday(
                    o.getInt("id"), o.getString("name"), o.getString("startDate"),
                    o.getString("endDate"), o.getBoolean("provisional")
                )
            }
            db.withTransaction {
                dao.clearRecords()
                dao.clearSubjects()
                dao.clearHolidays()
                dao.insertAll(subs)
                dao.insertRecords(recs)
                dao.insertHolidays(hols)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addHoliday(name: String, start: String, end: String) {
        viewModelScope.launch {
            dao.insertHoliday(
                Holiday(name = name, startDate = start, endDate = end, provisional = true)
            )
        }
    }

    fun deleteHoliday(h: Holiday) {
        viewModelScope.launch { dao.deleteHoliday(h) }
    }
}
