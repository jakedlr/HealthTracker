package com.jakedlr.healthtracker.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import java.time.Clock
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
    // All four start empty: sleep times never default to "now".
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
    val saved: Boolean = false,
) {
    val isComplete: Boolean get() = startAt != null && endAt != null
}

class AddSleepViewModel(
    private val repository: SleepRepository,
    clock: Clock,
) : ViewModel() {
    private val zone: ZoneId = clock.zone
    private val _uiState = MutableStateFlow(AddSleepUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<AddSleepUiState> = _uiState.asStateFlow()

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
            when (val result = repository.addSleep(start, end)) {
                is SaveSleepResult.Saved -> _uiState.update { it.copy(saved = true) }
                is SaveSleepResult.Overlaps -> _uiState.update { it.copy(overlap = result.existing) }
                is SaveSleepResult.Invalid -> _uiState.update { it.copy(error = result.error) }
            }
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AddSleepViewModel(container.sleepRepository, container.clock)
            }
        }
    }
}
