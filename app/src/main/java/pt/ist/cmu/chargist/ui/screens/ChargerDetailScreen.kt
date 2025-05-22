package pt.ist.cmu.chargist.ui.screens

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.NearbyService
import pt.ist.cmu.chargist.data.repository.ImageCodec
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel
import pt.ist.cmu.chargist.ui.viewmodel.MapViewModel
import pt.ist.cmu.chargist.util.NetworkResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerDetailScreen(
    chargerId: String,
    onBackClick: () -> Unit,
    onGoToMap: () -> Unit,
    onViewSlotDetails: (String) -> Unit,
    onAddChargingSlot: (String) -> Unit,
    chargerViewModel: ChargerViewModel = koinViewModel(),
    mapViewModel: MapViewModel
) {
    LaunchedEffect(chargerId) { chargerViewModel.loadChargerDetails(chargerId) }

    val context = LocalContext.current
    val detailState by chargerViewModel.chargerDetailState.collectAsState()
    val chargerWithDetails = detailState.chargerWithDetails
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(chargerWithDetails?.charger) {
        chargerWithDetails?.charger?.let { charger ->
            val location = LatLng(charger.latitude, charger.longitude)
            chargerViewModel.loadNearbyPlaces(location)
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
                        onGoToMap()
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
                        HeaderSection(chargerWithDetails)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Charging Slots",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onAddChargingSlot(chargerId) }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Charging Slot",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val fastSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.FAST }
                    if (fastSlots.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "Fast Charging",
                                items = fastSlots
                            ) { slot ->
                                ChargingSlotItem(slot = slot) { onViewSlotDetails(slot.id) }
                            }
                        }
                    }

                    val mediumSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.MEDIUM }
                    if (mediumSlots.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "Medium Charging",
                                items = mediumSlots
                            ) { slot ->
                                ChargingSlotItem(slot = slot) { onViewSlotDetails(slot.id) }
                            }
                        }
                    }

                    val slowSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.SLOW }
                    if (slowSlots.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "Slow Charging",
                                items = slowSlots
                            ) { slot ->
                                ChargingSlotItem(slot = slot) { onViewSlotDetails(slot.id) }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        CollapsibleSection(
                            title = "Payment Methods",
                            items = chargerWithDetails.paymentSystems,
                            emptyMessage = "No payment information available"
                        ) { payment ->
                            Text(
                                payment.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        CollapsibleSection(
                            title = "Nearby Services",
                            items = chargerWithDetails.nearbyServices,
                            emptyMessage = if (chargerWithDetails.nearbyServices.isEmpty())
                                "Looking for nearby services..." else "No nearby services found"
                        ) { service ->
                            NearbyServiceItem(service)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(chargerWithDetails: pt.ist.cmu.chargist.data.model.ChargerWithDetails) {
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

    Spacer(Modifier.height(16.dp))
}

@Composable
private fun NearbyServiceItem(service: NearbyService) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

@Composable
private fun <T> CollapsibleSection(
    title: String,
    items: List<T>,
    onAddAction: (() -> Unit)? = null,
    emptyMessage: String? = null,
    itemContent: @Composable (T) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(if (isExpanded) 180f else 0f)

    Column(Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                onAddAction?.let {
                    IconButton(onClick = onAddAction) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add $title"
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (items.isEmpty() && emptyMessage != null) {
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    items.forEach { item ->
                        itemContent(item)
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

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(slot.connectorType.name, style = MaterialTheme.typography.titleMedium)

                val (status, colour) = when {
                    slot.damaged   -> "Damaged" to MaterialTheme.colorScheme.error
                    slot.available -> "Available" to MaterialTheme.colorScheme.primary
                    else             -> "Occupied"  to MaterialTheme.colorScheme.tertiary
                }

                Text(status, style = MaterialTheme.typography.bodyMedium, color = colour)
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