package com.jakedlr.healthtracker.ui.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.GraphPreferences
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.domain.DailyValue
import com.jakedlr.healthtracker.domain.GraphRange
import com.jakedlr.healthtracker.domain.GraphSeries
import com.jakedlr.healthtracker.domain.buildGraphSeries
import com.jakedlr.healthtracker.domain.graphBounds
import com.jakedlr.healthtracker.domain.sleepDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class GraphsUiState(
    val loading: Boolean = true,
    val range: GraphRange,
    /** Window shared by all four graphs; null when there is nothing to plot. */
    val bounds: ClosedRange<LocalDate>? = null,
    val series: GraphSeries = GraphSeries(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class GraphsViewModel(
    private val weightRepository: WeightRepository,
    private val foodRepository: FoodRepository,
    private val sleepRepository: SleepRepository,
    private val preferences: GraphPreferences,
    private val clock: Clock,
) : ViewModel() {
    private val range = MutableStateFlow(preferences.range)

    /** firstAny/lastAny across weight, food (local date), and sleep (assigned date). */
    private val dataSpan = combine(
        weightRepository.observeDateRange(),
        foodRepository.observeEatenAtRange(),
        sleepRepository.observeStartAtRange(),
    ) { weight, eatenAt, sleepStarts ->
        // sleepDate() never decreases as start time grows, so the earliest and latest starts
        // give the earliest and latest assigned dates.
        val firsts = listOfNotNull(
            weight.first,
            eatenAt.first?.toLocalDate(),
            sleepStarts.first?.let { sleepDate(it, clock.zone) },
        )
        val lasts = listOfNotNull(
            weight.last,
            eatenAt.last?.toLocalDate(),
            sleepStarts.last?.let { sleepDate(it, clock.zone) },
        )
        firsts.minOrNull() to lasts.maxOrNull()
    }

    val uiState: StateFlow<GraphsUiState> = combine(range, dataSpan) { range, (first, last) ->
        range to graphBounds(range, LocalDate.now(clock), first, last)
    }
        .distinctUntilChanged()
        .flatMapLatest { (range, bounds) ->
            if (bounds == null) {
                flowOf(GraphsUiState(loading = false, range = range))
            } else {
                combine(
                    weightRepository.observeBetween(bounds),
                    foodRepository.observeLogsForDates(bounds, clock.zone),
                    sleepRepository.observeSleepForDates(bounds, clock.zone),
                ) { weights, logs, sleep ->
                    GraphsUiState(
                        loading = false,
                        range = range,
                        bounds = bounds,
                        series = buildGraphSeries(
                            window = bounds,
                            weights = weights.map { DailyValue(it.date, it.weightLbs) },
                            food = logs.map { it.toLoggedFood() },
                            sleep = sleep.map { it.toSpan() },
                            zone = clock.zone,
                        ),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphsUiState(range = range.value))

    fun onRangeChange(newRange: GraphRange) {
        preferences.range = newRange
        range.value = newRange
    }

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(clock.zone).toLocalDate()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                GraphsViewModel(
                    container.weightRepository,
                    container.foodRepository,
                    container.sleepRepository,
                    container.graphPreferences,
                    container.clock,
                )
            }
        }
    }
}
