package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.label
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme

@Composable
fun ExerciseFormScreen(
    onDone: () -> Unit,
    viewModel: ExerciseFormViewModel = viewModel(factory = ExerciseFormViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    ExerciseFormContent(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onSidednessChange = viewModel::onSidednessChange,
        onDefaultUnitChange = viewModel::onDefaultUnitChange,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseFormContent(
    state: ExerciseFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Long) -> Unit,
    onSidednessChange: (Sidedness) -> Unit,
    onDefaultUnitChange: (LoadUnit) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.original == null) "New exercise" else "Edit exercise") },
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
            val blankName = state.showErrors && state.name.isBlank()
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                singleLine = true,
                isError = blankName || state.nameTaken,
                supportingText = {
                    when {
                        state.nameTaken -> Text("An exercise with this name already exists")
                        blankName -> Text("Enter a name")
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryPicker(state.categories, state.categoryId, onCategoryChange)

            Text("Sides", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = listOf(Sidedness.DOUBLE, Sidedness.SINGLE),
                selected = state.sidedness,
                label = { it.label() },
                onSelect = onSidednessChange,
                enabled = !state.sidednessLocked,
            )
            when {
                state.sidednessLocked -> Text(
                    "This exercise has logs, so its sides can't change. Create a new exercise instead.",
                    style = MaterialTheme.typography.bodySmall,
                )
                state.showErrors && state.sidedness == null -> Text(
                    "Choose double-sided (both sides together) or single-sided (each side logged separately)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Text(
                    "Single-sided exercises log the left and right sides separately.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text("Default unit", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = LoadUnit.entries,
                selected = state.defaultUnit,
                label = { it.label },
                onSelect = onDefaultUnitChange,
            )
            Text(
                "Pre-selected for new logs. Past logs keep the unit they were logged in.",
                style = MaterialTheme.typography.bodySmall,
            )

            Button(onClick = onSave, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

@Composable
private fun CategoryPicker(categories: List<ExerciseCategory>, selectedId: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(categories.firstOrNull { it.id == selectedId }?.name ?: "Select category")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseFormPreview() {
    HealthTrackerTheme {
        ExerciseFormContent(
            state = ExerciseFormUiState(
                loading = false,
                categories = listOf(ExerciseCategory(1, "Leg day", 0)),
                name = "Bulgarian split squat",
                categoryId = 1,
            ),
            onBack = {}, onNameChange = {}, onCategoryChange = {}, onSidednessChange = {},
            onDefaultUnitChange = {}, onSave = {},
        )
    }
}
