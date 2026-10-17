package com.jakedlr.healthtracker.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.ui.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodScreen(
    onDone: () -> Unit,
    viewModel: EditFoodViewModel = viewModel(factory = EditFoodViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit food") }, navigationIcon = { BackButton(onDone) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = FoodScreenSpacing,
        ) {
            when {
                state.loading -> Unit
                state.original == null -> Text("This item is no longer in your library.")
                else -> {
                    FoodNameField(state.name, viewModel::onNameChange, state.showErrors, state.nameTaken)
                    NutritionFields(state.nutrition, viewModel::onNutritionChange, state.showErrors)
                    Text(
                        "Changes apply to future logs only. Food you've already logged keeps its values.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                    OutlinedButton(
                        onClick = viewModel::requestDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Delete from library") }
                }
            }
        }
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete \"${state.original?.name}\"?") },
            text = {
                Text("It will be removed from your library. Food you've already logged keeps its values.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") } },
        )
    }
}
