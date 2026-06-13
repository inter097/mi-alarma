package com.mialarma.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.mialarma.app.MiAlarmaApplication
import com.mialarma.app.R
import com.mialarma.app.data.AlarmDatabase
import com.mialarma.app.data.AlarmEntity
import com.mialarma.app.ui.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que se mantiene activo mientras una alarma está sonando:
 * reproduce el tono en bucle, vibra y muestra una notificación de pantalla completa
 * que lanza [AlarmRingingActivity] incluso sobre la pantalla de bloqueo.
 */
class AlarmService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentAlarm: AlarmEntity? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
                if (alarmId == -1L) {
                    stopSelf()
                } else {
                    startAlarm(alarmId)
                }
            }
            ACTION_SNOOZE_ALARM -> snooze()
            ACTION_DISMISS_ALARM -> dismiss()
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun startAlarm(alarmId: Long) {
        serviceScope.launch {
            val alarm = AlarmDatabase.getInstance(applicationContext).alarmDao().getAlarmById(alarmId)
                ?: AlarmEntity(id = alarmId, hour = 0, minute = 0)
            currentAlarm = alarm

            val notification = buildNotification(alarm)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            playSound(alarm.soundUri)
            if (alarm.vibrate) startVibration()
        }
    }

    private fun playSound(soundUri: String?) {
        val uri: Uri = soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        raiseAlarmVolumeIfMuted()

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            } catch (e: Exception) {
                // Si el tono seleccionado ya no existe, usar el predeterminado del sistema.
                try {
                    reset()
                    setDataSource(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    isLooping = true
                    prepare()
                    start()
                } catch (_: Exception) {
                    // No se pudo reproducir ningún sonido; continuar solo con vibración/notificación.
                }
            }
        }
    }

    private fun raiseAlarmVolumeIfMuted() {
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        if (current == 0) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (max * 0.7).toInt().coerceAtLeast(1), 0)
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val pattern = longArrayOf(0, 800, 800)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun snooze() {
        currentAlarm?.let { alarm ->
            AlarmScheduler(applicationContext).scheduleSnooze(alarm, SNOOZE_MINUTES)
        }
        stopAlarmAndService()
    }

    private fun dismiss() {
        stopAlarmAndService()
    }

    private fun stopAlarmAndService() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(alarm: AlarmEntity): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_DISMISS + alarm.id.toInt(),
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_SNOOZE + alarm.id.toInt(),
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = alarm.label.ifBlank { getString(R.string.app_name) }
        val time = "%02d:%02d".format(alarm.hour, alarm.minute)

        return NotificationCompat.Builder(this, MiAlarmaApplication.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(title)
            .setContentText(time)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, getString(R.string.action_snooze), snoozePendingIntent)
            .addAction(0, getString(R.string.action_dismiss), dismissPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START_ALARM = "com.mialarma.app.action.START_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.mialarma.app.action.SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.mialarma.app.action.DISMISS_ALARM"

        const val SNOOZE_MINUTES = 10
        private const val NOTIFICATION_ID = 42
        private const val REQUEST_CODE_DISMISS = 200_000
        private const val REQUEST_CODE_SNOOZE = 300_000
    }
}
