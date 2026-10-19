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
import com.jakedlr.healthtracker.domain.LoadUnit
import com.jakedlr.healthtracker.domain.Sidedness
import com.jakedlr.healthtracker.domain.TopSet
import com.jakedlr.healthtracker.domain.TopSets
import com.jakedlr.healthtracker.domain.isLoggableDate
import com.jakedlr.healthtracker.domain.parseLoad
import com.jakedlr.healthtracker.domain.parseReps
import com.jakedlr.healthtracker.domain.perSide
import com.jakedlr.healthtracker.ui.formatAmount
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Text inputs for one top set; [skipped] only applies to a side of a single-sided exercise. */
data class TopSetInput(val load: String = "", val reps: String = "", val skipped: Boolean = false) {
    fun parse(): TopSet? {
        val load = parseLoad(load) ?: return null
        val reps = parseReps(reps) ?: return null
        return TopSet(load, reps)
    }

    companion object {
        fun from(set: TopSet?) =
            set?.let { TopSetInput(formatAmount(it.load), it.reps.toString()) } ?: TopSetInput(skipped = true)
    }
}

data class ExerciseLogFormUiState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
    val today: LocalDate,
    val date: LocalDate,
    /** The log this form was opened to edit; null when adding. */
    val editing: ExerciseLog? = null,
    /** The log already saved for [date], if any; saving replaces it. */
    val existingForDate: ExerciseLog? = null,
    val unit: LoadUnit = LoadUnit.LBS,
    /** Double-sided input. */
    val both: TopSetInput = TopSetInput(),
    val left: TopSetInput = TopSetInput(),
    val right: TopSetInput = TopSetInput(),
    val showErrors: Boolean = false,
    val confirmingDelete: Boolean = false,
    /** Saved or deleted; the screen should close. */
    val finished: Boolean = false,
) {
    /** Picking a date with another log loads that log; saving then replaces it. */
    val replacesAnotherLog: Boolean get() = existingForDate != null && existingForDate.id != editing?.id

    /** When editing, Delete always removes the log that was opened; when adding, the log already on [date]. */
    val deleteTarget: ExerciseLog? get() = editing ?: existingForDate

    val bothSidesSkipped: Boolean get() = left.skipped && right.skipped
}

/** Add log / Edit log for one exercise. One log per exercise and date. */
class ExerciseLogFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TrainingRepository,
    clock: Clock,
) : ViewModel() {
    private val exerciseId: Long = checkNotNull(savedStateHandle[ExerciseFormViewModel.EXERCISE_ID_ARG])
    private val logId: Long = savedStateHandle[LOG_ID_ARG] ?: NO_ID

    private val _uiState = MutableStateFlow(
        LocalDate.now(clock).let { ExerciseLogFormUiState(today = it, date = it) },
    )
    val uiState: StateFlow<ExerciseLogFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = repository.getExercise(exerciseId)
            val editing = if (logId != NO_ID) repository.getLog(logId) else null
            val date = editing?.date ?: _uiState.value.date
            val existing = editing ?: repository.getLog(exerciseId, date)
            _uiState.update { state ->
                val base = state.copy(
                    loading = false,
                    exercise = exercise,
                    date = date,
                    editing = editing,
                    unit = exercise?.defaultUnit ?: state.unit,
                )
                existing?.let { base.prefilledFrom(it) } ?: base
            }
        }
    }

    fun onDateChange(date: LocalDate) {
        val state = _uiState.value
        if (state.loading || !isLoggableDate(date, state.today) || date == state.date) return
        val previous = state.existingForDate
        _uiState.update { it.copy(date = date, existingForDate = null) }
        viewModelScope.launch {
            val existing = repository.getLog(exerciseId, date)
            _uiState.update { s ->
                if (s.date != date) return@update s
                when {
                    existing != null -> s.prefilledFrom(existing)
                    // Moving the opened log to an empty date: it keeps its own values.
                    s.editing != null -> s.prefilledFrom(s.editing).copy(existingForDate = null)
                    // Don't carry another date's untouched values over to a date with no log.
                    previous != null && s.inputsMatch(previous) -> s.copy(
                        unit = s.exercise?.defaultUnit ?: s.unit,
                        both = TopSetInput(),
                        left = TopSetInput(),
                        right = TopSetInput(),
                    )
                    else -> s
                }
            }
        }
    }

    fun onUnitChange(unit: LoadUnit) = _uiState.update { it.copy(unit = unit) }

    fun onBothChange(input: TopSetInput) = _uiState.update { it.copy(both = input) }

    fun onLeftChange(input: TopSetInput) = _uiState.update { it.copy(left = input) }

    fun onRightChange(input: TopSetInput) = _uiState.update { it.copy(right = input) }

    fun save() {
        val state = _uiState.value
        val exercise = state.exercise ?: return
        val sets: TopSets? = when (exercise.sidedness) {
            Sidedness.DOUBLE -> state.both.parse()?.let { TopSets.Both(it) }
            Sidedness.SINGLE -> {
                val left = if (state.left.skipped) null else state.left.parse()
                val right = if (state.right.skipped) null else state.right.parse()
                val sideInvalid = (!state.left.skipped && left == null) || (!state.right.skipped && right == null)
                if (sideInvalid) null else perSide(left, right)
            }
        }
        if (sets == null || !isLoggableDate(state.date, state.today)) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            repository.saveLog(exerciseId, state.editing?.id, state.date, state.unit, sets)
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(confirmingDelete = true) }

    fun dismissDelete() = _uiState.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            repository.deleteLog(target.id)
            _uiState.update { it.copy(confirmingDelete = false, finished = true) }
        }
    }

    /** True if the inputs still hold exactly what [log] would pre-fill (nothing typed since). */
    private fun ExerciseLogFormUiState.inputsMatch(log: ExerciseLog): Boolean {
        val prefilled = prefilledFrom(log)
        return unit == prefilled.unit && both == prefilled.both && left == prefilled.left && right == prefilled.right
    }

    private fun ExerciseLogFormUiState.prefilledFrom(log: ExerciseLog) = when (val sets = log.topSets) {
        is TopSets.Both -> copy(existingForDate = log, unit = log.unit, both = TopSetInput.from(sets.set))
        is TopSets.PerSide -> copy(
            existingForDate = log,
            unit = log.unit,
            left = TopSetInput.from(sets.left),
            right = TopSetInput.from(sets.right),
        )
    }

    companion object {
        const val LOG_ID_ARG = "logId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ExerciseLogFormViewModel(createSavedStateHandle(), container.trainingRepository, container.clock)
            }
        }
    }
}
