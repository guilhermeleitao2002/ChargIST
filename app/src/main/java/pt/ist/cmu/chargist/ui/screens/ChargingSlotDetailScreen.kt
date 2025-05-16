package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSlotDetailScreen(
    slotId: String,
    onBackClick: () -> Unit,
    viewModel: ChargerViewModel = koinViewModel()
) {
    var debugMessage by remember { mutableStateOf<String?>(null) }

    // Carregar o Charger correspondente ao slotId usando o ViewModel
    LaunchedEffect(slotId) {
        viewModel.loadChargerBySlotId(slotId)
        debugMessage = "Attempting to find charger containing slot: $slotId"
    }

    val detailState by viewModel.chargerDetailState.collectAsState()
    val chargerWithDetails = detailState.chargerWithDetails
    val slot = chargerWithDetails?.chargingSlots?.find { it.id == slotId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charging Slot Details") },
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = detailState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )

                    // Show debug info if available
                    debugMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (slot != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Charging speed indicator
                Box(
                    modifier = Modifier
                        .size(100.dp)
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
                        fontSize = 40.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (slot.speed) {
                        ChargingSpeed.FAST -> "Fast Charging"
                        ChargingSpeed.MEDIUM -> "Medium Charging"
                        ChargingSpeed.SLOW -> "Slow Charging"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = slot.connectorType.name,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (slot.isDamaged) {
                            MaterialTheme.colorScheme.errorContainer
                        } else if (slot.isAvailable) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val statusText = when {
                            slot.isDamaged -> "Damaged"
                            slot.isAvailable -> "Available"
                            else -> "Occupied"
                        }
                        val statusIcon = when {
                            slot.isDamaged -> Icons.Default.Warning
                            else -> null
                        }
                        val statusColor = when {
                            slot.isDamaged -> MaterialTheme.colorScheme.error
                            slot.isAvailable -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (statusIcon != null) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.headlineSmall,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Price Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cost per kWh",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = "$${slot.price}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Change slot details button
                    OutlinedButton(onClick = { /* Change slot details */ }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Details")
                    }

                    // Report damage button
                    Button(
                        onClick = {
                            viewModel.reportSlotDamage(slotId, !slot.isDamaged, slot.speed)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (slot.isDamaged) "Mark as Fixed" else "Report Damage")
                    }
                }

                // Last update information
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Last updated: ${timeSince(slot.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // If slot is null but we have charger details, show error with more details
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Charging slot not found",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // More detailed debug info
                    Text(
                        text = "Slot ID: $slotId",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (chargerWithDetails != null) {
                        Text(
                            text = "Charger found: ${chargerWithDetails.charger.name}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Available slots: ${chargerWithDetails.chargingSlots.size}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (chargerWithDetails.chargingSlots.isNotEmpty()) {
                            Text(
                                text = "Available slot IDs: ${chargerWithDetails.chargingSlots.map { it.id }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Show the debug message if available
                    debugMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format time since the last update
private fun timeSince(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        else -> "${diff / (24 * 60 * 60 * 1000)} days ago"
    }
}