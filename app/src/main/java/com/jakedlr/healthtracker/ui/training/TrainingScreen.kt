package com.jakedlr.healthtracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import com.jakedlr.healthtracker.ui.BackButton
import com.jakedlr.healthtracker.ui.theme.HealthTrackerTheme

/** The Weight training home: exercise categories in the user's order. */
@Composable
fun TrainingScreen(
    onBack: () -> Unit,
    onOpenCategory: (categoryId: Long) -> Unit,
    viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val newCategory by viewModel.newCategory.collectAsStateWithLifecycle()

    TrainingContent(
        categories = categories,
        onBack = onBack,
        onOpenCategory = onOpenCategory,
        onNewCategory = viewModel::openNewCategory,
        onReorder = viewModel::reorder,
    )
    newCategory?.let {
        CategoryNameDialog(
            title = "New category",
            state = it,
            onTextChange = viewModel::onNewCategoryText,
            onConfirm = viewModel::createCategory,
            onDismiss = viewModel::dismissNewCategory,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingContent(
    categories: List<ExerciseCategory>?,
    onBack: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    onNewCategory: () -> Unit,
    onReorder: (List<ExerciseCategory>) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Weight training") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onNewCategory, modifier = Modifier.fillMaxWidth()) { Text("New category") }
            when {
                categories == null -> Unit
                categories.isEmpty() -> Text(
                    "No categories yet. Create one, such as \"Leg day\", to start adding exercises.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> ReorderableColumn(
                    items = categories,
                    key = { it.id },
                    onReorder = onReorder,
                ) { category, dragging ->
                    ReorderableCard(dragging = dragging, onClick = { onOpenCategory(category.id) }) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainingPreview() {
    HealthTrackerTheme {
        TrainingContent(
            categories = listOf(ExerciseCategory(1, "Leg day", 0), ExerciseCategory(2, "Back day", 1)),
            onBack = {}, onOpenCategory = {}, onNewCategory = {}, onReorder = {},
        )
    }
}
