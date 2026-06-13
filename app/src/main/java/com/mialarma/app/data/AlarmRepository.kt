package com.mialarma.app.data

import com.mialarma.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Punto único de acceso a las alarmas: persiste en Room y sincroniza la programación
 * con [AlarmScheduler] cada vez que se crea, actualiza, elimina o (des)activa una alarma.
 */
class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) {

    val alarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    /** Inserta o actualiza la alarma y (re)programa o cancela su disparo según corresponda. */
    suspend fun upsert(alarm: AlarmEntity) {
        if (alarm.id == 0L) {
            val newId = alarmDao.insert(alarm)
            alarmScheduler.schedule(alarm.copy(id = newId))
        } else {
            alarmDao.update(alarm)
            alarmScheduler.schedule(alarm)
        }
    }

    suspend fun delete(alarm: AlarmEntity) {
        alarmScheduler.cancel(alarm)
        alarmDao.delete(alarm)
    }

    suspend fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        val updated = alarm.copy(enabled = enabled)
        alarmDao.update(updated)
        alarmScheduler.schedule(updated)
    }

    /** Reprograma todas las alarmas activas, usado tras un reinicio del dispositivo. */
    suspend fun rescheduleAll() {
        alarmDao.getEnabledAlarms().forEach { alarmScheduler.schedule(it) }
    }
}
