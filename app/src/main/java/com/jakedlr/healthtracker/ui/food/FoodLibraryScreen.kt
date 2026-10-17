package com.jakedlr.healthtracker.ui.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.ui.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    onBack: () -> Unit,
    onEditItem: (itemId: Long) -> Unit,
    viewModel: FoodSearchViewModel = viewModel(factory = FoodSearchViewModel.Factory),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Food library") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = FoodScreenSpacing,
        ) {
            FoodSearchField(query, viewModel::onQueryChange)
            FoodResultsList(results, query, onItemClick = { onEditItem(it.id) })
        }
    }
}
