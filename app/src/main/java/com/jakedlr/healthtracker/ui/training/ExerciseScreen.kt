package com.jakedlr.healthtracker.ui.training

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.RECENT_LOG_COUNT
import com.jakedlr.healthtracker.data.db.Exercise
import com.jakedlr.healthtracker.data.db.ExerciseLog
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSet
import com.jakedlr.healthtracker.domain.TopSets
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.ConfirmDeleteDialog
import com.jakedlr.healthtracker.ui.formatTopSets
import com.jakedlr.healthtracker.ui.label
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme
import java.time.LocalDate

/** One exercise: its latest logs, Add log (or Edit today's log), and All logs. */
@Composable
fun ExerciseScreen(
    onBack: () -> Unit,
    onEdit: (exerciseId: Long) -> Unit,
    onAddLog: (exerciseId: Long) -> Unit,
    onEditLog: (exerciseId: Long, logId: Long) -> Unit,
    onAllLogs: (exerciseId: Long) -> Unit,
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.RecentFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val confirmingDelete by viewModel.confirmingDelete.collectAsStateWithLifecycle()
    val id = viewModel.exerciseId

    LaunchedEffect(state.loading, state.exercise) {
        if (!state.loading && state.exercise == null) onBack()
    }

    ExerciseContent(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(id) },
        onDelete = viewModel::requestDelete,
        onAddOrEditToday = { state.todaysLog?.let { onEditLog(id, it.id) } ?: onAddLog(id) },
        onLogClick = { onEditLog(id, it) },
        onAllLogs = { onAllLogs(id) },
    )
    confirmingDelete?.let { count ->
        ConfirmDeleteDialog(
            title = "Delete ${state.exercise?.name.orEmpty()}?",
            text = when (count) {
                0 -> "It has no logs."
                1 -> "Its 1 log will be deleted too."
                else -> "Its $count logs will be deleted too."
            },
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseContent(
    state: ExerciseUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddOrEditToday: () -> Unit,
    onLogClick: (Long) -> Unit,
    onAllLogs: () -> Unit,
) {
    val exercise = state.exercise
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name.orEmpty()) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    TextButton(onClick = onEdit, enabled = exercise != null) { Text("Edit") }
                    TextButton(onClick = onDelete, enabled = exercise != null) { Text("Delete") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (exercise != null) {
                Text(
                    "${exercise.sidedness.label()} · default ${exercise.defaultUnit.label}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = onAddOrEditToday, enabled = exercise != null, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.todaysLog != null) "Edit today's log" else "Add log")
            }
            Text("Recent", style = MaterialTheme.typography.titleMedium)
            when {
                state.loading -> Unit
                state.logs.isEmpty() -> Text("No logs yet.", style = MaterialTheme.typography.bodyLarge)
                else -> state.logs.forEach { log -> LogRow(log, state.today, onClick = { onLogClick(log.id) }) }
            }
            OutlinedButton(onClick = onAllLogs, enabled = exercise != null, modifier = Modifier.fillMaxWidth()) {
                Text("All logs")
            }
            if (state.logs.size == RECENT_LOG_COUNT) {
                Text(
                    "Showing the latest $RECENT_LOG_COUNT. All logs has the full history.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** A log's date and top sets, e.g. "Today · 135 lbs × 8". Tapping opens it for editing. */
@Composable
fun LogRow(log: ExerciseLog, today: LocalDate, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (log.date == today) "Today" else log.date.format(DATE_FORMAT),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(formatTopSets(log.topSets, log.unit), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExercisePreview() {
    val today = LocalDate.of(2026, 10, 19)
    HealthTrackerTheme {
        ExerciseContent(
            state = ExerciseUiState(
                loading = false,
                exercise = Exercise(1, 1, "Lunge", Sidedness.SINGLE, LoadUnit.LBS, 0),
                logs = listOf(
                    ExerciseLog.of(1, today, LoadUnit.LBS, TopSets.PerSide(TopSet(20.0, 10), TopSet(22.5, 8)), id = 2),
                    ExerciseLog.of(1, today.minusDays(3), LoadUnit.LBS, TopSets.PerSide(null, TopSet(20.0, 8)), id = 1),
                ),
                today = today,
            ),
            onBack = {}, onEdit = {}, onDelete = {}, onAddOrEditToday = {}, onLogClick = {}, onAllLogs = {},
        )
    }
}
