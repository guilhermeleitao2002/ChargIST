package pt.ist.cmu.chargist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.ui.screens.*
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.ui.viewmodel.UserViewModel

/* ------------------------------------------------------------------------- */
/*  Route helpers – keep every navigation string in one place                */
/* ------------------------------------------------------------------------- */
object Route {
    const val HOME           = "home"
    const val ADD_CHARGER    = "add_charger"
    const val SEARCH         = "search"
    const val PROFILE        = "profile"
    const val CHARGER_DETAIL = "charger_detail"
    const val SLOT_DETAIL    = "slot_detail"
    const val ADD_CHARGING_SLOT = "add_charging_slot"
    const val CHANGE_SLOT_DETAILS = "change_slot_details"

    fun charger(id: String) = "$CHARGER_DETAIL/$id"
    fun slot(id: String)    = "$SLOT_DETAIL/$id"
    fun addChargingSlot(chargerId: String) = "$ADD_CHARGING_SLOT/$chargerId"
    fun changeChargingSlotDetails(id: String) = "$CHANGE_SLOT_DETAILS/$id"
}

/* ------------------------------------------------------------------------- */
/*  Graph                                                                    */
/* ------------------------------------------------------------------------- */
@Composable
fun ChargISTNavigation() {

    val nav       = rememberNavController()
    val goHome    = remember { { nav.popBackStack(Route.HOME, false) } }
    val mapVM: MapViewModel = koinViewModel()
    val userVM: UserViewModel = koinViewModel()

    NavHost(
        navController    = nav,
        startDestination = Route.HOME
    ) {

        /* ── Home (map) ──────────────────────────────────────────────── */
        composable(Route.HOME) {
            HomeScreen(
                onChargerClick    = { id -> nav.navigate(Route.charger(id)) },
                onAddChargerClick = { nav.navigate(Route.ADD_CHARGER)      },
                onSearchClick     = { nav.navigate(Route.SEARCH)           },
                onProfileClick    = { nav.navigate(Route.PROFILE)          },
                mapViewModel      = mapVM,
                userViewModel     = userVM
            )
        }

        /* ── Add‑charger wizard ──────────────────────────────────────── */
        composable(Route.ADD_CHARGER) {
            AddChargerScreen(
                onBackClick = { nav.popBackStack() },
                onSuccess   = { goHome() }               // after saving -> back to map
            )
        }

        /* ── Search list ─────────────────────────────────────────────── */
        composable(Route.SEARCH) {
            SearchScreen(
                onBackClick   = { nav.popBackStack() },
                onChargerClick= { id -> nav.navigate(Route.charger(id)) }
            )
        }

        /* ── User profile ────────────────────────────────────────────── */
        composable(Route.PROFILE) {
            UserProfileScreen( onBackClick = { nav.popBackStack() } )
        }

        /* ── Charger details ─────────────────────────────────────────── */
        composable(
            route = "${Route.CHARGER_DETAIL}/{chargerId}",
            arguments = listOf(navArgument("chargerId"){ type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments!!.getString("chargerId")!!
            ChargerDetailScreen(
                chargerId = id,
                onBackClick = { nav.popBackStack() },
                onGoToMap = { nav.popBackStack(Route.HOME, false) },
                onViewSlotDetails = { slotId -> nav.navigate(Route.slot(slotId)) },
                onAddChargingSlot = { chargerId -> nav.navigate(Route.addChargingSlot(chargerId)) }, // Add this line
                chargerViewModel = koinViewModel(),
                mapViewModel = mapVM
            )
        }

        /* ── Single slot details ─────────────────────────────────────── */
        composable(
            route     = "${Route.SLOT_DETAIL}/{slotId}",
            arguments = listOf( navArgument("slotId"){ type = NavType.StringType } )
        ) { backStack ->
            val slotId = backStack.arguments!!.getString("slotId")!!
            ChargingSlotDetailScreen(
                slotId      = slotId,
                onBackClick = { nav.popBackStack() },
                onChangeChargingSlotDetails = {slotId -> nav.navigate(Route.changeChargingSlotDetails(slotId))}
            )
        }

        /* ── Add charging slot ───────────────────────────────────────── */
        composable(
            route = "${Route.ADD_CHARGING_SLOT}/{chargerId}",
            arguments = listOf(navArgument("chargerId") { type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments!!.getString("chargerId")!!
            AddChargingSlotScreen(
                chargerId = id,
                onBackClick = { nav.popBackStack() },
                onSuccess = { nav.popBackStack() }
            )
        }

        /*────Change Charging Slot Details─────────────────────────────*/

        composable(
            route = "${Route.CHANGE_SLOT_DETAILS}/{slotId}"
        ) { backStack ->
            val slotId = backStack.arguments!!.getString("slotId")!!
            ChangeSlotDetailScreen(
                slotId      = slotId,
                onBackClick = { nav.popBackStack() },
                onSuccess = { nav.popBackStack() }
            )
        }
    }
}