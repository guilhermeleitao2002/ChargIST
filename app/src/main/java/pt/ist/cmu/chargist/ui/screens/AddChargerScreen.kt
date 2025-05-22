package pt.ist.cmu.chargist.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.PaymentSystem
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.data.model.ConnectorType
import androidx.compose.ui.text.input.KeyboardType

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
    val coroutine = rememberCoroutineScope()

    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    var fastPositions by remember { mutableIntStateOf(0) }
    var mediumPositions by remember { mutableIntStateOf(0) }
    var slowPositions by remember { mutableIntStateOf(0) }

    // Available payment systems
    val availablePaymentSystems = remember {
        listOf(
            PaymentSystem(id = "visa", name = "VISA"),
            PaymentSystem(id = "mastercard", name = "Mastercard"),
            PaymentSystem(id = "mbway", name = "MBWay"),
            PaymentSystem(id = "googlepay", name = "Google Pay")
        )
    }

    // Track selected payment systems
    val selectedPaymentSystems = remember { mutableStateListOf<PaymentSystem>() }

    val defaultLocation = mapState.currentLocation ?: LatLng(38.7369, -9.1366)
    val cameraPosState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { chargerViewModel.updateChargerCreationImage(it) } }
    )

    var showAddressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chargerCreationState.isSuccess) {
        if (chargerCreationState.isSuccess) {
            onSuccess()
            chargerViewModel.resetChargerCreation()
        }
    }

    LaunchedEffect(chargerCreationState.error) {
        chargerCreationState.error?.let {
            coroutine.launch { snackbarHostState.showSnackbar(it) }
        }
    }

    LaunchedEffect(mapState.searchedLocation) {
        mapState.searchedLocation?.let { location ->
            cameraPosState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
            selectedLocation = location
            chargerViewModel.updateChargerCreationLocation(location)
        }
    }

    // Initial setup to ensure we're tracking the payment systems
    LaunchedEffect(Unit) {
        chargerViewModel.updatePaymentSystems(selectedPaymentSystems.toList())
    }

    // Update the ViewModel whenever selected payment systems change
    LaunchedEffect(selectedPaymentSystems.size) {
        chargerViewModel.updatePaymentSystems(selectedPaymentSystems.toList())
        Log.d("AddChargerScreen", "Payment systems updated: ${selectedPaymentSystems.size} selected")
    }

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
            OutlinedTextField(
                value = chargerCreationState.name,
                onValueChange = { chargerViewModel.updateChargerCreationName(it) },
                label = { Text("Station Name") },
                placeholder = { Text("Enter station name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

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
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPosState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false),
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        chargerViewModel.updateChargerCreationLocation(latLng)
                    }
                ) {
                    selectedLocation?.let {
                        Marker(state = MarkerState(it), title = "Selected Location")
                    }
                }

                IconButton(
                    onClick = { showAddressDialog = true },
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

            Text("Charging Positions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Charging Positions Section
            ChargingPositionSelector(
                "Fast Charging",
                fastPositions,
                { fastPositions = it; chargerViewModel.updateFastPositions(it) },
                chargerCreationState.fastConnectorType,
                chargerCreationState.fastPrice,
                { chargerViewModel.updateFastConnectorType(it) },
                { chargerViewModel.updateFastPrice(it) }
            )

            Spacer(Modifier.height(8.dp))

            ChargingPositionSelector(
                "Medium Charging",
                mediumPositions,
                { mediumPositions = it; chargerViewModel.updateMediumPositions(it) },
                chargerCreationState.mediumConnectorType,
                chargerCreationState.mediumPrice,
                { chargerViewModel.updateMediumConnectorType(it) },
                { chargerViewModel.updateMediumPrice(it) }
            )

            Spacer(Modifier.height(8.dp))

            ChargingPositionSelector(
                "Slow Charging",
                slowPositions,
                { slowPositions = it; chargerViewModel.updateSlowPositions(it) },
                chargerCreationState.slowConnectorType,
                chargerCreationState.slowPrice,
                { chargerViewModel.updateSlowConnectorType(it) },
                { chargerViewModel.updateSlowPrice(it) }
            )

            Spacer(Modifier.height(24.dp))

            // Payment Systems Section
            Text("Payment Methods", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Text(
                "Select the payment methods accepted at this charging station:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                availablePaymentSystems.forEach { paymentSystem ->
                    val isSelected = chargerCreationState.paymentSystems.any { it.id == paymentSystem.id }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                chargerViewModel.togglePaymentSystem(paymentSystem, !isSelected)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                chargerViewModel.togglePaymentSystem(paymentSystem, checked)
                            }
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        Text(
                            text = paymentSystem.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { chargerViewModel.createCharger() },
                modifier = Modifier.fillMaxWidth(),
                enabled = (!chargerCreationState.isSubmitting &&
                        chargerCreationState.name.isNotBlank() &&
                        chargerCreationState.location != null &&
                        (fastPositions > 0 || mediumPositions > 0 || slowPositions > 0) &&
                        chargerCreationState.paymentSystems.isNotEmpty())
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

    if (showAddressDialog) {
        var text by remember { mutableStateOf("") }

        LaunchedEffect(text) {
            delay(300)
            if (text.isNotEmpty()) {
                mapViewModel.getAutocompleteSuggestions(text)
            }
        }

        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Search Address") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Enter address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                        text = prediction.getFullText(null).toString()
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
                        if (text.isNotBlank() && mapState.autocompleteSuggestions.isNotEmpty()) {
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
fun ChargingPositionSelector(
    title: String,
    count: Int,
    onCountChange: (Int) -> Unit,
    connectorType: ConnectorType,
    price: Double,
    onConnectorTypeChange: (ConnectorType) -> Unit,
    onPriceChange: (Double) -> Unit
) {
    var isCustomizeExpanded by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf(price.toString()) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))

            // Customize button
            if (count > 0) {
                IconButton(onClick = { isCustomizeExpanded = !isCustomizeExpanded }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Customize $title"
                    )
                }
            }
        }

        Slider(
            value = count.toFloat(),
            onValueChange = { onCountChange(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9
        )

        // Customization panel
        AnimatedVisibility(visible = isCustomizeExpanded && count > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Customize $title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Connector Type", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConnectorTypeChange(ConnectorType.CCS2) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectorType == ConnectorType.CCS2)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (connectorType == ConnectorType.CCS2)
                                    MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CCS2")
                        }

                        Button(
                            onClick = { onConnectorTypeChange(ConnectorType.TYPE2) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectorType == ConnectorType.TYPE2)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (connectorType == ConnectorType.TYPE2)
                                    MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("TYPE2")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Price input
                    Text("Cost per kWh ($)", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                priceInput = it
                                it.toDoubleOrNull()?.let { value ->
                                    onPriceChange(value)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}