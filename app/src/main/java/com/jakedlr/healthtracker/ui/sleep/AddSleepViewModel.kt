package com.jakedlr.healthtracker.ui.sleep

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.SaveSleepResult
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.data.db.SleepEntry
import com.jakedlr.healthtracker.domain.SleepError
import com.jakedlr.healthtracker.domain.sleepDate
import com.jakedlr.healthtracker.domain.validateSleep
import com.jakedlr.healthtracker.ui.optionalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddSleepUiState(
    val today: LocalDate,
    /** Where the "Fell asleep" pickers open when empty: the night of the chosen day (default yesterday). */
    val suggestedStart: LocalDateTime,
    /** Set when editing an existing entry (from History). */
    val editingId: Long? = null,
    val loading: Boolean = false,
    // All four start empty for a new entry: sleep times never default to "now".
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    /** Instants, set once both date and time of that side are chosen. */
    val startAt: Long? = null,
    val endAt: Long? = null,
    /** Validation result once both instants are known. */
    val error: SleepError? = null,
    /** The date this sleep would count toward, when valid. */
    val countsToward: LocalDate? = null,
    /** Save was pressed with fields missing. */
    val showMissing: Boolean = false,
    val overlap: SleepEntry? = null,
    val confirmingDelete: Boolean = false,
    /** Saved or deleted; the screen should close. */
    val finished: Boolean = false,
) {
    val isComplete: Boolean get() = startAt != null && endAt != null
}

/** Adds a sleep entry, or edits one when opened with [SLEEP_ID_ARG]. */
class AddSleepViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SleepRepository,
    clock: Clock,
) : ViewModel() {
    private val zone: ZoneId = clock.zone
    private val editingId: Long? = savedStateHandle.get<Long>(SLEEP_ID_ARG)?.takeIf { it != NO_SLEEP_ID }

    private val _uiState = MutableStateFlow(
        LocalDate.now(clock).let { today ->
            // A sleep that counts toward day D usually starts the evening of D.
            val night = savedStateHandle.optionalDate() ?: today.minusDays(1)
            AddSleepUiState(
                today = today,
                suggestedStart = LocalDateTime.of(night, LocalTime.of(23, 0)),
                editingId = editingId,
                loading = editingId != null,
            )
        },
    )
    val uiState: StateFlow<AddSleepUiState> = _uiState.asStateFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val entry = repository.getSleep(editingId)
                if (entry == null) {
                    _uiState.update { it.copy(loading = false, finished = true) }
                    return@launch
                }
                val start = Instant.ofEpochMilli(entry.startAt).atZone(zone).toLocalDateTime()
                val end = Instant.ofEpochMilli(entry.endAt).atZone(zone).toLocalDateTime()
                edit {
                    it.copy(
                        loading = false,
                        startDate = start.toLocalDate(),
                        startTime = start.toLocalTime(),
                        endDate = end.toLocalDate(),
                        endTime = end.toLocalTime(),
                    )
                }
            }
        }
    }

    fun onStartDateChange(date: LocalDate) = edit { it.copy(startDate = date) }
    fun onStartTimeChange(time: LocalTime) = edit { it.copy(startTime = time) }
    fun onEndDateChange(date: LocalDate) = edit { it.copy(endDate = date) }
    fun onEndTimeChange(time: LocalTime) = edit { it.copy(endTime = time) }

    fun save() {
        val state = _uiState.value
        val start = state.startAt
        val end = state.endAt
        if (start == null || end == null) {
            _uiState.update { it.copy(showMissing = true) }
            return
        }
        if (state.error != null) return
        viewModelScope.launch {
            val result = if (editingId != null) {
                repository.updateSleep(editingId, start, end)
            } else {
                repository.addSleep(start, end)
            }
            when (result) {
                is SaveSleepResult.Saved -> _uiState.update { it.copy(finished = true) }
                is SaveSleepResult.Overlaps -> _uiState.update { it.copy(overlap = result.existing) }
                is SaveSleepResult.Invalid -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(confirmingDelete = true) }

    fun cancelDelete() = _uiState.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        val id = editingId ?: return
        viewModelScope.launch {
            repository.deleteSleep(id)
            _uiState.update { it.copy(confirmingDelete = false, finished = true) }
        }
    }

    /** Applies a field change and recomputes everything derived from the four fields. */
    private fun edit(change: (AddSleepUiState) -> AddSleepUiState) = _uiState.update { old ->
        val s = change(old)
        val startAt = instant(s.startDate, s.startTime)
        val endAt = instant(s.endDate, s.endTime)
        val error = if (startAt != null && endAt != null) validateSleep(startAt, endAt) else null
        s.copy(
            startAt = startAt,
            endAt = endAt,
            error = error,
            countsToward = if (startAt != null && endAt != null && error == null) sleepDate(startAt, zone) else null,
            overlap = null,
        )
    }

    private fun instant(date: LocalDate?, time: LocalTime?): Long? =
        if (date == null || time == null) null
        else LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

    companion object {
        const val SLEEP_ID_ARG = "sleepId"
        const val NO_SLEEP_ID = -1L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AddSleepViewModel(createSavedStateHandle(), container.sleepRepository, container.clock)
            }
        }
    }
}
