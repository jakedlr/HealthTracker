package com.jakedlr.healthtracker.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date and time buttons that open pickers. Dates after [today] can't be chosen. */
@Composable
fun DateTimeField(
    value: LocalDateTime,
    today: LocalDate,
    onValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
            val label = value.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            Text(if (value.toLocalDate() == today) "Today" else label)
        }
        OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
            Text(value.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
        }
    }

    if (showDate) {
        PastDatePickerDialog(
            selected = value.toLocalDate(),
            today = today,
            onConfirm = {
                onValueChange(LocalDateTime.of(it, value.toLocalTime()))
                showDate = false
            },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        TimePickerDialog(
            initial = value.toLocalTime(),
            onConfirm = {
                onValueChange(LocalDateTime.of(value.toLocalDate(), it))
                showTime = false
            },
            onDismiss = { showTime = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}
