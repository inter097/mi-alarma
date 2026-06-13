package com.mialarma.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mialarma.app.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Se dispara cuando [android.app.AlarmManager] activa una alarma programada con
 * [AlarmScheduler]. Reprograma la siguiente ocurrencia (si la alarma es repetitiva)
 * y arranca [AlarmService] en primer plano para reproducir el sonido y mostrar la
 * pantalla de alarma.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                val alarm = dao.getAlarmById(alarmId) ?: return@launch

                val scheduler = AlarmScheduler(context)
                if (alarm.isRepeating) {
                    scheduler.schedule(alarm)
                } else {
                    dao.update(alarm.copy(enabled = false))
                }

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START_ALARM
                    putExtra(EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.mialarma.app.action.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
