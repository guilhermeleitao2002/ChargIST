package pt.ist.cmu.chargist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pt.ist.cmu.chargist.ui.screens.AddChargerScreen
import pt.ist.cmu.chargist.ui.screens.ChargerDetailScreen
import pt.ist.cmu.chargist.ui.screens.ChargingSlotDetailScreen
import pt.ist.cmu.chargist.ui.screens.HomeScreen
import pt.ist.cmu.chargist.ui.screens.SearchScreen
import pt.ist.cmu.chargist.ui.screens.UserProfileScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddCharger : Screen("add_charger")
    object ChargerDetail : Screen("charger_detail/{chargerId}") {
        fun createRoute(chargerId: String) = "charger_detail/$chargerId"
    }
    object ChargingSlotDetail : Screen("charging_slot_detail/{slotId}") {
        fun createRoute(slotId: String) = "charging_slot_detail/$slotId"
    }
    object Search : Screen("search")
    object UserProfile : Screen("user_profile")
}



@Composable
fun ChargISTNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onChargerClick = { chargerId ->
                    navController.navigate(Screen.ChargerDetail.createRoute(chargerId))
                },
                onAddChargerClick = {
                    navController.navigate(Screen.AddCharger.route)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.UserProfile.route)
                }
            )
        }

        composable(Screen.AddCharger.route) {
            AddChargerScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ChargerDetail.route,
            arguments = listOf(
                navArgument("chargerId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val chargerId = backStackEntry.arguments?.getString("chargerId") ?: ""
            ChargerDetailScreen(
                chargerId = chargerId,
                onBackClick = {
                    navController.popBackStack()
                },
                onSlotClick = { slotId ->
                    navController.navigate(Screen.ChargingSlotDetail.createRoute(slotId))
                }
            )
        }

        composable(
            route = Screen.ChargingSlotDetail.route,
            arguments = listOf(
                navArgument("slotId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val slotId = backStackEntry.arguments?.getString("slotId") ?: ""
            ChargingSlotDetailScreen(
                slotId = slotId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onChargerClick = { chargerId ->
                    navController.navigate(Screen.ChargerDetail.createRoute(chargerId))
                }
            )
        }

        composable(Screen.UserProfile.route) {
            UserProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}