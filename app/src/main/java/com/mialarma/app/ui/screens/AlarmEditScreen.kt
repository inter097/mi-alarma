package com.mialarma.app.ui.screens

import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mialarma.app.R
import com.mialarma.app.data.AlarmEntity
import com.mialarma.app.data.DayOfWeek
import com.mialarma.app.viewmodel.AlarmViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    viewModel: AlarmViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val isNew = alarmId <= 0L

    val now = remember { Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )

    var label by rememberSaveable { mutableStateOf("") }
    var repeatDays by rememberSaveable { mutableStateOf(setOf<DayOfWeek>()) }
    var vibrate by rememberSaveable { mutableStateOf(true) }
    var soundUri by rememberSaveable { mutableStateOf<String?>(null) }
    var soundName by rememberSaveable { mutableStateOf(context.getString(R.string.default_sound_name)) }
    var originalAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(alarmId) {
        if (!isNew) {
            val existing = viewModel.getAlarm(alarmId)
            if (existing != null) {
                originalAlarm = existing
                timePickerState.hour = existing.hour
                timePickerState.minute = existing.minute
                label = existing.label
                repeatDays = existing.repeatDays
                vibrate = existing.vibrate
                soundUri = existing.soundUri
                soundName = existing.soundName.ifBlank { context.getString(R.string.default_sound_name) }
            }
        }
        loaded = true
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            soundUri = uri.toString()
            soundName = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                ?: context.getString(R.string.default_sound_name)
        }
    }

    fun launchRingtonePicker() {
        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            )
            val existingUri = soundUri?.let { android.net.Uri.parse(it) }
                ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.select_ringtone))
        }
        ringtonePickerLauncher.launch(intent)
    }

    fun buildAlarm(): AlarmEntity = (originalAlarm ?: AlarmEntity(hour = 0, minute = 0)).copy(
        hour = timePickerState.hour,
        minute = timePickerState.minute,
        label = label.trim(),
        repeatDays = repeatDays,
        vibrate = vibrate,
        soundUri = soundUri,
        soundName = if (soundUri == null) "" else soundName,
        enabled = originalAlarm?.enabled ?: true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_alarm_title else R.string.edit_alarm_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    IconButton(onClick = {
                        viewModel.save(buildAlarm())
                        onDone()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) return@Scaffold

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.label_field_label)) },
                placeholder = { Text(stringResource(R.string.label_field_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.repeat_section_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayOfWeek.ORDERED.forEach { day ->
                    val selected = day in repeatDays
                    FilterChip(
                        selected = selected,
                        onClick = {
                            repeatDays = if (selected) repeatDays - day else repeatDays + day
                        },
                        label = { Text(day.shortLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vibrate_section_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.sound_section_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = soundName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { launchRingtonePicker() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.select_ringtone))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_alarm_confirm_title)) },
            text = { Text(stringResource(R.string.delete_alarm_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    originalAlarm?.let { viewModel.delete(it) }
                    showDeleteDialog = false
                    onDone()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
