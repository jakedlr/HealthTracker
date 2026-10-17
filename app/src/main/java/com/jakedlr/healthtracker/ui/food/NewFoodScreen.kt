package com.jakedlr.healthtracker.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.db.FoodItem
import com.jakedlr.healthtracker.domain.normalizeFoodName
import com.jakedlr.healthtracker.domain.parseServings
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.DateTimeField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewFoodScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onUseExisting: (itemId: Long) -> Unit,
    viewModel: NewFoodViewModel = viewModel(factory = NewFoodViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LaunchedEffect(state.useExistingItemId) {
        state.useExistingItemId?.let(onUseExisting)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("New food") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = FoodScreenSpacing,
        ) {
            state.barcode?.let {
                Text("Barcode $it", style = MaterialTheme.typography.bodyMedium)
            }
            FoodNameField(
                name = state.name,
                onNameChange = viewModel::onNameChange,
                showErrors = state.showErrors,
                nameTaken = state.nameTaken,
                takenAction = {
                    TextButton(onClick = viewModel::useExisting) {
                        Text(if (state.canLinkBarcode) "Use existing and link barcode" else "Use existing")
                    }
                },
            )
            NutritionFields(state.nutrition, viewModel::onNutritionChange, state.showErrors)
            OutlinedTextField(
                value = state.servingsText,
                onValueChange = viewModel::onServingsChange,
                label = { Text("Servings eaten") },
                singleLine = true,
                isError = state.showErrors && parseServings(state.servingsText) == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            DateTimeField(state.eatenAt, state.today, viewModel::onEatenAtChange)
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) { Text("Save and log") }
        }
    }
}

/**
 * Name input shared by New food and Edit food. When [nameTaken] is set, shows the conflict
 * and [takenAction] (if any) under the field.
 */
@Composable
fun FoodNameField(
    name: String,
    onNameChange: (String) -> Unit,
    showErrors: Boolean,
    nameTaken: FoodItem?,
    takenAction: (@Composable (FoodItem) -> Unit)? = null,
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            isError = nameTaken != null || (showErrors && normalizeFoodName(name) == null),
            supportingText = {
                when {
                    nameTaken != null -> Text("\"${nameTaken.name}\" is already in your library")
                    showErrors && normalizeFoodName(name) == null -> Text("Enter a name")
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (nameTaken != null && takenAction != null) takenAction(nameTaken)
    }
}
