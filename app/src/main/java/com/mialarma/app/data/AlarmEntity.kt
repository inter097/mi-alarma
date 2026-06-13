package com.mialarma.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    /** URI del tono como String, o null para usar el tono de alarma predeterminado del sistema. */
    val soundUri: String? = null,
    val soundName: String = ""
) {
    val isRepeating: Boolean get() = repeatDays.isNotEmpty()
}
