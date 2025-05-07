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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.repository.ImageCodec   // ← helper to decode Base‑64
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerDetailScreen(
    chargerId: String,
    onBackClick: () -> Unit,
    onSlotClick: (String) -> Unit,
    viewModel: ChargerViewModel = koinViewModel()
) {
    LaunchedEffect(chargerId) { viewModel.loadChargerDetails(chargerId) }

    val detailState by viewModel.chargerDetailState.collectAsState()
    val chargerWithDetails = detailState.chargerWithDetails
    val userId = FirebaseAuth.getInstance().currentUser?.uid


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
                        val isFavorite = chargerWithDetails.charger.favoriteUsers.contains(userId) ?: false
                        IconButton(onClick = { viewModel.toggleFavorite(userId) }) {
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
            FloatingActionButton(
                onClick = { /* open external navigation here */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White) }
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
                    /* ───── image + mini‑map ───── */
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

                    /* ───── charging slots ───── */
                    sectionHeader("Charging Slots")
                    if (chargerWithDetails.chargingSlots.isEmpty()) {
                        item { EmptyLine("No charging slots available") }
                    } else {
                        items(chargerWithDetails.chargingSlots) { slot ->
                            ChargingSlotItem(slot) { onSlotClick(slot.id) }
                        }
                    }

                    /* ───── payment systems ───── */
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

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

/* ---------- helpers & smaller composables ---------- */

@Composable
private fun CenterBox(pad: PaddingValues, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center, content = content)
}

private fun LazyListScope.sectionHeader(title: String) {
    item {
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyLine(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp))

/* ---------- slot card (unchanged) ---------- */

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
            /* speed badge */
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
