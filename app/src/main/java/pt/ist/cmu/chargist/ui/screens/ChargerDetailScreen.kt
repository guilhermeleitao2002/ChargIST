package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.NearbyService
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerDetailScreen(
    chargerId: String,
    onBackClick: () -> Unit,
    onSlotClick: (String) -> Unit,
    viewModel: ChargerViewModel = koinViewModel()
) {
    LaunchedEffect(chargerId) {
        viewModel.loadChargerDetails(chargerId)
    }

    val detailState by viewModel.chargerDetailState.collectAsState()
    val chargerWithDetails = detailState.chargerWithDetails

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chargerWithDetails?.charger?.name ?: "Charger Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (chargerWithDetails?.charger?.isFavorite == true) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (chargerWithDetails?.charger?.isFavorite == true) {
                                "Remove from Favorites"
                            } else {
                                "Add to Favorites"
                            }
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
                onClick = { /* Open navigation in maps app */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Navigate",
                    tint = Color.White
                )
            }
        }
    ) { innerPadding ->
        if (detailState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else if (detailState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = detailState.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (chargerWithDetails != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Image and Map
                item {
                    if (chargerWithDetails.charger.imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(chargerWithDetails.charger.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Charger image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No image available")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Small map
                    val location = LatLng(
                        chargerWithDetails.charger.latitude,
                        chargerWithDetails.charger.longitude
                    )
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(location, 15f)
                    }

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            compassEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = location),
                            title = chargerWithDetails.charger.name
                        )
                    }
                }

                // Charging Slots Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Charging Slots",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // List of Charging Slots
                if (chargerWithDetails.chargingSlots.isEmpty()) {
                    item {
                        Text(
                            text = "No charging slots available",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(chargerWithDetails.chargingSlots) { slot ->
                        ChargingSlotItem(
                            slot = slot,
                            onClick = { onSlotClick(slot.id) }
                        )
                    }
                }

                // Nearby Services Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Nearby Services",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // List of Nearby Services
                if (chargerWithDetails.nearbyServices.isEmpty()) {
                    item {
                        Text(
                            text = "No nearby services available",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(chargerWithDetails.nearbyServices) { service ->
                        NearbyServiceItem(service = service)
                    }
                }

                // Payment Systems Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Payment Methods",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // List of Payment Systems
                if (chargerWithDetails.paymentSystems.isEmpty()) {
                    item {
                        Text(
                            text = "No payment information available",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(chargerWithDetails.paymentSystems) { payment ->
                        Text(
                            text = payment.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Add some space at the bottom
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSlotItem(
    slot: ChargingSlot,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (slot.speed) {
                            ChargingSpeed.FAST -> Color(0xFF4CAF50) // Green
                            ChargingSpeed.MEDIUM -> Color(0xFFFFC107) // Amber
                            ChargingSpeed.SLOW -> Color(0xFFFF5722) // Deep Orange
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (slot.speed) {
                        ChargingSpeed.FAST -> "F"
                        ChargingSpeed.MEDIUM -> "M"
                        ChargingSpeed.SLOW -> "S"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = slot.connectorType.name,
                    style = MaterialTheme.typography.titleMedium
                )

                val statusText = when {
                    slot.isDamaged -> "Damaged"
                    slot.isAvailable -> "Available"
                    else -> "Occupied"
                }
                val statusColor = when {
                    slot.isDamaged -> MaterialTheme.colorScheme.error
                    slot.isAvailable -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "$${slot.price}/kWh",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NearbyServiceItem(service: NearbyService) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (service.type) {
                    "FOOD" -> "🍔"
                    "TOILET" -> "🚻"
                    "AIR_WATER" -> "💧"
                    else -> "🏪"
                },
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "${service.distance}m away",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}