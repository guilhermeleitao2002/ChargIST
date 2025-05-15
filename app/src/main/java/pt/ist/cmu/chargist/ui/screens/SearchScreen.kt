package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.PaymentSystem
import pt.ist.cmu.chargist.data.repository.ImageCodec
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import androidx.compose.material3.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onChargerClick: (String) -> Unit,
    viewModel: ChargerViewModel = koinViewModel(),
    userLocation: LatLng? = null
) {
    val searchState by viewModel.searchState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()

    val availablePaymentSystems = listOf(
        PaymentSystem(id = "visa", name = "VISA"),
        PaymentSystem(id = "mastercard", name = "Mastercard"),
        PaymentSystem(id = "mbway", name = "MBWay"),
        PaymentSystem(id = "googlepay", name = "Google Pay")
    )

    var selectedPaymentSystems by remember { mutableStateOf(searchState.paymentSystems ?: emptyList()) }

    LaunchedEffect(userLocation) {
        viewModel.updateUserLocation(userLocation)
    }

    // Debounce the search query
    LaunchedEffect(searchState.query) {
        delay(300) // Wait 300ms after the last change
        if (searchState.query != "") {
            viewModel.searchChargers()
        }
    }

    LaunchedEffect(Unit) { viewModel.searchChargers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Charging Stations") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            /* ───── search bar ───── */
            OutlinedTextField(
                value = searchState.query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search for charging stations") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::searchChargers) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            /* ───── results list ───── */
            when {
                searchState.isLoading -> CenterBox { Text("Searching…") }
                searchState.error != null -> CenterBox {
                    Text(searchState.error!!, color = MaterialTheme.colorScheme.error)
                }
                searchState.searchResults.isEmpty() -> CenterBox { Text("No charging stations found") }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(searchState.searchResults) { result ->
                            val charger = result.charger
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { onChargerClick(charger.id) },
                                elevation = CardDefaults.cardElevation(2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    /* ---------- station image with Base‑64 ---------- */
                                    val photoData = charger.imageData
                                    if (photoData != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(ImageCodec.base64ToBytes(photoData))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Charger image",
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    /* ---------- station details ---------- */
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                charger.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val userId = FirebaseAuth.getInstance().currentUser?.uid

                                            if (charger.favoriteUsers.contains(userId)) {
                                                Spacer(Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Favorite, "Favorite",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        val available = result.chargingSlots.count { it.isAvailable }
                                        val total = result.chargingSlots.size
                                        Text(
                                            if (total > 0) "$available/$total slots available"
                                            else "No slot information",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Filters",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )


                    Spacer(modifier = Modifier.width(100.dp))

                    var expandedSorter by remember { mutableStateOf(false) }
                    TextField(
                        value = when (searchState.sortBy.lowercase()) {
                            "distance" -> "Sort by Distance"
                            "travel_time" -> "Sort by Travel Time"
                            else -> "Sort by"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedSorter = true }) {
                                Icon(
                                    if (expandedSorter) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle dropdown"
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedSorter,
                        onDismissRequest = { expandedSorter = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(
                            text = { Text("Distance") },
                            onClick = {
                                viewModel.updateSearchSortBy("distance")
                                scope.launch { viewModel.searchChargers() }
                                expandedSorter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Travel Time") },
                            onClick = {
                                viewModel.updateSearchSortBy("travel_time")
                                scope.launch { viewModel.searchChargers() }
                                expandedSorter = false
                            }
                        )
                    }
                }

                // Availability Filter
                var showAvailableOnly by remember { mutableStateOf(searchState.isAvailable ?: false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Show Available Only", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showAvailableOnly,
                        onCheckedChange = { newValue ->
                            showAvailableOnly = newValue
                            viewModel.updateSearchAvailability(if (newValue) true else null)
                            scope.launch { viewModel.searchChargers() }
                        }
                    )
                }

                // Charging Speed Filter
                var selectedSpeed by remember { mutableStateOf(searchState.chargingSpeed) }
                var expanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Charging Speed", modifier = Modifier.padding(bottom = 8.dp))
                    Box {
                        OutlinedTextField(
                            value = selectedSpeed?.name ?: "All Speeds",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle dropdown"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Speeds") },
                                onClick = {
                                    selectedSpeed = null
                                    viewModel.updateSearchChargingSpeed(null)
                                    scope.launch { viewModel.searchChargers() }
                                    expanded = false
                                }
                            )
                            ChargingSpeed.entries.forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text(speed.name) },
                                    onClick = {
                                        selectedSpeed = speed
                                        viewModel.updateSearchChargingSpeed(speed)
                                        scope.launch { viewModel.searchChargers() }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Price Filter
                var maxPrice by remember { mutableStateOf(searchState.maxPrice ?: 1.0) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Maximum Price per kWh: $${String.format("%.2f", maxPrice)}")
                    Slider(
                        value = maxPrice.toFloat(),
                        onValueChange = { newValue ->
                            maxPrice = (newValue.toDouble() * 20).toInt() / 20.0 // Round to nearest 0.05
                            viewModel.updateSearchMaxPrice(maxPrice)
                            scope.launch { viewModel.searchChargers() }
                        },
                        valueRange = 0f..1f,
                        steps = 19 // Steps of 0.05 between 0 and 1 (20 intervals)
                    )
                }

                // Payment Systems Filter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Payment Methods", modifier = Modifier.padding(bottom = 8.dp))
                    availablePaymentSystems.forEach { paymentSystem ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelection = if (selectedPaymentSystems.contains(paymentSystem)) {
                                        selectedPaymentSystems - paymentSystem
                                    } else {
                                        selectedPaymentSystems + paymentSystem
                                    }
                                    selectedPaymentSystems = newSelection
                                    viewModel.updateSearchPaymentSystems(
                                        if (newSelection.isEmpty()) null else newSelection
                                    )
                                    scope.launch { viewModel.searchChargers() }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedPaymentSystems.contains(paymentSystem),
                                onCheckedChange = null
                            )
                            Text(paymentSystem.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                showFilters = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}

/* ---------- small helper ---------- */
@Composable
private fun CenterBox(content: @Composable BoxScope.() -> Unit) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)