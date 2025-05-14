package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Place
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.repository.ImageCodec
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerDetailScreen(
    chargerId: String,
    onBackClick: () -> Unit,
    onGoToMap: () -> Unit,
    onViewSlotDetails: (String) -> Unit,
    onAddChargingSlot: (String) -> Unit, // Add this parameter
    chargerViewModel: ChargerViewModel = koinViewModel(),
    mapViewModel: MapViewModel
) {
    LaunchedEffect(chargerId) { chargerViewModel.loadChargerDetails(chargerId) }

    val detailState by chargerViewModel.chargerDetailState.collectAsState()
    val chargerWithDetails = detailState.chargerWithDetails
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var nearbyServicesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(chargerWithDetails?.charger?.id) {
        chargerWithDetails?.charger?.let { charger ->
            if (!nearbyServicesLoaded) {
                val location = LatLng(charger.latitude, charger.longitude)
                chargerViewModel.loadNearbyPlaces(location)
                nearbyServicesLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chargerWithDetails?.charger?.name ?: "Charger Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (userId != null && chargerWithDetails != null) {
                        val isFavorite = chargerWithDetails.charger.favoriteUsers.contains(userId)
                        IconButton(onClick = { chargerViewModel.toggleFavorite(userId) }) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (chargerWithDetails != null) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        mapViewModel.focusOn(
                            LatLng(
                                chargerWithDetails.charger.latitude,
                                chargerWithDetails.charger.longitude
                            )
                        )
                        onGoToMap()            // <- just pop, no coroutine / delay needed
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Take me there",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPad ->
        when {
            detailState.isLoading -> CenterBox(innerPad) { Text("Loading…") }
            detailState.error != null -> CenterBox(innerPad) {
                Text(detailState.error!!, color = MaterialTheme.colorScheme.error)
            }
            chargerWithDetails != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPad)
                ) {
                    item {
                        val photoData = chargerWithDetails.charger.imageData
                        if (photoData != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(ImageCodec.base64ToBytes(photoData))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Charger photo",
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
                            ) { Text("No image available") }
                        }

                        Spacer(Modifier.height(16.dp))

                        val loc = LatLng(
                            chargerWithDetails.charger.latitude,
                            chargerWithDetails.charger.longitude
                        )
                        val cam = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(loc, 15f)
                        }
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            cameraPositionState = cam,
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
                        ) { Marker(MarkerState(loc), title = chargerWithDetails.charger.name) }
                    }

                    sectionHeader("Charging Slots", onAddAction = { onAddChargingSlot(chargerId) })
                    if (chargerWithDetails.chargingSlots.isEmpty()) {
                        item { EmptyLine("No charging slots available") }
                    } else {
                        // Group by charging speed
                        val fastSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.FAST }
                        val mediumSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.MEDIUM }
                        val slowSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.SLOW }

                        // Display fast charging slots
                        if (fastSlots.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Fast Charging",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(fastSlots) { slot ->
                                ChargingSlotItem(slot = slot) {
                                    onViewSlotDetails(slot.id)
                                }
                            }
                        }

                        // Display medium charging slots
                        if (mediumSlots.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Medium Charging",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(mediumSlots) { slot ->
                                ChargingSlotItem(slot = slot) {
                                    onViewSlotDetails(slot.id)
                                }
                            }
                        }

                        // Display slow charging slots
                        if (slowSlots.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Slow Charging",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(slowSlots) { slot ->
                                ChargingSlotItem(slot = slot) {
                                    onViewSlotDetails(slot.id)
                                }
                            }
                        }
                    }

                    sectionHeader("Payment Methods")
                    if (chargerWithDetails.paymentSystems.isEmpty()) {
                        item { EmptyLine("No payment information available") }
                    } else {
                        items(chargerWithDetails.paymentSystems) {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    sectionHeader("Nearby Services")
                    if (chargerWithDetails?.nearbyServices?.isEmpty() != false) {
                        item { EmptyLine("Looking for nearby services...") }
                    } else {
                        items(chargerWithDetails.nearbyServices) { service ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon based on service type
                                val icon = when {
                                    service.type.contains("RESTAURANT") || service.type.contains("CAFE") ->
                                        Icons.Default.Restaurant
                                    service.type.contains("GAS_STATION") -> Icons.Default.LocalGasStation
                                    service.type.contains("STORE") || service.type.contains("SHOP") ->
                                        Icons.Default.ShoppingCart
                                    else -> Icons.Default.Place
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(text = service.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${service.distance}m away",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterBox(pad: PaddingValues, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center, content = content)
}

private fun LazyListScope.sectionHeader(
    title: String,
    onAction: (() -> Unit)? = null,
    onAddAction: (() -> Unit)? = null // Add this parameter
) {
    item {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )

            Row {
                // Add button if onAddAction is provided
                onAddAction?.let {
                    IconButton(onClick = onAddAction) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add $title"
                        )
                    }
                }

                // Original action button (if provided)
                onAction?.let {
                    IconButton(onClick = onAction) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "View All $title"
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyLine(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSlotItem(slot: ChargingSlot, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (slot.speed) {
                            ChargingSpeed.FAST   -> Color(0xFF4CAF50)
                            ChargingSpeed.MEDIUM -> Color(0xFFFFC107)
                            ChargingSpeed.SLOW   -> Color(0xFFFF5722)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (slot.speed) {
                        ChargingSpeed.FAST   -> "F"
                        ChargingSpeed.MEDIUM -> "M"
                        ChargingSpeed.SLOW   -> "S"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(slot.connectorType.name, style = MaterialTheme.typography.titleMedium)

                val (status, colour) = when {
                    slot.isDamaged   -> "Damaged" to MaterialTheme.colorScheme.error
                    slot.isAvailable -> "Available" to MaterialTheme.colorScheme.primary
                    else             -> "Occupied"  to MaterialTheme.colorScheme.tertiary
                }

                Text(status, style = MaterialTheme.typography.bodyMedium, color = colour)
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "$${slot.price}/kWh",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}