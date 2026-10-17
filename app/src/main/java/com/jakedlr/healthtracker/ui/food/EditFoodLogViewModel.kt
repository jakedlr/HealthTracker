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
import com.jakedlr.healthtracker.data.db.FoodLog
import com.jakedlr.healthtracker.domain.Nutrition
import com.jakedlr.healthtracker.domain.parseServings
import com.jakedlr.healthtracker.ui.formatAmount
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditFoodLogUiState(
    val loading: Boolean = true,
    /** Null once loaded means the log no longer exists. */
    val log: FoodLog? = null,
    val servingsText: String = "",
    val eatenAt: LocalDateTime? = null,
    val today: LocalDate,
    val showError: Boolean = false,
    val confirmingDelete: Boolean = false,
    /** Saved or deleted; the screen should close. */
    val finished: Boolean = false,
) {
    /** Live total from the log's own snapshot, or null if servings is invalid. */
    val total: Nutrition? get() = log?.let { l -> parseServings(servingsText)?.let { l.perServing * it } }
}

/** Edits a past food log's servings and time. Its nutrition snapshot never changes. */
class EditFoodLogViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FoodRepository,
    private val clock: Clock,
) : ViewModel() {
    private val logId: Long = checkNotNull(savedStateHandle[LOG_ID_ARG])

    private val _uiState = MutableStateFlow(EditFoodLogUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<EditFoodLogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val log = repository.getLog(logId)
            _uiState.update {
                it.copy(
                    loading = false,
                    log = log,
                    servingsText = log?.let { l -> formatAmount(l.servings) }.orEmpty(),
                    eatenAt = log?.let { l -> Instant.ofEpochMilli(l.eatenAt).atZone(clock.zone).toLocalDateTime() },
                )
            }
        }
    }

    fun onServingsChange(text: String) = _uiState.update { it.copy(servingsText = text, showError = false) }

    fun onEatenAtChange(value: LocalDateTime) = _uiState.update { it.copy(eatenAt = value) }

    fun save() {
        val state = _uiState.value
        val log = state.log ?: return
        val eatenAt = state.eatenAt ?: return
        val servings = parseServings(state.servingsText)
        if (servings == null) {
            _uiState.update { it.copy(showError = true) }
            return
        }
        viewModelScope.launch {
            repository.updateLog(log.id, servings, eatenAt.atZone(clock.zone).toInstant().toEpochMilli())
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(confirmingDelete = true) }

    fun cancelDelete() = _uiState.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        viewModelScope.launch {
            repository.deleteLog(logId)
            _uiState.update { it.copy(confirmingDelete = false, finished = true) }
        }
    }

    companion object {
        const val LOG_ID_ARG = "logId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                EditFoodLogViewModel(createSavedStateHandle(), container.foodRepository, container.clock)
            }
        }
    }
}
