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
import com.jakedlr.healthtracker.data.db.Exercise
import com.jakedlr.healthtracker.data.db.ExerciseLog
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseUiState(
    val loading: Boolean = true,
    /** Null once loaded means the exercise no longer exists. */
    val exercise: Exercise? = null,
    /** Newest first: the latest 10 on the Exercise screen, every log on All logs. */
    val logs: List<ExerciseLog> = emptyList(),
    val today: LocalDate,
) {
    /** Today's log, if any: the Exercise screen offers to edit it instead of adding another. */
    val todaysLog: ExerciseLog? get() = logs.firstOrNull()?.takeIf { it.date == today }
}

/** Backs both the Exercise screen (recent logs) and All logs (every log); see the two factories. */
class ExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TrainingRepository,
    clock: Clock,
    allLogs: Boolean,
) : ViewModel() {
    val exerciseId: Long = checkNotNull(savedStateHandle[ExerciseFormViewModel.EXERCISE_ID_ARG])
    private val today = LocalDate.now(clock)

    private val logs: Flow<List<ExerciseLog>> =
        if (allLogs) repository.observeAllLogs(exerciseId) else repository.observeRecentLogs(exerciseId)

    val uiState: StateFlow<ExerciseUiState> = combine(repository.observeExercise(exerciseId), logs) { exercise, logs ->
        ExerciseUiState(loading = false, exercise = exercise, logs = logs, today = today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseUiState(today = today))

    /** Log count for the delete confirmation; null when no confirmation is showing. */
    private val _confirmingDelete = MutableStateFlow<Int?>(null)
    val confirmingDelete: StateFlow<Int?> = _confirmingDelete.asStateFlow()

    fun requestDelete() {
        viewModelScope.launch { _confirmingDelete.update { repository.countLogs(exerciseId) } }
    }

    fun dismissDelete() = _confirmingDelete.update { null }

    /** The screen closes once the exercise is gone (it observes [uiState]). */
    fun confirmDelete() {
        _confirmingDelete.update { null }
        viewModelScope.launch { repository.deleteExercise(exerciseId) }
    }

    companion object {
        private fun factory(allLogs: Boolean) = viewModelFactory {
            initializer {
                val container = appContainer()
                ExerciseViewModel(createSavedStateHandle(), container.trainingRepository, container.clock, allLogs)
            }
        }

        val RecentFactory: ViewModelProvider.Factory = factory(allLogs = false)
        val AllLogsFactory: ViewModelProvider.Factory = factory(allLogs = true)
    }
}
