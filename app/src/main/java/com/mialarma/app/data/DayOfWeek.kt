package com.mialarma.app.data

import java.util.Calendar

/**
 * Días de la semana ordenados de lunes a domingo (L-D) para mostrarlos en la UI,
 * mapeados a las constantes de [Calendar] para poder calcular la próxima ocurrencia.
 */
enum class DayOfWeek(val calendarDay: Int, val shortLabel: String) {
    MONDAY(Calendar.MONDAY, "L"),
    TUESDAY(Calendar.TUESDAY, "M"),
    WEDNESDAY(Calendar.WEDNESDAY, "X"),
    THURSDAY(Calendar.THURSDAY, "J"),
    FRIDAY(Calendar.FRIDAY, "V"),
    SATURDAY(Calendar.SATURDAY, "S"),
    SUNDAY(Calendar.SUNDAY, "D");

    companion object {
        val ORDERED = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)

        fun fromCalendarDay(calendarDay: Int): DayOfWeek =
            ORDERED.first { it.calendarDay == calendarDay }
    }
}
