package com.jakedlr.healthtracker.ui.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.domain.DailyValue
import com.jakedlr.healthtracker.domain.GraphRange
import com.jakedlr.healthtracker.domain.graphBounds
import com.jakedlr.healthtracker.domain.pointsInRange
import com.jakedlr.healthtracker.domain.sleepDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GraphsUiState(
    val loading: Boolean = true,
    /** Null when there is nothing to plot. */
    val bounds: ClosedRange<LocalDate>? = null,
    val weight: List<DailyValue> = emptyList(),
)

class GraphsViewModel(
    weightRepository: WeightRepository,
    foodRepository: FoodRepository,
    sleepRepository: SleepRepository,
    private val clock: Clock,
) : ViewModel() {
    // The range selector arrives in milestone 5; until then always show everything.
    private val range = GraphRange.ALL_TIME

    val uiState: StateFlow<GraphsUiState> = combine(
        weightRepository.observeAll(),
        foodRepository.observeEatenAtRange(),
        sleepRepository.observeStartAtRange(),
    ) { entries, eatenAt, sleepStarts ->
        val weight = entries.map { DailyValue(it.date, it.weightLbs) }
        val foodFirst = eatenAt.first?.toLocalDate()
        val foodLast = eatenAt.last?.toLocalDate()
        // Sleep counts on its assigned date. sleepDate() never decreases as start time grows,
        // so the earliest and latest starts give the earliest and latest assigned dates.
        val sleepFirst = sleepStarts.first?.let { sleepDate(it, clock.zone) }
        val sleepLast = sleepStarts.last?.let { sleepDate(it, clock.zone) }
        val bounds = graphBounds(
            range = range,
            today = LocalDate.now(clock),
            firstAny = listOfNotNull(weight.minOfOrNull { it.date }, foodFirst, sleepFirst).minOrNull(),
            lastAny = listOfNotNull(weight.maxOfOrNull { it.date }, foodLast, sleepLast).maxOrNull(),
        )
        GraphsUiState(
            loading = false,
            bounds = bounds,
            weight = bounds?.let { pointsInRange(weight, it) }.orEmpty(),
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphsUiState())

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(clock.zone).toLocalDate()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                GraphsViewModel(
                    container.weightRepository,
                    container.foodRepository,
                    container.sleepRepository,
                    container.clock,
                )
            }
        }
    }
}
