package com.jakedlr.healthtracker.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.ConfirmDeleteDialog
import com.jakedlr.healthtracker.ui.DateTimeField
import com.jakedlr.healthtracker.ui.DeleteButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodLogScreen(
    onDone: () -> Unit,
    viewModel: EditFoodLogViewModel = viewModel(factory = EditFoodLogViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.log?.name ?: "Edit food") }, navigationIcon = { BackButton(onDone) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = FoodScreenSpacing,
        ) {
            val log = state.log
            val eatenAt = state.eatenAt
            when {
                state.loading -> Unit
                log == null || eatenAt == null -> Text("This entry no longer exists.")
                else -> {
                    Text(
                        "Per serving (as logged): ${nutritionSummary(log.perServing)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = state.servingsText,
                        onValueChange = viewModel::onServingsChange,
                        label = { Text("Servings") },
                        singleLine = true,
                        isError = state.showError,
                        supportingText = { if (state.showError) Text("Enter a number greater than 0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateTimeField(eatenAt, state.today, viewModel::onEatenAtChange)
                    state.total?.let { total ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Total", style = MaterialTheme.typography.labelLarge)
                                Text(nutritionSummary(total), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Text(
                        "Nutrition stays as it was when you logged this, even if the library item changed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                    DeleteButton("Delete entry", viewModel::requestDelete)
                }
            }
        }
    }

    if (state.confirmingDelete) {
        ConfirmDeleteDialog(
            title = "Delete \"${state.log?.name}\"?",
            text = "This removes it from the day. Your food library isn't affected.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}
