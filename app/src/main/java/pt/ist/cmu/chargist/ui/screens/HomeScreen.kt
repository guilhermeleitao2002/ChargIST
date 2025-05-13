package pt.ist.cmu.chargist.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChargerClick: (String) -> Unit,
    onAddChargerClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    mapViewModel: MapViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val mapState by mapViewModel.mapState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val coroutine = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) mapViewModel.onLocationPermissionGranted()
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val cameraPosState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            mapState.currentLocation ?: LatLng(38.7369, -9.1366),
            15f
        )
    }

    LaunchedEffect(Unit) {
        mapViewModel.focusRequests.collect { target ->
            cameraPosState.animate(
                update = CameraUpdateFactory.newLatLngZoom(target, 17f),
                durationMs = 750
            )
            mapViewModel.markFocusConsumed()
            mapViewModel.loadChargers()
        }
    }


    var showAddressDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Debounce para evitar requisições excessivas
    LaunchedEffect(searchQuery) {
        delay(300) // Debounce de 300ms
        if (searchQuery.isNotEmpty()) {
            mapViewModel.getAutocompleteSuggestions(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChargIST") },
                actions = {
                    IconButton(onClick = { showAddressDialog = true }) {
                        Icon(Icons.Default.LocationOn, "Search address")
                    }
                    IconButton(onClick = onSearchClick) {
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
        Box(Modifier.fillMaxSize().padding(pad)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPosState,
                properties = MapProperties(
                    isMyLocationEnabled = mapState.hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = false
                )
            ) {
                mapState.chargers.forEach { charger ->
                    val fav = userState.user?.id?.let { charger.favoriteUsers.contains(it) } ?: false
                    Marker(
                        state = MarkerState(charger.getLatLng()),
                        title = charger.name,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (fav) BitmapDescriptorFactory.HUE_ROSE
                            else BitmapDescriptorFactory.HUE_GREEN
                        ),
                        onClick = {
                            onChargerClick(charger.id)
                            true
                        }
                    )
                }
            }

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

    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Search Address") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Enter address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Dropdown de sugestões
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(mapState.autocompleteSuggestions) { prediction ->
                            Text(
                                text = prediction.getFullText(null).toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = prediction.getFullText(null).toString()
                                        mapViewModel.searchAddressUsingPlaceId(prediction.placeId)
                                        showAddressDialog = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            coroutine.launch {
                                mapViewModel.searchAddressUsingPlaceId(mapState.autocompleteSuggestions.firstOrNull()?.placeId ?: "")
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

@Composable
fun ChargerMarker(
    charger: Charger,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val hue = if (isFavorite) BitmapDescriptorFactory.HUE_ROSE
    else BitmapDescriptorFactory.HUE_GREEN

    Marker(
        state = MarkerState(charger.getLatLng()),
        title = charger.name,
        icon = BitmapDescriptorFactory.defaultMarker(hue),
        onClick = { onClick(); true }
    )
}