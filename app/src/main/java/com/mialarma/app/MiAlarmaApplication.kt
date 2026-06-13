package com.mialarma.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mialarma.app.alarm.AlarmScheduler
import com.mialarma.app.data.AlarmDatabase
import com.mialarma.app.data.AlarmRepository

class MiAlarmaApplication : Application() {

    val repository: AlarmRepository by lazy {
        AlarmRepository(
            AlarmDatabase.getInstance(this).alarmDao(),
            AlarmScheduler(this)
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        // Canal para la notificación a pantalla completa de la alarma sonando.
        // Sin sonido propio: el audio de la alarma lo reproduce AlarmService.
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.notification_channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_alarm_description)
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
