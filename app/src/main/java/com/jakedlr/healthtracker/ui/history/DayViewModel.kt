package com.jakedlr.healthtracker.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.SleepRepository
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.data.db.FoodLog
import com.jakedlr.healthtracker.data.db.SleepEntry
import com.jakedlr.healthtracker.data.db.WeightEntry
import com.jakedlr.healthtracker.domain.Nutrition
import com.jakedlr.healthtracker.domain.dailyNutrition
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DayUiState(
    val date: LocalDate,
    val zone: ZoneId,
    val loading: Boolean = true,
    val weight: WeightEntry? = null,
    val foodLogs: List<FoodLog> = emptyList(),
    /** Entries assigned to [date] by the sleep attribution rule. */
    val sleep: List<SleepEntry> = emptyList(),
    /** Null when no food was logged. */
    val nutrition: Nutrition? = null,
    /** Null when no sleep counts toward this day. */
    val sleepMillis: Long? = null,
    val confirmingWeightDelete: Boolean = false,
)

/** Everything recorded for one day, kept live so edits made elsewhere show up immediately. */
class DayViewModel(
    savedStateHandle: SavedStateHandle,
    private val weightRepository: WeightRepository,
    foodRepository: FoodRepository,
    sleepRepository: SleepRepository,
    clock: Clock,
) : ViewModel() {
    private val date: LocalDate = LocalDate.ofEpochDay(checkNotNull(savedStateHandle.get<Long>(DAY_ARG)))
    private val confirmingWeightDelete = MutableStateFlow(false)

    val uiState: StateFlow<DayUiState> = combine(
        weightRepository.observeWeight(date),
        foodRepository.observeLogsForDay(date, clock.zone),
        sleepRepository.observeSleepForDate(date, clock.zone),
        confirmingWeightDelete,
    ) { weight, logs, sleep, confirming ->
        DayUiState(
            date = date,
            zone = clock.zone,
            loading = false,
            weight = weight,
            foodLogs = logs,
            sleep = sleep,
            nutrition = dailyNutrition(logs.map { it.toLoggedFood() }, clock.zone)[date],
            sleepMillis = sleep.takeIf { it.isNotEmpty() }?.sumOf { it.endAt - it.startAt },
            confirmingWeightDelete = confirming,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState(date = date, zone = clock.zone))

    fun requestWeightDelete() = confirmingWeightDelete.update { true }

    fun cancelWeightDelete() = confirmingWeightDelete.update { false }

    fun confirmWeightDelete() {
        val entry = uiState.value.weight ?: return
        viewModelScope.launch {
            weightRepository.delete(entry)
            confirmingWeightDelete.value = false
        }
    }

    companion object {
        const val DAY_ARG = "day"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                DayViewModel(
                    createSavedStateHandle(),
                    container.weightRepository,
                    container.foodRepository,
                    container.sleepRepository,
                    container.clock,
                )
            }
        }
    }
}
