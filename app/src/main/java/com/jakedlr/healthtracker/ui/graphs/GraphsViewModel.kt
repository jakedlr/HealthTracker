package com.jakedlr.healthtracker.ui.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.domain.DailyValue
import com.jakedlr.healthtracker.domain.GraphRange
import com.jakedlr.healthtracker.domain.graphBounds
import com.jakedlr.healthtracker.domain.pointsInRange
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GraphsUiState(
    val loading: Boolean = true,
    /** Null when there is nothing to plot. */
    val bounds: ClosedRange<LocalDate>? = null,
    val weight: List<DailyValue> = emptyList(),
)

class GraphsViewModel(
    weightRepository: WeightRepository,
    private val clock: Clock,
) : ViewModel() {
    // The range selector arrives in milestone 5; until then always show everything.
    private val range = GraphRange.ALL_TIME

    val uiState: StateFlow<GraphsUiState> = weightRepository.observeAll()
        .map { entries ->
            val weight = entries.map { DailyValue(it.date, it.weightLbs) }
            // firstAny/lastAny will span food and sleep too once those exist.
            val bounds = graphBounds(
                range = range,
                today = LocalDate.now(clock),
                firstAny = weight.minOfOrNull { it.date },
                lastAny = weight.maxOfOrNull { it.date },
            )
            GraphsUiState(
                loading = false,
                bounds = bounds,
                weight = bounds?.let { pointsInRange(weight, it) }.orEmpty(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphsUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                GraphsViewModel(container.weightRepository, container.clock)
            }
        }
    }
}
