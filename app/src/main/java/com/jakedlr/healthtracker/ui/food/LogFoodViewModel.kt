package com.jakedlr.healthtracker.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.db.FoodItem
import com.jakedlr.healthtracker.domain.Nutrition
import com.jakedlr.healthtracker.domain.parseServings
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogFoodUiState(
    val loading: Boolean = true,
    /** Null once loaded means the item no longer exists. */
    val item: FoodItem? = null,
    val servingsText: String = "1",
    val eatenAt: LocalDateTime,
    val today: LocalDate,
    val showError: Boolean = false,
    val saved: Boolean = false,
) {
    /** Live total for the current servings, or null if servings is invalid. */
    val total: Nutrition? get() = item?.let { i -> parseServings(servingsText)?.let { i.nutrition * it } }
}

class LogFoodViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository,
    private val clock: Clock,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle[ITEM_ID_ARG])

    private val _uiState = MutableStateFlow(
        LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES).let {
            LogFoodUiState(eatenAt = it, today = it.toLocalDate())
        },
    )
    val uiState: StateFlow<LogFoodUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val item = repository.getItem(itemId)
            _uiState.update { it.copy(loading = false, item = item) }
        }
    }

    fun onServingsChange(text: String) = _uiState.update { it.copy(servingsText = text, showError = false) }

    fun onEatenAtChange(value: LocalDateTime) = _uiState.update { it.copy(eatenAt = value) }

    fun save() {
        val state = _uiState.value
        val item = state.item ?: return
        val servings = parseServings(state.servingsText)
        if (servings == null) {
            _uiState.update { it.copy(showError = true) }
            return
        }
        viewModelScope.launch {
            repository.logFood(item, servings, state.eatenAt.atZone(clock.zone).toInstant().toEpochMilli())
            _uiState.update { it.copy(saved = true) }
        }
    }

    companion object {
        const val ITEM_ID_ARG = "itemId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                LogFoodViewModel(createSavedStateHandle(), container.foodRepository, container.clock)
            }
        }
    }
}
