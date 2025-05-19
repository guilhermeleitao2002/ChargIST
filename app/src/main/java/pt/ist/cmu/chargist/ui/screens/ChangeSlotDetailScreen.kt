package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.ConnectorType
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeSlotDetailScreen(
    slotId: String,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ChargerViewModel = koinViewModel()
) {
    val detailState by viewModel.chargerDetailState.collectAsState()
    val chargerWithDetails = detailState.chargerWithDetails
    val slot = chargerWithDetails?.chargingSlots?.find { it.id == slotId }

    var debugMessage by remember { mutableStateOf<String?>(null) }

    var selectedSpeed by remember { mutableStateOf(ChargingSpeed.SLOW) }
    var selectedConnectorType by remember { mutableStateOf(ConnectorType.TYPE2) }

    LaunchedEffect(slotId) {
        viewModel.loadChargerBySlotId(slotId)
        debugMessage = "Loading slot details for: $slotId"
    }

    // Atualizar os estados quando o slot for carregado
    LaunchedEffect(slot) {
        if (slot != null) {
            selectedSpeed = slot.speed
            selectedConnectorType = slot.connectorType
            debugMessage = "Slot loaded: Speed=${slot.speed}, ConnectorType=${slot.connectorType}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Charging Slot Details") },
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
                var expandedSpeed by remember { mutableStateOf(false) }

                Box {
                    OutlinedTextField(
                        value = selectedSpeed.name,
                        onValueChange = {},
                        label = { Text("Charging Speed") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedSpeed = true }) {
                                Icon(
                                    if (expandedSpeed) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle dropdown"
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedSpeed,
                        onDismissRequest = { expandedSpeed = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ChargingSpeed.entries.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text(speed.name) },
                                onClick = {
                                    selectedSpeed = speed
                                    expandedSpeed = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var expandedConnector by remember { mutableStateOf(false) }

                Box {
                    OutlinedTextField(
                        value = selectedConnectorType.name,
                        onValueChange = {},
                        label = { Text("Connector Type") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedConnector = true }) {
                                Icon(
                                    if (expandedConnector) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle dropdown"
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedConnector,
                        onDismissRequest = { expandedConnector = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ConnectorType.entries.forEach { connector ->
                            DropdownMenuItem(
                                text = { Text(connector.name) },
                                onClick = {
                                    selectedConnectorType = connector
                                    expandedConnector = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.updateChargingSlot(
                            slotId = slot.id,
                            speed = selectedSpeed,
                            connectorType = selectedConnectorType,
                            price = slot.price,
                            isAvailable = slot.available,
                            isDamaged = slot.damaged
                        )
                        onSuccess()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}