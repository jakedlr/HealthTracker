package com.jakedlr.healthtracker.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.clickable
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.db.FoodItem
import com.jakedlr.healthtracker.ui.formatKcal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FoodSearchResults(
    val loading: Boolean = true,
    val items: List<FoodItem> = emptyList(),
)

/** Library search shared by Add food and Food library. Most recently logged items come first. */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodSearchViewModel(repository: FoodRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<FoodSearchResults> = _query
        .flatMapLatest { repository.searchItems(it) }
        .map { FoodSearchResults(loading = false, items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodSearchResults())

    fun onQueryChange(query: String) {
        _query.value = query
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { FoodSearchViewModel(appContainer().foodRepository) }
        }
    }
}

@Composable
fun FoodSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search library") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun FoodResultsList(
    results: FoodSearchResults,
    query: String,
    onItemClick: (FoodItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (results.loading) return
    if (results.items.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                if (query.isBlank()) "Your food library is empty" else "No matches for \"${query.trim()}\"",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    LazyColumn(modifier) {
        items(results.items, key = { it.id }) { item ->
            Column {
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text("${formatKcal(item.caloriesKcal)} kcal per serving") },
                    modifier = Modifier.clickable { onItemClick(item) },
                )
                HorizontalDivider()
            }
        }
    }
}

/** Column spacing used by the food screens. */
val FoodScreenSpacing = Arrangement.spacedBy(12.dp)
