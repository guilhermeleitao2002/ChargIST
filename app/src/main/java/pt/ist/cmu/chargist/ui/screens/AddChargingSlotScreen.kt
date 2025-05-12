package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.model.ConnectorType
import pt.ist.cmu.chargist.ui.viewmodel.ChargerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChargingSlotScreen(
    chargerId: String,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ChargerViewModel = koinViewModel()
) {
    var selectedSpeed by remember { mutableStateOf(ChargingSpeed.MEDIUM) }
    var selectedConnector by remember { mutableStateOf(ConnectorType.CCS2) }
    var price by remember { mutableStateOf("0.35") }
    var isSubmitting by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Charging Slot") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Charging Speed Selection
            Text(
                text = "Charging Speed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChargingSpeedButton(
                    text = "Fast",
                    isSelected = selectedSpeed == ChargingSpeed.FAST,
                    onClick = { selectedSpeed = ChargingSpeed.FAST }
                )

                ChargingSpeedButton(
                    text = "Medium",
                    isSelected = selectedSpeed == ChargingSpeed.MEDIUM,
                    onClick = { selectedSpeed = ChargingSpeed.MEDIUM }
                )

                ChargingSpeedButton(
                    text = "Slow",
                    isSelected = selectedSpeed == ChargingSpeed.SLOW,
                    onClick = { selectedSpeed = ChargingSpeed.SLOW }
                )
            }

            // Connector Type Selection
            Text(
                text = "Connector Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConnectorTypeButton(
                    text = "CCS2",
                    isSelected = selectedConnector == ConnectorType.CCS2,
                    onClick = { selectedConnector = ConnectorType.CCS2 }
                )

                ConnectorTypeButton(
                    text = "Type 2",
                    isSelected = selectedConnector == ConnectorType.TYPE2,
                    onClick = { selectedConnector = ConnectorType.TYPE2 }
                )
            }

            // Price Input
            Text(
                text = "Price per kWh ($)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = price,
                onValueChange = {
                    // Allow only valid decimal numbers
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        price = it
                    }
                },
                label = { Text("Price") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            // Default price suggestions based on speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestedPriceButton(text = "€0.25", onClick = { price = "0.25" })
                SuggestedPriceButton(text = "€0.35", onClick = { price = "0.35" })
                SuggestedPriceButton(text = "€0.50", onClick = { price = "0.50" })
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    viewModel.addChargingSlot(
                        chargerId = chargerId,
                        speed = selectedSpeed,
                        connector = selectedConnector,
                        price = priceValue
                    )
                    isSubmitting = false
                    onSuccess()
                },
                enabled = !isSubmitting && price.isNotEmpty() && price.toDoubleOrNull() != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Charging Slot")
                }
            }
        }
    }
}

@Composable
private fun ChargingSpeedButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun ConnectorTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun SuggestedPriceButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}