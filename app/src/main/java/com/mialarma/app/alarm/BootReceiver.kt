package com.mialarma.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mialarma.app.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reprograma todas las alarmas activas cuando el dispositivo se reinicia, ya que
 * [android.app.AlarmManager] no conserva las alarmas programadas tras un reinicio.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                val scheduler = AlarmScheduler(context)
                dao.getEnabledAlarms().forEach { alarm -> scheduler.schedule(alarm) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
