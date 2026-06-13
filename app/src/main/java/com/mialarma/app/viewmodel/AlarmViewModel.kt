package com.mialarma.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mialarma.app.data.AlarmEntity
import com.mialarma.app.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(private val repository: AlarmRepository) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = repository.alarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(alarm, enabled) }
    }

    fun save(alarm: AlarmEntity) {
        viewModelScope.launch { repository.upsert(alarm) }
    }

    fun delete(alarm: AlarmEntity) {
        viewModelScope.launch { repository.delete(alarm) }
    }

    suspend fun getAlarm(id: Long): AlarmEntity? = repository.getAlarmById(id)

    companion object {
        fun factory(repository: AlarmRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AlarmViewModel(repository) }
        }
    }
}
