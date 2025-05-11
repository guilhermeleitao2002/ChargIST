package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSlot
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSlotsScreen(
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
                title = { Text("Charging Slots") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        when {
            detailState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            detailState.error != null -> {
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
            }
            chargerWithDetails != null -> {
                if (chargerWithDetails.chargingSlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No charging slots available")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Station name and summary
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = chargerWithDetails.charger.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Summary statistics
                                    val availableCount = chargerWithDetails.chargingSlots.count { it.isAvailable }
                                    val totalCount = chargerWithDetails.chargingSlots.size
                                    val fastCount = chargerWithDetails.chargingSlots.count { it.speed == ChargingSpeed.FAST }
                                    val mediumCount = chargerWithDetails.chargingSlots.count { it.speed == ChargingSpeed.MEDIUM }
                                    val slowCount = chargerWithDetails.chargingSlots.count { it.speed == ChargingSpeed.SLOW }

                                    Text(
                                        text = "$availableCount of $totalCount slots available",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Fast: $fastCount, Medium: $mediumCount, Slow: $slowCount",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Group by charging speed
                        val fastSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.FAST }
                        val mediumSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.MEDIUM }
                        val slowSlots = chargerWithDetails.chargingSlots.filter { it.speed == ChargingSpeed.SLOW }

                        if (fastSlots.isNotEmpty()) {
                            item {
                                SectionTitle("Fast Charging", Color(0xFF4CAF50))
                            }
                            items(fastSlots) { slot ->
                                ChargingSlotCard(slot, onSlotClick)
                            }
                        }

                        if (mediumSlots.isNotEmpty()) {
                            item {
                                SectionTitle("Medium Charging", Color(0xFFFFC107))
                            }
                            items(mediumSlots) { slot ->
                                ChargingSlotCard(slot, onSlotClick)
                            }
                        }

                        if (slowSlots.isNotEmpty()) {
                            item {
                                SectionTitle("Slow Charging", Color(0xFFFF5722))
                            }
                            items(slowSlots) { slot ->
                                ChargingSlotCard(slot, onSlotClick)
                            }
                        }

                        // Extra space at the bottom
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChargingSlotCard(slot: ChargingSlot, onSlotClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                slot.isDamaged -> MaterialTheme.colorScheme.errorContainer
                !slot.isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                slot.isDamaged -> MaterialTheme.colorScheme.error
                !slot.isAvailable -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        onClick = { onSlotClick(slot.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot status indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            slot.isDamaged -> MaterialTheme.colorScheme.error
                            !slot.isAvailable -> MaterialTheme.colorScheme.outline
                            else -> when (slot.speed) {
                                ChargingSpeed.FAST -> Color(0xFF4CAF50)
                                ChargingSpeed.MEDIUM -> Color(0xFFFFC107)
                                ChargingSpeed.SLOW -> Color(0xFFFF5722)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (slot.isDamaged) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )
                } else {
                    Text(
                        text = when (slot.speed) {
                            ChargingSpeed.FAST -> "F"
                            ChargingSpeed.MEDIUM -> "M"
                            ChargingSpeed.SLOW -> "S"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Connector: ${slot.connectorType.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (slot.isDamaged) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Damaged",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        slot.isDamaged -> "Damaged"
                        slot.isAvailable -> "Available"
                        else -> "In Use"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        slot.isDamaged -> MaterialTheme.colorScheme.error
                        slot.isAvailable -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${slot.price}/kWh",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}