package pt.ist.cmu.chargist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.util.PlaceSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChargerScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    chargerViewModel: ChargerViewModel = koinViewModel(),
    mapViewModel: MapViewModel = koinViewModel()
) {
    val chargerCreationState by chargerViewModel.chargerCreationState.collectAsState()
    val mapState by mapViewModel.mapState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Selected location on map
    var selectedLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    // Track charging positions
    var fastPositions by remember { mutableIntStateOf(0) }
    var mediumPositions by remember { mutableIntStateOf(0) }
    var slowPositions by remember { mutableIntStateOf(0) }

    // Camera position - start with current location or IST
    val defaultLocation = mapState.currentLocation ?: LatLng(38.7369, -9.1366)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { chargerViewModel.updateChargerCreationImage(it) }
        }
    )

    val searchAddressLauncher = rememberLauncherForActivityResult(
        contract = PlaceSearch()
    ) { latLng ->
        latLng?.let {
            selectedLocation = it
            chargerViewModel.updateChargerCreationLocation(it)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
        }
    }

    // Check for success and navigate back
    LaunchedEffect(chargerCreationState.isSuccess) {
        if (chargerCreationState.isSuccess) {
            onSuccess()
            chargerViewModel.resetChargerCreation()
        }
    }

    // Show error in snackbar if any
    LaunchedEffect(chargerCreationState.error) {
        chargerCreationState.error?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Charging Station") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Name field
            OutlinedTextField(
                value = chargerCreationState.name,
                onValueChange = { chargerViewModel.updateChargerCreationName(it) },
                label = { Text("Station Name") },
                placeholder = { Text("Enter station name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Map for location selection
            Text(
                text = "Select Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        chargerViewModel.updateChargerCreationLocation(latLng)
                    }
                ) {
                    // Show marker for selected location
                    selectedLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Selected Location"
                        )
                    }
                }

                IconButton(
                    onClick = {
                        searchAddressLauncher.launch(Unit)
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }

                // Current location button
                IconButton(
                    onClick = {
                        mapState.currentLocation?.let {
                            selectedLocation = it
                            chargerViewModel.updateChargerCreationLocation(it)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Use Current Location"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo selection
            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (chargerCreationState.imageUri != null) {
                    AsyncImage(
                        model = chargerCreationState.imageUri,
                        contentDescription = "Charger Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tap to select an image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Charging positions
            Text(
                text = "Charging Positions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fast charging positions
            ChargingPositionSelector(
                title = "Fast Charging",
                count = fastPositions,
                onCountChange = { fastPositions = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Medium charging positions
            ChargingPositionSelector(
                title = "Medium Charging",
                count = mediumPositions,
                onCountChange = { mediumPositions = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slow charging positions
            ChargingPositionSelector(
                title = "Slow Charging",
                count = slowPositions,
                onCountChange = { slowPositions = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Submit button
            Button(
                onClick = { chargerViewModel.createCharger() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !chargerCreationState.isSubmitting &&
                        chargerCreationState.name.isNotBlank() &&
                        chargerCreationState.location != null &&
                        (fastPositions > 0 || mediumPositions > 0 || slowPositions > 0)
            ) {
                if (chargerCreationState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Charging Station")
                }
            }

            // Add some space at the bottom
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ChargingPositionSelector(
    title: String,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = count.toFloat(),
            onValueChange = { onCountChange(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9 // 10 possible positions (0-10)
        )
    }
}