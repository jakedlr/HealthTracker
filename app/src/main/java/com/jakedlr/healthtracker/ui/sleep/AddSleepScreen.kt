package com.jakedlr.healthtracker.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.db.SleepEntry
import com.jakedlr.healthtracker.domain.SleepError
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.OptionalDateTimeField
import com.jakedlr.healthtracker.ui.formatDuration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSleepScreen(
    onDone: () -> Unit,
    viewModel: AddSleepViewModel = viewModel(factory = AddSleepViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add sleep") }, navigationIcon = { BackButton(onDone) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Fell asleep", style = MaterialTheme.typography.labelLarge)
            OptionalDateTimeField(
                date = state.startDate,
                time = state.startTime,
                suggestedDate = state.today.minusDays(1),
                suggestedTime = LocalTime.of(23, 0),
                today = state.today,
                allowFutureDates = false,
                onDateChange = viewModel::onStartDateChange,
                onTimeChange = viewModel::onStartTimeChange,
            )

            // Suggest a wake time 8 hours after the chosen sleep time.
            val suggestedEnd = state.startDate?.let { d ->
                LocalDateTime.of(d, state.startTime ?: LocalTime.of(23, 0)).plusHours(8)
            } ?: LocalDateTime.of(state.today, LocalTime.of(7, 0))
            Text("Woke up", style = MaterialTheme.typography.labelLarge)
            OptionalDateTimeField(
                date = state.endDate,
                time = state.endTime,
                suggestedDate = suggestedEnd.toLocalDate(),
                suggestedTime = suggestedEnd.toLocalTime(),
                today = state.today,
                allowFutureDates = true,
                onDateChange = viewModel::onEndDateChange,
                onTimeChange = viewModel::onEndTimeChange,
            )

            val message = when {
                state.overlap != null -> "Overlaps sleep from ${formatRange(state.overlap!!)}"
                state.error == SleepError.END_NOT_AFTER_START -> "Wake time must be after sleep time"
                state.error == SleepError.TOO_LONG -> "Sleep can't be longer than 24 hours"
                state.showMissing && !state.isComplete -> "Choose both dates and times"
                else -> null
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (state.countsToward != null && state.startAt != null && state.endAt != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(formatDuration(state.endAt!! - state.startAt!!), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Counts toward " +
                                state.countsToward!!.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

private fun formatRange(entry: SleepEntry): String {
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    val start = Instant.ofEpochMilli(entry.startAt).atZone(zone).format(fmt)
    val end = Instant.ofEpochMilli(entry.endAt).atZone(zone).format(fmt)
    return "$start – $end"
}
