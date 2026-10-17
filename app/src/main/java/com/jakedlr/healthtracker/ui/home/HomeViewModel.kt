package com.jakedlr.healthtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jakedlr.healthtracker.appContainer
import com.jakedlr.healthtracker.data.FoodRepository
import com.jakedlr.healthtracker.data.WeightRepository
import com.jakedlr.healthtracker.domain.Nutrition
import com.jakedlr.healthtracker.domain.dailyNutrition
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val today: LocalDate,
    val todayWeightLbs: Double? = null,
    /** Null when nothing has been logged today. */
    val todayNutrition: Nutrition? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val weightRepository: WeightRepository,
    private val foodRepository: FoodRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<HomeUiState> = today
        .flatMapLatest { date ->
            combine(
                weightRepository.observeWeight(date),
                foodRepository.observeLogsForDay(date, clock.zone),
            ) { weight, logs ->
                HomeUiState(
                    today = date,
                    todayWeightLbs = weight?.weightLbs,
                    todayNutrition = dailyNutrition(logs.map { it.toLoggedFood() }, clock.zone)[date],
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(today.value))

    /** Call on resume so the screen rolls over to the new day if the app stayed open past midnight. */
    fun refreshToday() {
        today.value = LocalDate.now(clock)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(container.weightRepository, container.foodRepository, container.clock)
            }
        }
    }
}
