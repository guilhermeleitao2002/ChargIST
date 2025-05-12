package pt.ist.cmu.chargist.ui.screens

/* ---------- imports ---------- */
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel

/* ------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChargerScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    chargerViewModel: ChargerViewModel = koinViewModel(),
    mapViewModel: MapViewModel     = koinViewModel()
) {
    /* ---------- state ---------- */
    val chargerCreationState by chargerViewModel.chargerCreationState.collectAsState()
    val mapState            by mapViewModel.mapState.collectAsState()
    val snackbarHostState   = remember { SnackbarHostState() }
    val coroutine           = rememberCoroutineScope()

    /* selected map location */
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    /* charging positions */
    var fastPositions   by remember { mutableIntStateOf(0) }
    var mediumPositions by remember { mutableIntStateOf(0) }
    var slowPositions   by remember { mutableIntStateOf(0) }

    /* camera position (IST fallback) */
    val defaultLocation = mapState.currentLocation ?: LatLng(38.7369, -9.1366)
    val cameraPosState  = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    /* ---------- activity result: pick image ---------- */
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { chargerViewModel.updateChargerCreationImage(it) } }
    )

    /* ---------- cheap address search (Geocoder) ---------- */
    var showAddressDialog by remember { mutableStateOf(false) }

    /* navigation on success */
    LaunchedEffect(chargerCreationState.isSuccess) {
        if (chargerCreationState.isSuccess) {
            onSuccess()
            chargerViewModel.resetChargerCreation()
        }
    }

    /* error snackbar */
    LaunchedEffect(chargerCreationState.error) {
        chargerCreationState.error?.let {
            coroutine.launch { snackbarHostState.showSnackbar(it) }
        }
    }

    /* Move camera to searched location */
    LaunchedEffect(mapState.searchedLocation) {
        mapState.searchedLocation?.let { location ->
            cameraPosState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
            selectedLocation = location
            chargerViewModel.updateChargerCreationLocation(location)
        }
    }

    /* ------------------------------ UI ------------------------------ */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Charging Station") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            /* ---------- name ---------- */
            OutlinedTextField(
                value         = chargerCreationState.name,
                onValueChange = { chargerViewModel.updateChargerCreationName(it) },
                label         = { Text("Station Name") },
                placeholder   = { Text("Enter station name") },
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            /* ---------- map ---------- */
            Text("Select Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                GoogleMap(
                    modifier            = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPosState,
                    properties          = MapProperties(isMyLocationEnabled = true),
                    uiSettings          = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false),
                    onMapClick          = { latLng ->
                        selectedLocation = latLng
                        chargerViewModel.updateChargerCreationLocation(latLng)
                    }
                ) {
                    selectedLocation?.let {
                        Marker(state = MarkerState(it), title = "Selected Location")
                    }
                }

                /* tiny search button (opens free‑tier dialog) */
                IconButton(
                    onClick  = { showAddressDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                ) { Icon(Icons.Default.Search, contentDescription = "Search address") }
            }

            Spacer(Modifier.height(16.dp))

            /* ---------- photo ---------- */
            Text("Add Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to select an image", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            /* ---------- charging positions ---------- */
            Text("Charging Positions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            ChargingPositionSelector("Fast Charging",   fastPositions)   { fastPositions   = it; chargerViewModel.updateFastPositions(it) }
            Spacer(Modifier.height(8.dp))
            ChargingPositionSelector("Medium Charging", mediumPositions) { mediumPositions = it; chargerViewModel.updateMediumPositions(it) }
            Spacer(Modifier.height(8.dp))
            ChargingPositionSelector("Slow Charging",   slowPositions)   { slowPositions   = it; chargerViewModel.updateSlowPositions(it) }

            Spacer(Modifier.height(32.dp))

            /* ---------- submit ---------- */
            Button(
                onClick = { chargerViewModel.createCharger() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !chargerCreationState.isSubmitting &&
                        chargerCreationState.name.isNotBlank() &&
                        chargerCreationState.location != null &&
                        (fastPositions > 0 || mediumPositions > 0 || slowPositions > 0)
            ) {
                if (chargerCreationState.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add Charging Station")
                }
            }

            Spacer(Modifier.height(32.dp))
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
                    value         = text,
                    onValueChange = { text = it },
                    label         = { Text("Enter address") },
                    singleLine    = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            coroutine.launch { mapViewModel.searchAddressUsingGeocoder(text) }
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

/* ------------------------------------------------------------------------- */

@Composable
fun ChargingPositionSelector(
    title: String,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        Slider(
            value         = count.toFloat(),
            onValueChange = { onCountChange(it.toInt()) },
            valueRange    = 0f..10f,
            steps         = 9
        )
    }
}