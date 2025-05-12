package pt.ist.cmu.chargist.ui.screens

/* ---------- imports ---------- */
import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.ui.viewmodel.UserViewModel

/* ───────────────────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChargerClick:    (String) -> Unit,
    onAddChargerClick: () -> Unit,
    onSearchClick:     () -> Unit,
    onProfileClick:    () -> Unit,
    mapViewModel:      MapViewModel  = koinViewModel(),
    userViewModel:     UserViewModel = koinViewModel()
) {
    /* ---------- permission helper ---------- */
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) mapViewModel.onLocationPermissionGranted()
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /* ---------- state ---------- */
    val mapState   by mapViewModel.mapState.collectAsState()
    val userState  by userViewModel.userState.collectAsState()
    val coroutine  = rememberCoroutineScope()

    /* ---------- camera ---------- */
    val cameraPosState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapState.currentLocation ?: LatLng(38.7369, -9.1366),
            15f
        )
    }

    /* respond to “focusOn()” requests from ChargerDetailScreen */
    LaunchedEffect(Unit) {
        mapViewModel.focusRequests.collect { target ->
            cameraPosState.animate(
                update     = CameraUpdateFactory.newLatLngZoom(target, 17f),
                durationMs = 750
            )
            mapViewModel.markFocusConsumed()
            mapViewModel.loadChargers()
        }
    }

    /* Move camera to searched location */
    LaunchedEffect(mapState.searchedLocation) {
        mapState.searchedLocation?.let { location ->
            cameraPosState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    /* ---------- simple free‑tier address dialog ---------- */
    var showAddressDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text("ChargIST") },
                actions = {
                    IconButton(onClick = { showAddressDialog = true }) {
                        Icon(Icons.Default.LocationOn, "Search address")
                    }
                    IconButton(onClick = onSearchClick)  {
                        Icon(Icons.Default.Search, "Search chargers")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddChargerClick,
                containerColor = MaterialTheme.colorScheme.primary

            ) { Icon(Icons.Default.Add, "Add charger", tint = Color.White) }
        }
    ) { pad ->
        /* ------------------ MAP ------------------ */
        Box(Modifier.fillMaxSize().padding(pad)) {
            GoogleMap(
                modifier            = Modifier.fillMaxSize(),
                cameraPositionState = cameraPosState,
                properties          = MapProperties(
                    isMyLocationEnabled = mapState.hasLocationPermission
                ),
                uiSettings          = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled     = false
                )
            ) {
                mapState.chargers.forEach { charger ->
                    val fav = userState.user?.id?.let { charger.favoriteUsers.contains(it) } ?: false
                    Marker(
                        state = MarkerState(charger.getLatLng()),
                        title = charger.name,
                        icon  = BitmapDescriptorFactory.defaultMarker(
                            if (fav) BitmapDescriptorFactory.HUE_ROSE
                            else      BitmapDescriptorFactory.HUE_GREEN
                        ),
                        onClick = {
                            onChargerClick(charger.id)
                            true
                        }
                    )
                }
            }

            /* error banner */
            mapState.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }
    }

    /* ---------- address input dialog ---------- */
    if (showAddressDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title            = { Text("Search Address") },
            text             = {
                OutlinedTextField(
                    value       = text,
                    onValueChange = { text = it },
                    label       = { Text("Enter address") },
                    singleLine  = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            coroutine.launch {
                                mapViewModel.searchAddressUsingGeocoder(text)
                            }
                        }
                        showAddressDialog = false
                    }
                ) { Text("Search") }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/* ---------- marker composable (unchanged) ---------- */
@Composable
fun ChargerMarker(
    charger:     Charger,
    isFavorite:  Boolean,
    onClick:     () -> Unit
) {
    val hue = if (isFavorite) BitmapDescriptorFactory.HUE_ROSE
    else           BitmapDescriptorFactory.HUE_GREEN

    Marker(
        state = MarkerState(charger.getLatLng()),
        title = charger.name,
        icon  = BitmapDescriptorFactory.defaultMarker(hue),
        onClick = { onClick(); true }
    )
}