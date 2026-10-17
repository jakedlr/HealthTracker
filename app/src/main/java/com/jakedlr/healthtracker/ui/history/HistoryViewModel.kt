package com.jakedlr.healthtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.data.observeDataSpan
import com.jakedlr.healthtracker.domain.DayMarks
import com.jakedlr.healthtracker.domain.daysWithData
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val loading: Boolean = true,
    /** Earliest month that can be shown: the month of the first data, or the current month. */
    val firstMonth: YearMonth,
    val currentMonth: YearMonth,
    val today: LocalDate,
)

class HistoryViewModel(
    private val weightRepository: WeightRepository,
    private val foodRepository: FoodRepository,
    private val sleepRepository: SleepRepository,
    private val clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> =
        observeDataSpan(weightRepository, foodRepository, sleepRepository, clock.zone)
            .map { span ->
                val current = YearMonth.now(clock)
                HistoryUiState(
                    loading = false,
                    firstMonth = minOf(span.first?.let(YearMonth::from) ?: current, current),
                    currentMonth = current,
                    today = LocalDate.now(clock),
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                YearMonth.now(clock).let { HistoryUiState(firstMonth = it, currentMonth = it, today = LocalDate.now(clock)) },
            )

    /** Which kinds of data each day of [month] has. */
    fun marksFor(month: YearMonth): Flow<Map<LocalDate, DayMarks>> {
        val days = month.atDay(1)..month.atEndOfMonth()
        return combine(
            weightRepository.observeBetween(days),
            foodRepository.observeLogsForDates(days, clock.zone),
            sleepRepository.observeSleepForDates(days, clock.zone),
        ) { weights, logs, sleep ->
            daysWithData(
                month = month,
                weightDates = weights.map { it.date },
                foodTimes = logs.map { it.eatenAt },
                sleepStarts = sleep.map { it.startAt },
                zone = clock.zone,
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HistoryViewModel(
                    container.weightRepository,
                    container.foodRepository,
                    container.sleepRepository,
                    container.clock,
                )
            }
        }
    }
}
