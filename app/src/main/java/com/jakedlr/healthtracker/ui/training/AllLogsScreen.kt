package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.jakedlr.healthtracker.ui.BackButton

/** Every log for one exercise, newest first; tap to edit or delete, or back-fill with Add log. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllLogsScreen(
    onBack: () -> Unit,
    onAddLog: (exerciseId: Long) -> Unit,
    onEditLog: (exerciseId: Long, logId: Long) -> Unit,
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.AllLogsFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val id = viewModel.exerciseId

    LaunchedEffect(state.loading, state.exercise) {
        if (!state.loading && state.exercise == null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.exercise?.let { "${it.name}: all logs" }.orEmpty()) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(onClick = { onAddLog(id) }, modifier = Modifier.fillMaxWidth()) { Text("Add log") }
            }
            if (!state.loading && state.logs.isEmpty()) {
                item { Text("No logs yet.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(state.logs, key = { it.id }) { log ->
                LogRow(log, state.today, onClick = { onEditLog(id, log.id) })
            }
        }
    }
}
