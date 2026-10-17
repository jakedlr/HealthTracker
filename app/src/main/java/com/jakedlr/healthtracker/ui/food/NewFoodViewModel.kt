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
import com.jakedlr.healthtracker.data.SaveItemResult
import com.jakedlr.healthtracker.data.db.FoodItem
import com.jakedlr.healthtracker.domain.normalizeFoodName
import com.jakedlr.healthtracker.domain.parseServings
import com.jakedlr.healthtracker.ui.optionalDate
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewFoodUiState(
    /** Scanned barcode (normalized) to save with the new item, if this came from a scan. */
    val barcode: String? = null,
    val name: String = "",
    val nutrition: NutritionInput = NutritionInput(),
    val servingsText: String = "1",
    val eatenAt: LocalDateTime,
    val today: LocalDate,
    val showErrors: Boolean = false,
    /** Set when the name matches an existing library item (ignoring case). */
    val nameTaken: FoodItem? = null,
    val saved: Boolean = false,
    /** Set when the user chose to log an existing item instead; navigate to it. */
    val useExistingItemId: Long? = null,
) {
    /** Whether "Use existing" will also attach the scanned barcode to the existing item. */
    val canLinkBarcode: Boolean get() = barcode != null && nameTaken != null && nameTaken.barcode == null
}

/** Manual entry (or unknown scanned barcode): always creates a new library item and logs it. */
class NewFoodViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        defaultEatenAt(clock, savedStateHandle.optionalDate()).let {
            NewFoodUiState(
                barcode = savedStateHandle.get<String>(BARCODE_ARG),
                eatenAt = it,
                today = LocalDate.now(clock),
            )
        },
    )
    val uiState: StateFlow<NewFoodUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameTaken = null) }

    fun onNutritionChange(input: NutritionInput) = _uiState.update { it.copy(nutrition = input) }

    fun onServingsChange(text: String) = _uiState.update { it.copy(servingsText = text) }

    fun onEatenAtChange(value: LocalDateTime) = _uiState.update { it.copy(eatenAt = value) }

    fun save() {
        val state = _uiState.value
        val name = normalizeFoodName(state.name)
        val nutrition = state.nutrition.parse()
        val servings = parseServings(state.servingsText)
        if (name == null || nutrition == null || servings == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val item = FoodItem(
                name = name,
                barcode = state.barcode,
                caloriesKcal = nutrition.caloriesKcal,
                proteinG = nutrition.proteinG,
                carbsG = nutrition.carbsG,
                fiberG = nutrition.fiberG,
                fatG = nutrition.fatG,
            )
            val eatenAt = state.eatenAt.atZone(clock.zone).toInstant().toEpochMilli()
            when (val result = repository.createItemAndLog(item, servings, eatenAt)) {
                is SaveItemResult.Saved -> _uiState.update { it.copy(saved = true) }
                is SaveItemResult.NameTaken -> _uiState.update { it.copy(nameTaken = result.existing) }
            }
        }
    }

    /** Log the existing item with the clashing name instead, linking the scanned barcode if possible. */
    fun useExisting() {
        val state = _uiState.value
        val existing = state.nameTaken ?: return
        viewModelScope.launch {
            if (state.canLinkBarcode) repository.linkBarcode(existing.id, state.barcode!!)
            _uiState.update { it.copy(useExistingItemId = existing.id) }
        }
    }

    companion object {
        const val BARCODE_ARG = "barcode"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                NewFoodViewModel(createSavedStateHandle(), container.foodRepository, container.clock)
            }
        }
    }
}
