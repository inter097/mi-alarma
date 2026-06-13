package com.mialarma.app.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromDaySet(days: Set<DayOfWeek>): String =
        days.joinToString(separator = ",") { it.calendarDay.toString() }

    @TypeConverter
    fun toDaySet(value: String): Set<DayOfWeek> =
        if (value.isBlank()) {
            emptySet()
        } else {
            value.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { DayOfWeek.fromCalendarDay(it) }
                .toSet()
        }
}
