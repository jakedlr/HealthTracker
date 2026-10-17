package com.jakedlr.healthtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jakedlr.healthtracker.ui.DATE_ARG
import com.jakedlr.healthtracker.ui.NO_DATE
import com.jakedlr.healthtracker.ui.food.AddFoodScreen
import com.jakedlr.healthtracker.ui.food.EditFoodLogScreen
import com.jakedlr.healthtracker.ui.food.EditFoodLogViewModel
import com.jakedlr.healthtracker.ui.food.EditFoodScreen
import com.jakedlr.healthtracker.ui.food.EditFoodViewModel
import com.jakedlr.healthtracker.ui.food.FoodLibraryScreen
import com.jakedlr.healthtracker.ui.food.LogFoodScreen
import com.jakedlr.healthtracker.ui.food.LogFoodViewModel
import com.jakedlr.healthtracker.ui.food.NewFoodScreen
import com.jakedlr.healthtracker.ui.food.NewFoodViewModel
import com.jakedlr.healthtracker.ui.graphs.GraphsScreen
import com.jakedlr.healthtracker.ui.history.DayScreen
import com.jakedlr.healthtracker.ui.history.DayViewModel
import com.jakedlr.healthtracker.ui.history.HistoryScreen
import com.jakedlr.healthtracker.ui.home.HomeScreen
import com.jakedlr.healthtracker.ui.scan.ScanScreen
import com.jakedlr.healthtracker.ui.sleep.AddSleepScreen
import com.jakedlr.healthtracker.ui.sleep.AddSleepViewModel
import com.jakedlr.healthtracker.ui.weight.AddWeightScreen
import java.time.LocalDate

/**
 * Route patterns. Add flows take an optional [DATE_ARG] (epoch day) when started from a day in
 * History; it locks Add weight to that day and presets the date for food and sleep.
 */
object Routes {
    private const val BARCODE = NewFoodViewModel.BARCODE_ARG
    private const val ITEM_ID = LogFoodViewModel.ITEM_ID_ARG
    private const val SLEEP_ID = AddSleepViewModel.SLEEP_ID_ARG
    private const val DATE_Q = "$DATE_ARG={$DATE_ARG}"

    const val HOME = "home"
    const val GRAPHS = "graphs"
    const val HISTORY = "history"
    const val DAY = "day/{${DayViewModel.DAY_ARG}}"
    const val ADD_WEIGHT = "addWeight?$DATE_Q"
    const val SLEEP_FORM = "sleep?$SLEEP_ID={$SLEEP_ID}&$DATE_Q"
    const val ADD_FOOD = "addFood?$DATE_Q"
    const val SCAN = "scan?$DATE_Q"
    const val NEW_FOOD = "newFood?$BARCODE={$BARCODE}&$DATE_Q"
    const val LOG_FOOD = "logFood/{$ITEM_ID}?$DATE_Q"
    const val EDIT_FOOD_LOG = "editFoodLog/{${EditFoodLogViewModel.LOG_ID_ARG}}"
    const val FOOD_LIBRARY = "foodLibrary"
    const val EDIT_FOOD = "editFood/{${EditFoodViewModel.ITEM_ID_ARG}}"

    private fun d(date: Long) = "$DATE_ARG=$date"

    fun day(date: LocalDate) = "day/${date.toEpochDay()}"
    fun addWeight(date: Long = NO_DATE) = "addWeight?${d(date)}"
    fun addSleep(date: Long = NO_DATE) = "sleep?${d(date)}"
    fun editSleep(id: Long) = "sleep?$SLEEP_ID=$id"
    fun addFood(date: Long = NO_DATE) = "addFood?${d(date)}"
    fun scan(date: Long) = "scan?${d(date)}"
    fun newFood(date: Long, barcode: String? = null) =
        if (barcode == null) "newFood?${d(date)}" else "newFood?$BARCODE=$barcode&${d(date)}"
    fun logFood(itemId: Long, date: Long) = "logFood/$itemId?${d(date)}"
    fun editFoodLog(logId: Long) = "editFoodLog/$logId"
    fun editFood(itemId: Long) = "editFood/$itemId"
}

private val dateArgument: NamedNavArgument = navArgument(DATE_ARG) {
    type = NavType.LongType
    defaultValue = NO_DATE
}

private fun longArgument(name: String, default: Long? = null): NamedNavArgument = navArgument(name) {
    type = NavType.LongType
    if (default != null) defaultValue = default
}

