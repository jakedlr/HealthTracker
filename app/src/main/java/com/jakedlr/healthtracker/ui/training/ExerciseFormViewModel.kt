package com.jakedlr.healthtracker.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.TrainingRepository
import com.jakedlr.healthtracker.data.TrainingSaveResult
import com.jakedlr.healthtracker.data.db.Exercise
import com.jakedlr.healthtracker.data.db.ExerciseCategory
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseFormUiState(
    val loading: Boolean = true,
    /** The exercise being edited; null for a new one. */
    val original: Exercise? = null,
    val categories: List<ExerciseCategory> = emptyList(),
    val name: String = "",
    val categoryId: Long = NO_ID,
    /** No default for a new exercise: a wrong guess locks in once it has logs. */
    val sidedness: Sidedness? = null,
    val defaultUnit: LoadUnit = LoadUnit.LBS,
    /** The exercise has logs, so sidedness can't change. */
    val sidednessLocked: Boolean = false,
    val showErrors: Boolean = false,
    val nameTaken: Boolean = false,
    val saved: Boolean = false,
)

/** New exercise (in a given category) or Edit exercise. */
class ExerciseFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TrainingRepository,
) : ViewModel() {
    private val exerciseId: Long = savedStateHandle[EXERCISE_ID_ARG] ?: NO_ID
    private val startCategoryId: Long = savedStateHandle[CATEGORY_ID_ARG] ?: NO_ID

    private val _uiState = MutableStateFlow(ExerciseFormUiState(categoryId = startCategoryId))
    val uiState: StateFlow<ExerciseFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = repository.observeCategories().first()
            val exercise = if (exerciseId != NO_ID) repository.getExercise(exerciseId) else null
            val hasLogs = exercise != null && repository.countLogs(exercise.id) > 0
            _uiState.update { state ->
                if (exercise == null) {
                    state.copy(loading = false, categories = categories)
                } else {
                    state.copy(
                        loading = false,
                        original = exercise,
                        categories = categories,
                        name = exercise.name,
                        categoryId = exercise.categoryId,
                        sidedness = exercise.sidedness,
                        defaultUnit = exercise.defaultUnit,
                        sidednessLocked = hasLogs,
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameTaken = false) }

    fun onCategoryChange(id: Long) = _uiState.update { it.copy(categoryId = id) }

    fun onSidednessChange(sidedness: Sidedness) = _uiState.update {
        if (it.sidednessLocked) it else it.copy(sidedness = sidedness)
    }

    fun onDefaultUnitChange(unit: LoadUnit) = _uiState.update { it.copy(defaultUnit = unit) }

    fun save() {
        val state = _uiState.value
        val sidedness = state.sidedness
        if (state.loading || state.name.isBlank() || sidedness == null || state.categoryId == NO_ID) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val result = state.original?.let { original ->
                repository.updateExercise(
                    original.copy(
                        name = state.name,
                        categoryId = state.categoryId,
                        sidedness = sidedness,
                        defaultUnit = state.defaultUnit,
                    ),
                )
            } ?: repository.createExercise(state.categoryId, state.name, sidedness, state.defaultUnit)
            when (result) {
                is TrainingSaveResult.Saved -> _uiState.update { it.copy(saved = true) }
                TrainingSaveResult.NameTaken -> _uiState.update { it.copy(nameTaken = true) }
                // Someone logged it in the meantime; show the lock and keep the saved sidedness.
                TrainingSaveResult.SidednessLocked -> _uiState.update {
                    it.copy(sidednessLocked = true, sidedness = it.original?.sidedness)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ExerciseFormViewModel(createSavedStateHandle(), appContainer().trainingRepository) }
        }
    }
}
