package com.attendtrack.telangana

import java.time.DayOfWeek

object Timetable {
    private const val MATHS = "Engineering Mathematics-I"
    private const val CHEM = "Engineering Chemistry"
    private const val ENG = "Communicative English"
    private const val DLD = "DLD"
    private const val CLAB = "Chemistry Lab"
    private const val ELAB = "English Lab"
    private const val GRAPH = "Engineering Graphics"

    // Periods: 10-11, 11-12, 12-1, 2-3, 3-4, 4-5 (null = free period)
    // New timetable, effective from 26-10-2026
    val week: Map<DayOfWeek, List<String?>> = mapOf(
        DayOfWeek.MONDAY to listOf(ENG, DLD, MATHS, GRAPH, GRAPH, GRAPH),
        DayOfWeek.TUESDAY to listOf(CHEM, DLD, MATHS, DLD, null, null),
        DayOfWeek.WEDNESDAY to listOf(GRAPH, GRAPH, GRAPH, MATHS, null, null),
        DayOfWeek.THURSDAY to listOf(ELAB, ELAB, CHEM, CLAB, CLAB, CLAB),
        DayOfWeek.FRIDAY to listOf(ENG, ENG, CHEM, CLAB, CLAB, CLAB),
        DayOfWeek.SATURDAY to listOf(null, MATHS, MATHS, CLAB, CLAB, CLAB)
    )
}
