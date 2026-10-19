package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.db.Exercise
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.MAX_LOAD
import com.jakedlr.healthtracker.domain.MAX_REPS
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.parseLoad
import com.jakedlr.healthtracker.domain.parseReps
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.ConfirmDeleteDialog
import com.jakedlr.healthtracker.ui.DeleteButton
import com.jakedlr.healthtracker.ui.PastDatePickerDialog
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme
import java.time.LocalDate

@Composable
fun ExerciseLogFormScreen(
    onDone: () -> Unit,
    viewModel: ExerciseLogFormViewModel = viewModel(factory = ExerciseLogFormViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }
    ExerciseLogFormContent(
        state = state,
        onBack = onDone,
        onDateChange = viewModel::onDateChange,
        onUnitChange = viewModel::onUnitChange,
        onDoubleChange = viewModel::onDoubleChange,
        onLeftChange = viewModel::onLeftChange,
        onRightChange = viewModel::onRightChange,
        onSave = viewModel::save,
        onDelete = viewModel::requestDelete,
    )
    if (state.confirmingDelete) {
        ConfirmDeleteDialog(
            title = "Delete this log?",
            text = "The ${state.exercise?.name.orEmpty()} log for " +
                "${(state.deleteTarget?.date ?: state.date).format(DATE_FORMAT)} will be removed.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseLogFormContent(
    state: ExerciseLogFormUiState,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onUnitChange: (LoadUnit) -> Unit,
    onDoubleChange: (TopSetInput) -> Unit,
    onLeftChange: (TopSetInput) -> Unit,
    onRightChange: (TopSetInput) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing != null) "Edit log" else "Add log") },
                navigationIcon = { BackButton(onBack) },
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
            state.exercise?.let { Text(it.name, style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                val label = state.date.format(DATE_FORMAT)
                Text(if (state.date == state.today) "Today, $label" else label)
            }
            if (state.replacesAnotherLog) {
                Text(
                    "This date already has a log. Saving will replace it.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ChoiceRow(
                options = LoadUnit.entries,
                selected = state.unit,
                label = { it.label },
                onSelect = onUnitChange,
            )
            when (state.exercise?.sidedness) {
                Sidedness.DOUBLE -> TopSetFields(state.double, state.unit, state.showErrors, onDoubleChange)
                Sidedness.SINGLE -> {
                    SideFields("Left", state.left, state.unit, state.showErrors, onLeftChange)
                    SideFields("Right", state.right, state.unit, state.showErrors, onRightChange)
                    if (state.showErrors && state.bothSidesSkipped) {
                        Text(
                            "Log at least one side",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                null -> Unit
            }
            Button(onClick = onSave, enabled = state.exercise != null, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
            if (state.deleteTarget != null) DeleteButton("Delete log", onDelete)
        }
    }

    if (showDatePicker) {
        PastDatePickerDialog(
            selected = state.date,
            today = state.today,
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun SideFields(
    label: String,
    input: TopSetInput,
    unit: LoadUnit,
    showErrors: Boolean,
    onChange: (TopSetInput) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Checkbox(checked = input.skipped, onCheckedChange = { onChange(input.copy(skipped = it)) })
        Text("Skip this side", style = MaterialTheme.typography.bodyMedium)
    }
    if (!input.skipped) TopSetFields(input, unit, showErrors, onChange)
}

@Composable
private fun TopSetFields(input: TopSetInput, unit: LoadUnit, showErrors: Boolean, onChange: (TopSetInput) -> Unit) {
    val loadError = showErrors && parseLoad(input.load) == null
    val repsError = showErrors && parseReps(input.reps) == null
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = input.load,
            onValueChange = { onChange(input.copy(load = it)) },
            label = { Text("Load (${unit.label})") },
            singleLine = true,
            isError = loadError,
            supportingText = { if (loadError) Text("0 to ${MAX_LOAD.toInt()}; 0 = bodyweight") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = input.reps,
            onValueChange = { onChange(input.copy(reps = it)) },
            label = { Text("Reps") },
            singleLine = true,
            isError = repsError,
            supportingText = { if (repsError) Text("Whole number, 1 to $MAX_REPS") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseLogFormPreview() {
    val today = LocalDate.of(2026, 10, 19)
    HealthTrackerTheme {
        ExerciseLogFormContent(
            state = ExerciseLogFormUiState(
                loading = false,
                exercise = Exercise(1, 1, "Lunge", Sidedness.SINGLE, LoadUnit.LBS, 0),
                today = today,
                date = today,
                left = TopSetInput("20", "10"),
                right = TopSetInput(skipped = true),
            ),
            onBack = {}, onDateChange = {}, onUnitChange = {}, onDoubleChange = {},
            onLeftChange = {}, onRightChange = {}, onSave = {}, onDelete = {},
        )
    }
}
