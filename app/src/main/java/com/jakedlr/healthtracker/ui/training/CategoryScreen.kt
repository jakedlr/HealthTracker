package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.jakedlr.healthtracker.data.db.Exercise
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import com.jakedlr.healthtracker.data.db.ExerciseWithLastLogged
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.ConfirmDeleteDialog
import com.jakedlr.healthtracker.ui.label
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One exercise category: its exercises in the user's order, each with its last logged date. */
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onOpenExercise: (exerciseId: Long) -> Unit,
    onNewExercise: (categoryId: Long) -> Unit,
    viewModel: CategoryViewModel = viewModel(factory = CategoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rename by viewModel.rename.collectAsStateWithLifecycle()
    val deleteDialog by viewModel.deleteDialog.collectAsStateWithLifecycle()

    // Deleted (here or elsewhere): nothing left to show.
    LaunchedEffect(state.loading, state.category) {
        if (!state.loading && state.category == null) onBack()
    }

    CategoryContent(
        state = state,
        onBack = onBack,
        onOpenExercise = onOpenExercise,
        onNewExercise = { onNewExercise(viewModel.categoryId) },
        onRename = viewModel::openRename,
        onDelete = viewModel::requestDelete,
        onReorder = viewModel::reorder,
    )
    rename?.let {
        CategoryNameDialog(
            title = "Rename category",
            state = it,
            onTextChange = viewModel::onRenameText,
            onConfirm = viewModel::saveRename,
            onDismiss = viewModel::dismissRename,
        )
    }
    when (deleteDialog) {
        CategoryDeleteDialog.CONFIRM -> ConfirmDeleteDialog(
            title = "Delete ${state.category?.name.orEmpty()}?",
            text = "This category has no exercises.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
        CategoryDeleteDialog.NOT_EMPTY -> AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Can't delete yet") },
            text = { Text("Move or delete this category's exercises first, so no exercise history is lost.") },
            confirmButton = { TextButton(onClick = viewModel::dismissDelete) { Text("OK") } },
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryContent(
    state: CategoryUiState,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onNewExercise: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onReorder: (List<ExerciseWithLastLogged>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.category?.name.orEmpty()) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    TextButton(onClick = onRename, enabled = state.category != null) { Text("Rename") }
                    TextButton(onClick = onDelete, enabled = state.category != null) { Text("Delete") }
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
            Button(onClick = onNewExercise, modifier = Modifier.fillMaxWidth()) { Text("New exercise") }
            when {
                state.loading -> Unit
                state.exercises.isEmpty() -> Text(
                    "No exercises in this category yet.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> ReorderableColumn(
                    items = state.exercises,
                    key = { it.exercise.id },
                    onReorder = onReorder,
                ) { row, dragging ->
                    ReorderableCard(dragging = dragging, onClick = { onOpenExercise(row.exercise.id) }) {
                        Column {
                            Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                row.lastLogged?.let { "Last logged ${it.format(DATE_FORMAT)}" } ?: "Never logged",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${row.exercise.sidedness.label()} · ${row.exercise.defaultUnit.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Preview(showBackground = true)
@Composable
private fun CategoryPreview() {
    HealthTrackerTheme {
        CategoryContent(
            state = CategoryUiState(
                loading = false,
                category = ExerciseCategory(1, "Leg day", 0),
                exercises = listOf(
                    ExerciseWithLastLogged(Exercise(1, 1, "Squat", Sidedness.DOUBLE, LoadUnit.LBS, 0), LocalDate.of(2026, 10, 15)),
                    ExerciseWithLastLogged(Exercise(2, 1, "Lunge", Sidedness.SINGLE, LoadUnit.KG, 1), null),
                ),
            ),
            onBack = {}, onOpenExercise = {}, onNewExercise = {}, onRename = {}, onDelete = {}, onReorder = {},
        )
    }
}
