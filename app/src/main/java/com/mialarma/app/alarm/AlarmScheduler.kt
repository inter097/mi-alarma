package com.mialarma.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mialarma.app.data.AlarmEntity
import com.mialarma.app.ui.MainActivity
import java.util.Calendar

/**
 * Programa y cancela alarmas usando [AlarmManager.setAlarmClock], lo que garantiza
 * disparos exactos exentos de las restricciones de Doze/App Standby y hace que el
 * sistema muestre el icono de alarma próxima en la barra de estado.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        ContextCompat.getSystemService(context, AlarmManager::class.java)!!

    fun canScheduleExactAlarms(): Boolean = alarmManager.canScheduleExactAlarms()

    /** Programa la próxima ocurrencia de la alarma. Si está deshabilitada, la cancela. */
    fun schedule(alarm: AlarmEntity) {
        if (!alarm.enabled) {
            cancel(alarm)
            return
        }
        val triggerAtMillis = nextTriggerTimeMillis(alarm)
        val showIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OFFSET_SHOW + alarm.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            triggerIntent
        )
    }

    /** Programa una alarma de posposición (snooze) que sonará dentro de [minutes] minutos. */
    fun scheduleSnooze(alarm: AlarmEntity, minutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        val showIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OFFSET_SHOW + alarm.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            triggerIntent
        )
    }

    fun cancel(alarm: AlarmEntity) {
        val triggerIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(triggerIntent)
    }

    /**
     * Calcula el siguiente instante (en milisegundos desde la época) en el que debe sonar
     * la alarma. Si no tiene días de repetición, es la próxima vez que ocurra esa hora
     * (hoy o mañana). Si tiene días de repetición, es la próxima ocurrencia entre hoy y
     * los 7 días siguientes que coincida con un día seleccionado.
     */
    fun nextTriggerTimeMillis(alarm: AlarmEntity, from: Long = System.currentTimeMillis()): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            if (target.timeInMillis <= from) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        val targetCalendarDays = alarm.repeatDays.map { it.calendarDay }.toSet()
        for (dayOffset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in targetCalendarDays &&
                candidate.timeInMillis > from
            ) {
                return candidate.timeInMillis
            }
        }
        // No debería ocurrir: repeatDays no está vacío, así que en 7 días hay coincidencia.
        return target.timeInMillis
    }

    companion object {
        private const val REQUEST_CODE_OFFSET_SHOW = 100_000
    }
}
