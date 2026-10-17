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
import com.jakedlr.healthtracker.ui.DateTimeField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: LogFoodViewModel = viewModel(factory = LogFoodViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.item?.name ?: "Log food") }, navigationIcon = { BackButton(onBack) })
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
            val item = state.item
            when {
                state.loading -> Unit
                item == null -> Text("This item is no longer in your library.")
                else -> {
                    Text("Per serving: ${nutritionSummary(item.nutrition)}", style = MaterialTheme.typography.bodyMedium)
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
                    DateTimeField(state.eatenAt, state.today, viewModel::onEatenAtChange)
                    state.total?.let { total ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Total", style = MaterialTheme.typography.labelLarge)
                                Text(nutritionSummary(total), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("Log") }
                }
            }
        }
    }
}
