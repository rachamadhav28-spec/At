package com.attendtrack.telangana

object Calculator {
    const val TARGET = 75

    fun percent(attended: Int, held: Int): Double =
        if (held == 0) 0.0 else attended * 100.0 / held

    // Classes you must attend in a row to reach 75%
    fun needed(attended: Int, held: Int): Int =
        maxOf(0, 3 * held - 4 * attended)

    // Classes you can skip and still stay at 75% or above
    fun canSkip(attended: Int, held: Int): Int =
        maxOf(0, (4 * attended) / 3 - held)
}
