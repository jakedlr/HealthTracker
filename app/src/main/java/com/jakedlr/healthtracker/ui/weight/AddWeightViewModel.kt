package com.jakedlr.healthtracker.ui.weight

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.domain.parseWeightLbs
import com.jakedlr.healthtracker.ui.formatLbs
import com.jakedlr.healthtracker.ui.optionalDate
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddWeightUiState(
    val date: LocalDate,
    val today: LocalDate,
    val weightText: String = "",
    /** Weight already saved for [date]; saving will overwrite it. */
    val existingWeightLbs: Double? = null,
    /** Opened for a specific day (from History): the date can't be changed. */
    val dateLocked: Boolean = false,
    val showError: Boolean = false,
    val saved: Boolean = false,
)

class AddWeightViewModel(
    savedStateHandle: SavedStateHandle,
    private val weightRepository: WeightRepository,
    clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LocalDate.now(clock).let { today ->
            val fixedDate = savedStateHandle.optionalDate()
            AddWeightUiState(date = fixedDate ?: today, today = today, dateLocked = fixedDate != null)
        },
    )
    val uiState: StateFlow<AddWeightUiState> = _uiState.asStateFlow()

    init {
        loadExisting(_uiState.value.date)
    }

    fun onDateChange(date: LocalDate) {
        val state = _uiState.value
        if (state.dateLocked || date > state.today || date == state.date) return
        _uiState.update { it.copy(date = date) }
        loadExisting(date)
    }

    fun onWeightTextChange(text: String) {
        _uiState.update { it.copy(weightText = text, showError = false) }
    }

    fun save() {
        val state = _uiState.value
        val weight = parseWeightLbs(state.weightText)
        if (weight == null) {
            _uiState.update { it.copy(showError = true) }
            return
        }
        viewModelScope.launch {
            weightRepository.saveWeight(state.date, weight)
            _uiState.update { it.copy(saved = true) }
        }
    }

    private fun loadExisting(date: LocalDate) {
        viewModelScope.launch {
            val existing = weightRepository.getWeight(date)?.weightLbs
            _uiState.update { state ->
                if (state.date != date) return@update state
                val previousPrefill = state.existingWeightLbs?.let(::formatLbs)
                val text = when {
                    existing != null -> formatLbs(existing)
                    // Don't carry the previous date's value over to a date with no entry.
                    state.weightText == previousPrefill -> ""
                    else -> state.weightText
                }
                state.copy(existingWeightLbs = existing, weightText = text, showError = false)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AddWeightViewModel(createSavedStateHandle(), container.weightRepository, container.clock)
            }
        }
    }
}
