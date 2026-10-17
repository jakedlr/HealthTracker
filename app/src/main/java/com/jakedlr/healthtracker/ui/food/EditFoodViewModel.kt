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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditFoodUiState(
    val loading: Boolean = true,
    /** The item as saved; null once loaded means it no longer exists. */
    val original: FoodItem? = null,
    val name: String = "",
    val nutrition: NutritionInput = NutritionInput(),
    val showErrors: Boolean = false,
    val nameTaken: FoodItem? = null,
    val confirmingDelete: Boolean = false,
    /** Saved or deleted; the screen should close. */
    val finished: Boolean = false,
)

/** Edits a library item. Changes apply to future logs only; past logs keep their snapshots. */
class EditFoodViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle[ITEM_ID_ARG])

    private val _uiState = MutableStateFlow(EditFoodUiState())
    val uiState: StateFlow<EditFoodUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val item = repository.getItem(itemId)
            _uiState.update {
                it.copy(
                    loading = false,
                    original = item,
                    name = item?.name.orEmpty(),
                    nutrition = item?.let { i -> NutritionInput.from(i.nutrition) } ?: NutritionInput(),
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameTaken = null) }

    fun onNutritionChange(input: NutritionInput) = _uiState.update { it.copy(nutrition = input) }

    fun save() {
        val state = _uiState.value
        val original = state.original ?: return
        val name = normalizeFoodName(state.name)
        val nutrition = state.nutrition.parse()
        if (name == null || nutrition == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            when (val result = repository.updateItem(original.copy(name = name).withNutrition(nutrition))) {
                is SaveItemResult.Saved -> _uiState.update { it.copy(finished = true) }
                is SaveItemResult.NameTaken -> _uiState.update { it.copy(nameTaken = result.existing) }
            }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(confirmingDelete = true) }

    fun cancelDelete() = _uiState.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        val original = _uiState.value.original ?: return
        viewModelScope.launch {
            repository.deleteItem(original)
            _uiState.update { it.copy(confirmingDelete = false, finished = true) }
        }
    }

    companion object {
        const val ITEM_ID_ARG = "itemId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { EditFoodViewModel(createSavedStateHandle(), appContainer().foodRepository) }
        }
    }
}
