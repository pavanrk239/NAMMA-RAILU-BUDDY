package com.example.railubuddy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.railubuddy.screens.HomeScreen
import com.example.railubuddy.screens.JourneyScreen
import com.example.railubuddy.screens.StationScreen
import com.example.railubuddy.viewmodel.MainViewModel

@Composable
fun NavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        // Step 1: HOME - Show train journeys (Origin -> Destination)
        composable("home") {
            HomeScreen(navController = navController, viewModel = viewModel)
        }

        // Step 2: JOURNEY - Show vertical station timeline for a train
        composable(
            route = "journey_screen/{trainNumber}",
            arguments = listOf(navArgument("trainNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val trainNumber = backStackEntry.arguments?.getString("trainNumber") ?: ""
            JourneyScreen(navController = navController, viewModel = viewModel, trainNumber = trainNumber)
        }

        // Step 3: STATION - Show coach detail and 5KM alarm tracking
        composable(
            route = "station_screen/{stationId}",
            arguments = listOf(navArgument("stationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getString("stationId") ?: ""
            StationScreen(navController = navController, stationId = stationId, viewModel = viewModel)
        }
    }
}