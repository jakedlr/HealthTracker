package com.jakedlr.healthtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jakedlr.healthtracker.ui.graphs.GraphsScreen
import com.jakedlr.healthtracker.ui.home.HomeScreen
import com.jakedlr.healthtracker.ui.weight.AddWeightScreen

object Routes {
    const val HOME = "home"
    const val ADD_WEIGHT = "addWeight"
    const val GRAPHS = "graphs"
}

@Composable
fun HealthTrackerNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddWeight = { navController.navigate(Routes.ADD_WEIGHT) },
                onOpenGraphs = { navController.navigate(Routes.GRAPHS) },
            )
        }
        composable(Routes.ADD_WEIGHT) {
            AddWeightScreen(onDone = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
        composable(Routes.GRAPHS) {
            GraphsScreen(onBack = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
    }
}