private fun NavBackStackEntry.date(): Long = arguments?.getLong(DATE_ARG, NO_DATE) ?: NO_DATE

@Composable
fun HealthTrackerNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }
    val backToHome: () -> Unit = { navController.popBackStack(Routes.HOME, inclusive = false) }
    // Ends a food flow and returns to whoever started it (Home or a History day).
    val finishFoodFlow: () -> Unit = { navController.popBackStack(Routes.ADD_FOOD, inclusive = true) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddWeight = { navController.navigate(Routes.addWeight()) },
                onAddFood = { navController.navigate(Routes.addFood()) },
                onAddSleep = { navController.navigate(Routes.addSleep()) },
                onOpenFoodLibrary = { navController.navigate(Routes.FOOD_LIBRARY) },
                onOpenGraphs = { navController.navigate(Routes.GRAPHS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.ADD_WEIGHT, arguments = listOf(dateArgument)) {
            AddWeightScreen(onDone = back)
        }
        composable(
            Routes.SLEEP_FORM,
            arguments = listOf(longArgument(AddSleepViewModel.SLEEP_ID_ARG, AddSleepViewModel.NO_SLEEP_ID), dateArgument),
        ) {
            AddSleepScreen(onDone = back)
        }
        composable(Routes.GRAPHS) {
            GraphsScreen(onBack = backToHome)
        }

        // History
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = back, onDayClick = { navController.navigate(Routes.day(it)) })
        }
        composable(Routes.DAY, arguments = listOf(longArgument(DayViewModel.DAY_ARG))) {
            DayScreen(
                onBack = back,
                onEditWeight = { navController.navigate(Routes.addWeight(it.toEpochDay())) },
                onAddFood = { navController.navigate(Routes.addFood(it.toEpochDay())) },
                onEditFoodLog = { navController.navigate(Routes.editFoodLog(it)) },
                onAddSleep = { navController.navigate(Routes.addSleep(it.toEpochDay())) },
                onEditSleep = { navController.navigate(Routes.editSleep(it)) },
            )
        }
        composable(Routes.EDIT_FOOD_LOG, arguments = listOf(longArgument(EditFoodLogViewModel.LOG_ID_ARG))) {
            EditFoodLogScreen(onDone = back)
        }

        // Food flow: Add food → (Scan) → Log food / New food. The date is passed along each step.
        composable(Routes.ADD_FOOD, arguments = listOf(dateArgument)) { entry ->
            val date = entry.date()
            AddFoodScreen(
                onBack = back,
                onItemSelected = { navController.navigate(Routes.logFood(it, date)) },
                onScan = { navController.navigate(Routes.scan(date)) },
                onEnterManually = { navController.navigate(Routes.newFood(date)) },
            )
        }
        composable(Routes.SCAN, arguments = listOf(dateArgument)) { entry ->
            val date = entry.date()
            // Each outcome replaces the scanner, so Back from the next screen returns to Add food.
            val replaceScan: (String) -> Unit = { route ->
                navController.navigate(route) { popUpTo(Routes.SCAN) { inclusive = true } }
            }
            ScanScreen(
                onBack = back,
                onKnownItem = { replaceScan(Routes.logFood(it, date)) },
                onUnknownBarcode = { replaceScan(Routes.newFood(date, it)) },
                onEnterManually = { replaceScan(Routes.newFood(date)) },
            )
        }
        composable(
            Routes.NEW_FOOD,
            arguments = listOf(
                navArgument(NewFoodViewModel.BARCODE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                dateArgument,
            ),
        ) { entry ->
            val date = entry.date()
            NewFoodScreen(
                onBack = back,
                onDone = finishFoodFlow,
                // Swap this screen for Log food so Back returns to Add food.
                onUseExisting = { id ->
                    navController.navigate(Routes.logFood(id, date)) {
                        popUpTo(Routes.NEW_FOOD) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOG_FOOD, arguments = listOf(longArgument(LogFoodViewModel.ITEM_ID_ARG), dateArgument)) {
            LogFoodScreen(onBack = back, onDone = finishFoodFlow)
        }

        // Library
        composable(Routes.FOOD_LIBRARY) {
            FoodLibraryScreen(
                onBack = backToHome,
                onEditItem = { navController.navigate(Routes.editFood(it)) },
            )
        }
        composable(Routes.EDIT_FOOD, arguments = listOf(longArgument(EditFoodViewModel.ITEM_ID_ARG))) {
            EditFoodScreen(onDone = back)
        }
    }
}
