package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.LocationOn
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.Charger
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import android.Manifest
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import pt.ist.cmu.chargist.util.PlaceSearch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChargerClick: (String) -> Unit,
    onAddChargerClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    //viewModel: MapViewModel = koinViewModel(),
    mapViewModel: MapViewModel
) {
    var showManualSearchDialog by remember { mutableStateOf(false) }

    /* ── 1. Ask for ACCESS_FINE_LOCATION once ─────────────────────── */
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) mapViewModel.onLocationPermissionGranted()
        }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Register the place search launcher
    val searchAddressLauncher = rememberLauncherForActivityResult(
        contract = PlaceSearch()
    ) { latLng ->
        latLng?.let {
            // Move camera to the selected location
            mapViewModel.setLocationAndMoveCameraManually(it)
        }
    }

    val mapState by mapViewModel.mapState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId) {
        if (userId != null) {
            mapViewModel.loadFavoriteChargers2(userId)
        }
    }

    // Initial position - IST Campus (Alameda)
    var defaultLocation by remember {
        mutableStateOf(LatLng(38.7369, -9.1366))
    }

    // If user's current location is available, use it
    LaunchedEffect(mapState.currentLocation) {
        mapState.currentLocation?.let {
            defaultLocation = it
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    //aquipackage pt.ist.cmu.chargist.ui.screens
    //
    //import androidx.compose.foundation.layout.Box
    //import androidx.compose.foundation.layout.fillMaxSize
    //import androidx.compose.foundation.layout.padding
    //import androidx.compose.material.icons.Icons
    //import androidx.compose.material.icons.filled.Add
    //import androidx.compose.material.icons.filled.Person
    //import androidx.compose.material.icons.filled.Search
    //import androidx.compose.material3.ExperimentalMaterial3Api
    //import androidx.compose.material3.FloatingActionButton
    //import androidx.compose.material3.Icon
    //import androidx.compose.material3.IconButton
    //import androidx.compose.material3.MaterialTheme
    //import androidx.compose.material3.Scaffold
    //import androidx.compose.material3.Text
    //import androidx.compose.material3.TopAppBar
    //import androidx.compose.material3.TopAppBarDefaults
    //import androidx.compose.runtime.Composable
    //import androidx.compose.runtime.LaunchedEffect
    //import androidx.compose.runtime.collectAsState
    //import androidx.compose.runtime.getValue
    //import androidx.compose.runtime.mutableStateOf
    //import androidx.compose.runtime.remember
    //import androidx.compose.runtime.setValue
    //import androidx.compose.ui.Alignment
    //import androidx.compose.ui.Modifier
    //import androidx.compose.ui.graphics.Color
    //import androidx.compose.ui.unit.dp
    //import com.google.android.gms.maps.model.BitmapDescriptorFactory
    //import com.google.android.gms.maps.model.CameraPosition
    //import com.google.android.gms.maps.model.LatLng
    //import com.google.maps.android.compose.GoogleMap
    //import com.google.maps.android.compose.MapProperties
    //import com.google.maps.android.compose.MapUiSettings
    //import com.google.maps.android.compose.Marker
    //import com.google.maps.android.compose.MarkerState
    //import com.google.maps.android.compose.rememberCameraPositionState
    //import androidx.activity.compose.rememberLauncherForActivityResult
    //import androidx.compose.material.icons.filled.LocationOn
    //import org.koin.androidx.compose.koinViewModel
    //import pt.ist.cmu.chargist.data.model.Charger
    //import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
    //import android.Manifest
    //import android.util.Log
    //import androidx.activity.result.contract.ActivityResultContracts
    //import androidx.compose.material3.AlertDialog
    //import androidx.compose.material3.Button
    //import androidx.compose.material3.OutlinedTextField
    //import androidx.compose.material3.SnackbarHostState
    //import androidx.compose.material3.TextButton
    //import androidx.compose.runtime.rememberCoroutineScope
    //import com.google.android.gms.maps.CameraUpdateFactory
    //import com.google.android.libraries.places.api.Places
    //import com.google.firebase.auth.FirebaseAuth
    //import pt.ist.cmu.chargist.util.PlaceSearch
    //
    //
    //@OptIn(ExperimentalMaterial3Api::class)
    //@Composable
    //fun HomeScreen(
    //    onChargerClick: (String) -> Unit,
    //    onAddChargerClick: () -> Unit,
    //    onSearchClick: () -> Unit,
    //    onProfileClick: () -> Unit,
    //    viewModel: MapViewModel = koinViewModel()
    //) {
    //    var showManualSearchDialog by remember { mutableStateOf(false) }
    //
    //    /* ── 1. Ask for ACCESS_FINE_LOCATION once ─────────────────────── */
    //    val permissionLauncher =
    //        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    //            if (granted) viewModel.onLocationPermissionGranted()
    //        }
    //
    //    LaunchedEffect(Unit) {
    //        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    //    }
    //
    //    // Register the place search launcher
    //    val searchAddressLauncher = rememberLauncherForActivityResult(
    //        contract = PlaceSearch()
    //    ) { latLng ->
    //        latLng?.let {
    //            // Move camera to the selected location
    //            viewModel.setLocationAndMoveCameraManually(it)
    //        }
    //    }
    //
    //    val mapState by viewModel.mapState.collectAsState()
    //    val scope = rememberCoroutineScope()
    //    val snackbarHostState = remember { SnackbarHostState() }
    //
    //    val userId = FirebaseAuth.getInstance().currentUser?.uid
    //
    //    LaunchedEffect(userId) {
    //        if (userId != null) {
    //            viewModel.loadFavoriteChargers2(userId)
    //        }
    //    }
    //
    //    // Initial position - IST Campus (Alameda)
    //    var defaultLocation by remember {
    //        mutableStateOf(LatLng(38.7369, -9.1366))
    //    }
    //
    //    // If user's current location is available, use it
    //    LaunchedEffect(mapState.currentLocation) {
    //        mapState.currentLocation?.let {
    //            defaultLocation = it
    //        }
    //    }
    //
    //    val cameraPositionState = rememberCameraPositionState {
    //        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    //    }
    //
    //    //aqui
    //    LaunchedEffect(Unit) {
    //        viewModel.focusRequests.collect { target ->
    //            cameraPositionState.animate(
    //                update = CameraUpdateFactory.newLatLngZoom(target, 17f),
    //                durationMs = 750
    //            )
    //        }
    //    }
    //
    //    val mapUiSettings = remember {
    //        MapUiSettings(
    //            zoomControlsEnabled = false,
    //            myLocationButtonEnabled = true
    //        )
    //    }
    //
    //    val mapProperties = remember {
    //        MapProperties(
    //            isMyLocationEnabled = true
    //        )
    //    }
    //
    //    Scaffold(
    //        topBar = {
    //            TopAppBar(
    //                title = { Text("ChargIST") },
    //                actions = {
    //                    IconButton(
    //                        onClick = {
    //                            try {
    //                                searchAddressLauncher.launch(Unit)
    //                            } catch (e: Exception) {
    //                                Log.e("HomeScreen", "Error launching Places: ${e.message}")
    //                                // Show our fallback dialog instead
    //                                showManualSearchDialog = true
    //                            }
    //                        }
    //                    ) {
    //                        Icon(
    //                            imageVector = Icons.Default.LocationOn,
    //                            contentDescription = "Search Address"
    //                        )
    //                    }
    //                    IconButton(onClick = onSearchClick) {
    //                        Icon(
    //                            imageVector = Icons.Default.Search,
    //                            contentDescription = "Search"
    //                        )
    //                    }
    //                    IconButton(onClick = onProfileClick) {
    //                        Icon(
    //                            imageVector = Icons.Default.Person,
    //                            contentDescription = "Profile"
    //                        )
    //                    }
    //                },
    //                colors = TopAppBarDefaults.topAppBarColors(
    //                    containerColor = MaterialTheme.colorScheme.primaryContainer,
    //                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    //                )
    //            )
    //        },
    //        floatingActionButton = {
    //            FloatingActionButton(
    //                onClick = onAddChargerClick,
    //                containerColor = MaterialTheme.colorScheme.primary
    //            ) {
    //                Icon(
    //                    imageVector = Icons.Default.Add,
    //                    contentDescription = "Add Charger",
    //                    tint = Color.White
    //                )
    //            }
    //        }
    //    ) { innerPadding ->
    //        Box(
    //            modifier = Modifier
    //                .fillMaxSize()
    //                .padding(innerPadding)
    //        ) {
    //            GoogleMap(
    //                modifier = Modifier.fillMaxSize(),
    //                cameraPositionState = cameraPositionState,
    //                properties = MapProperties(
    //                    isMyLocationEnabled = mapState.hasLocationPermission   // ← key line
    //                ),
    //                uiSettings = mapUiSettings,
    //                onMapLoaded = {
    //                    // When map is loaded, get chargers in current visible area
    //                    val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
    //                    if (bounds != null) {
    //                        viewModel.loadChargersInBounds(bounds)
    //                    }
    //                }
    //            ) {
    //                // Display all chargers on the map
    //                mapState.chargers.forEach { charger ->
    //                    val isFavorite = mapState.favoriteChargers.any { it.id == charger.id }
    //                    ChargerMarker(
    //                        charger = charger,
    //                        isFavorite = isFavorite,
    //                        onClick = { onChargerClick(charger.id) }
    //                    )
    //                }
    //            }
    //
    //            // Error message if any
    //            if (mapState.error != null) {
    //                Box(
    //                    modifier = Modifier
    //                        .align(Alignment.TopCenter)
    //                        .padding(16.dp),
    //                    contentAlignment = Alignment.Center
    //                ) {
    //                    Text(
    //                        text = mapState.error ?: "",
    //                        style = MaterialTheme.typography.bodyMedium,
    //                        color = MaterialTheme.colorScheme.error,
    //                        modifier = Modifier.padding(8.dp)
    //                    )
    //                }
    //            }
    //
    //            if (showManualSearchDialog) {
    //                var addressInput by remember { mutableStateOf("") }
    //
    //                AlertDialog(
    //                    onDismissRequest = { showManualSearchDialog = false },
    //                    title = { Text("Search Address") },
    //                    text = {
    //                        OutlinedTextField(
    //                            value = addressInput,
    //                            onValueChange = { addressInput = it },
    //                            label = { Text("Enter address") },
    //                            singleLine = true
    //                        )
    //                    },
    //                    confirmButton = {
    //                        Button(
    //                            onClick = {
    //                                if (addressInput.isNotBlank()) {
    //                                    viewModel.searchAddressUsingGeocoder(addressInput)
    //                                }
    //                                showManualSearchDialog = false
    //                            }
    //                        ) {
    //                            Text("Search")
    //                        }
    //                    },
    //                    dismissButton = {
    //                        TextButton(onClick = { showManualSearchDialog = false }) {
    //                            Text("Cancel")
    //                        }
    //                    }
    //                )
    //            }
    //        }
    //    }
    //}
    //
    //
    //
    //@Composable
    //fun ChargerMarker(
    //    charger: Charger,
    //    isFavorite: Boolean,
    //    onClick: () -> Unit
    //) {
    //    // Different marker color for favorite chargers
    //    val markerColor = if (isFavorite) {
    //        BitmapDescriptorFactory.HUE_ROSE
    //    } else {
    //        BitmapDescriptorFactory.HUE_GREEN
    //    }
    //
    //    Marker(
    //        state = MarkerState(position = charger.getLatLng()),
    //        title = charger.name,
    //        snippet = "Tap for details",
    //        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
    //        onClick = {
    //            onClick()
    //            true // Consume the event
    //        }
    //    )
    //}
    LaunchedEffect(Unit) {
        mapViewModel.focusRequests.collect { target ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(target, 17f),
                durationMs = 750
            )
            mapViewModel.markFocusConsumed()
        }
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true
        )
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = true
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChargIST") },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                searchAddressLauncher.launch(Unit)
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Error launching Places: ${e.message}")
                                // Show our fallback dialog instead
                                showManualSearchDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Search Address"
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddChargerClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Charger",
                    tint = Color.White
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = mapState.hasLocationPermission   // ← key line
                ),
                uiSettings = mapUiSettings,
                onMapLoaded = {
                    // When map is loaded, get chargers in current visible area
                    val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                    if (bounds != null) {
                        mapViewModel.loadChargersInBounds(bounds)
                    }
                }
            ) {
                // Display all chargers on the map
                mapState.chargers.forEach { charger ->
                    val isFavorite = mapState.favoriteChargers.any { it.id == charger.id }
                    ChargerMarker(
                        charger = charger,
                        isFavorite = isFavorite,
                        onClick = { onChargerClick(charger.id) }
                    )
                }
            }

            // Error message if any
            if (mapState.error != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mapState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (showManualSearchDialog) {
                var addressInput by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showManualSearchDialog = false },
                    title = { Text("Search Address") },
                    text = {
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("Enter address") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (addressInput.isNotBlank()) {
                                    mapViewModel.searchAddressUsingGeocoder(addressInput)
                                }
                                showManualSearchDialog = false
                            }
                        ) {
                            Text("Search")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showManualSearchDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}



@Composable
fun ChargerMarker(
    charger: Charger,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    // Different marker color for favorite chargers
    val markerColor = if (isFavorite) {
        BitmapDescriptorFactory.HUE_ROSE
    } else {
        BitmapDescriptorFactory.HUE_GREEN
    }

    Marker(
        state = MarkerState(position = charger.getLatLng()),
        title = charger.name,
        snippet = "Tap for details",
        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
        onClick = {
            onClick()
            true // Consume the event
        }
    )
}