package pt.ist.cmu.chargist.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onChargerClick: (String) -> Unit,
    viewModel: ChargerViewModel = koinViewModel()
) {
    val searchState by viewModel.searchState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        // Initial search if needed
        viewModel.searchChargers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Charging Stations") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Filters"
                        )
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
            // Search bar
            OutlinedTextField(
                value = searchState.query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search for charging stations") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchChargers() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Fast charging filter
                FilterChip(
                    selected = searchState.chargingSpeed == ChargingSpeed.FAST,
                    onClick = {
                        viewModel.updateSearchChargingSpeed(
                            if (searchState.chargingSpeed == ChargingSpeed.FAST) null else ChargingSpeed.FAST
                        )
                        viewModel.searchChargers()
                    },
                    label = { Text("Fast") },
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Medium charging filter
                FilterChip(
                    selected = searchState.chargingSpeed == ChargingSpeed.MEDIUM,
                    onClick = {
                        viewModel.updateSearchChargingSpeed(
                            if (searchState.chargingSpeed == ChargingSpeed.MEDIUM) null else ChargingSpeed.MEDIUM
                        )
                        viewModel.searchChargers()
                    },
                    label = { Text("Medium") },
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Slow charging filter
                FilterChip(
                    selected = searchState.chargingSpeed == ChargingSpeed.SLOW,
                    onClick = {
                        viewModel.updateSearchChargingSpeed(
                            if (searchState.chargingSpeed == ChargingSpeed.SLOW) null else ChargingSpeed.SLOW
                        )
                        viewModel.searchChargers()
                    },
                    label = { Text("Slow") },
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Available filter
                FilterChip(
                    selected = searchState.isAvailable == true,
                    onClick = {
                        viewModel.updateSearchAvailability(
                            if (searchState.isAvailable == true) null else true
                        )
                        viewModel.searchChargers()
                    },
                    label = { Text("Available") }
                )
            }

            // Results list
            if (searchState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Searching...")
                }
            } else if (searchState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = searchState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (searchState.searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No charging stations found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchState.searchResults) { chargerWithDetails ->
                        val charger = chargerWithDetails.charger

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onChargerClick(charger.id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Station image
                                if (charger.imageUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(charger.imageUrl)
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
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Station details
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Station name and favorite indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = charger.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (charger.isFavorite) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Favorite",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Available slots info
                                    val availableSlots = chargerWithDetails.chargingSlots.count { it.isAvailable }
                                    val totalSlots = chargerWithDetails.chargingSlots.size

                                    if (totalSlots > 0) {
                                        Text(
                                            text = "$availableSlots/$totalSlots slots available",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        Text(
                                            text = "No slot information",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    // Payment systems
                                    if (chargerWithDetails.paymentSystems.isNotEmpty()) {
                                        Text(
                                            text = "Payment: " + chargerWithDetails.paymentSystems.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.bodySmall
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

    // Filters bottom sheet
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
                Text(
                    text = "Filter By",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Charging Speed
                Text(
                    text = "Charging Speed",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchChargingSpeed(ChargingSpeed.FAST)
                        }
                ) {
                    RadioButton(
                        selected = searchState.chargingSpeed == ChargingSpeed.FAST,
                        onClick = {
                            viewModel.updateSearchChargingSpeed(ChargingSpeed.FAST)
                        }
                    )
                    Text("Fast Charging")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchChargingSpeed(ChargingSpeed.MEDIUM)
                        }
                ) {
                    RadioButton(
                        selected = searchState.chargingSpeed == ChargingSpeed.MEDIUM,
                        onClick = {
                            viewModel.updateSearchChargingSpeed(ChargingSpeed.MEDIUM)
                        }
                    )
                    Text("Medium Charging")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchChargingSpeed(ChargingSpeed.SLOW)
                        }
                ) {
                    RadioButton(
                        selected = searchState.chargingSpeed == ChargingSpeed.SLOW,
                        onClick = {
                            viewModel.updateSearchChargingSpeed(ChargingSpeed.SLOW)
                        }
                    )
                    Text("Slow Charging")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchChargingSpeed(null)
                        }
                ) {
                    RadioButton(
                        selected = searchState.chargingSpeed == null,
                        onClick = {
                            viewModel.updateSearchChargingSpeed(null)
                        }
                    )
                    Text("Any Speed")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Availability
                Text(
                    text = "Availability",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchAvailability(!searchState.isAvailable!!)
                        }
                ) {
                    Checkbox(
                        checked = searchState.isAvailable == true,
                        onCheckedChange = {
                            viewModel.updateSearchAvailability(it)
                        }
                    )
                    Text("Show only available chargers")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sort By
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchSortBy("distance")
                        }
                ) {
                    RadioButton(
                        selected = searchState.sortBy == "distance",
                        onClick = {
                            viewModel.updateSearchSortBy("distance")
                        }
                    )
                    Text("Distance")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchSortBy("price")
                        }
                ) {
                    RadioButton(
                        selected = searchState.sortBy == "price",
                        onClick = {
                            viewModel.updateSearchSortBy("price")
                        }
                    )
                    Text("Price (Low to High)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSearchSortBy("availability")
                        }
                ) {
                    RadioButton(
                        selected = searchState.sortBy == "availability",
                        onClick = {
                            viewModel.updateSearchSortBy("availability")
                        }
                    )
                    Text("Availability")
                }

                // Apply button
                Spacer(modifier = Modifier.height(24.dp))
                IconButton(
                    onClick = {
                        scope.launch {
                            viewModel.searchChargers()
                            bottomSheetState.hide()
                            showFilters = false
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Apply Filters")
                }

                // Add some space at the bottom
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}