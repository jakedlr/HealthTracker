package com.jakedlr.healthtracker.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.domain.MAX_WEIGHT_LBS
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.PastDatePickerDialog
import com.jakedlr.healthtracker.ui.formatLbs
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AddWeightScreen(
    onDone: () -> Unit,
    viewModel: AddWeightViewModel = viewModel(factory = AddWeightViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    AddWeightContent(
        state = state,
        onBack = onDone,
        onDateChange = viewModel::onDateChange,
        onWeightTextChange = viewModel::onWeightTextChange,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeightContent(
    state: AddWeightUiState,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onWeightTextChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.dateLocked && state.existingWeightLbs != null) "Edit weight" else "Add weight") },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                enabled = !state.dateLocked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val label = state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                Text(if (state.date == state.today) "Today, $label" else label)
            }
            OutlinedTextField(
                value = state.weightText,
                onValueChange = onWeightTextChange,
                label = { Text("Weight (lbs)") },
                singleLine = true,
                isError = state.showError,
                supportingText = {
                    when {
                        state.showError ->
                            Text("Enter a weight between 0 and ${MAX_WEIGHT_LBS.toInt()} lbs")
                        state.existingWeightLbs != null ->
                            Text("Replaces the saved ${formatLbs(state.existingWeightLbs)} lbs for this day")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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

@Preview(showBackground = true)
@Composable
private fun AddWeightPreview() {
    val today = LocalDate.of(2026, 10, 16)
    HealthTrackerTheme {
        AddWeightContent(
            state = AddWeightUiState(date = today, today = today, weightText = "182.4", existingWeightLbs = 183.0),
            onBack = {}, onDateChange = {}, onWeightTextChange = {}, onSave = {},
        )
    }
}
