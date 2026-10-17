package com.jakedlr.healthtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jakedlr.healthtracker.ui.food.AddFoodScreen
import com.jakedlr.healthtracker.ui.food.EditFoodScreen
import com.jakedlr.healthtracker.ui.food.EditFoodViewModel
import com.jakedlr.healthtracker.ui.food.FoodLibraryScreen
import com.jakedlr.healthtracker.ui.food.LogFoodScreen
import com.jakedlr.healthtracker.ui.food.LogFoodViewModel
import com.jakedlr.healthtracker.ui.food.NewFoodScreen
import com.jakedlr.healthtracker.ui.food.NewFoodViewModel
import com.jakedlr.healthtracker.ui.graphs.GraphsScreen
import com.jakedlr.healthtracker.ui.home.HomeScreen
import com.jakedlr.healthtracker.ui.scan.ScanScreen
import com.jakedlr.healthtracker.ui.weight.AddWeightScreen

object Routes {
    const val HOME = "home"
    const val ADD_WEIGHT = "addWeight"
    const val GRAPHS = "graphs"
    const val ADD_FOOD = "addFood"
    const val SCAN = "scan"
    const val NEW_FOOD = "newFood?${NewFoodViewModel.BARCODE_ARG}={${NewFoodViewModel.BARCODE_ARG}}"
    const val LOG_FOOD = "logFood/{${LogFoodViewModel.ITEM_ID_ARG}}"
    const val FOOD_LIBRARY = "foodLibrary"
    const val EDIT_FOOD = "editFood/{${EditFoodViewModel.ITEM_ID_ARG}}"

    fun newFood(barcode: String? = null) =
        if (barcode == null) "newFood" else "newFood?${NewFoodViewModel.BARCODE_ARG}=$barcode"
    fun logFood(itemId: Long) = "logFood/$itemId"
    fun editFood(itemId: Long) = "editFood/$itemId"
}

@Composable
fun HealthTrackerNavHost() {
    val navController = rememberNavController()
    val backToHome: () -> Unit = { navController.popBackStack(Routes.HOME, inclusive = false) }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddWeight = { navController.navigate(Routes.ADD_WEIGHT) },
                onAddFood = { navController.navigate(Routes.ADD_FOOD) },
                onOpenFoodLibrary = { navController.navigate(Routes.FOOD_LIBRARY) },
                onOpenGraphs = { navController.navigate(Routes.GRAPHS) },
            )
        }
        composable(Routes.ADD_WEIGHT) {
            AddWeightScreen(onDone = backToHome)
        }
        composable(Routes.GRAPHS) {
            GraphsScreen(onBack = backToHome)
        }
        composable(Routes.ADD_FOOD) {
            AddFoodScreen(
                onBack = backToHome,
                onItemSelected = { navController.navigate(Routes.logFood(it)) },
                onScan = { navController.navigate(Routes.SCAN) },
                onEnterManually = { navController.navigate(Routes.newFood()) },
            )
        }
        composable(Routes.SCAN) {
            // Each outcome replaces the scanner, so Back from the next screen returns to Add food.
            val replaceScan: (String) -> Unit = { route ->
                navController.navigate(route) { popUpTo(Routes.SCAN) { inclusive = true } }
            }
            ScanScreen(
                onBack = { navController.popBackStack() },
                onKnownItem = { replaceScan(Routes.logFood(it)) },
                onUnknownBarcode = { replaceScan(Routes.newFood(it)) },
                onEnterManually = { replaceScan(Routes.newFood()) },
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
            ),
        ) {
            NewFoodScreen(
                onBack = { navController.popBackStack() },
                onDone = backToHome,
                // Swap this screen for Log food so Back returns to Add food.
                onUseExisting = { id ->
                    navController.navigate(Routes.logFood(id)) {
                        popUpTo(Routes.NEW_FOOD) { inclusive = true }
                    }
                },
            )
        }
        composable(
            Routes.LOG_FOOD,
            arguments = listOf(navArgument(LogFoodViewModel.ITEM_ID_ARG) { type = NavType.LongType }),
        ) {
            LogFoodScreen(onBack = { navController.popBackStack() }, onDone = backToHome)
        }
        composable(Routes.FOOD_LIBRARY) {
            FoodLibraryScreen(
                onBack = backToHome,
                onEditItem = { navController.navigate(Routes.editFood(it)) },
            )
        }
        composable(
            Routes.EDIT_FOOD,
            arguments = listOf(navArgument(EditFoodViewModel.ITEM_ID_ARG) { type = NavType.LongType }),
        ) {
            EditFoodScreen(onDone = { navController.popBackStack() })
        }
    }
}
