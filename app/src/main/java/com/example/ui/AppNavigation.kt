package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.UserRole
import com.example.viewmodel.TournamentViewModel
import com.example.ui.screens.*

@Composable
fun MainApp(viewModel: TournamentViewModel) {
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()

    if (userRole == UserRole.NONE) {
        LoginScreen(viewModel = viewModel, onLoginSuccess = {
            // Recomposes automatically because userRole state flow is updated
        })
    } else {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "fixtures"

        val tabs = listOf(
            Pair("fixtures", "Fikstür"),
            Pair("standings", "Puan Durumu"),
            Pair("knockout", "Elemeler"),
            Pair("scorers", "Gol Krallığı"),
            Pair("settings", "Ayarlar")
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEach { (route, label) ->
                        NavigationBarItem(
                            icon = { 
                                when(route) {
                                    "fixtures" -> Icon(Icons.Default.SportsSoccer, contentDescription = null)
                                    "standings" -> Icon(Icons.Default.TableChart, contentDescription = null)
                                    "knockout" -> Icon(Icons.Default.AccountTree, contentDescription = null)
                                    "scorers" -> Icon(Icons.Default.DirectionsRun, contentDescription = null)
                                    "settings" -> Icon(Icons.Default.Settings, contentDescription = null)
                                    else -> Icon(Icons.Default.Star, contentDescription = null)
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "fixtures",
                modifier = Modifier.padding(padding)
            ) {
                composable("fixtures") { FixturesScreen(viewModel) }
                composable("standings") { StandingsScreen(viewModel) }
                composable("knockout") { KnockoutScreen(viewModel) }
                composable("scorers") { ScorersScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
            }
        }
    }
}
