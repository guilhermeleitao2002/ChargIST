package pt.ist.cmu.chargist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.ist.cmu.chargist.data.model.ChargingSpeed
import pt.ist.cmu.chargist.data.repository.ImageCodec        // ← new import
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
                        Icon(Icons.Default.Add, contentDescription = "Filters")
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

            /* ───── search bar (unchanged) ───── */
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

            /* chips … (all unchanged) */

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
                                        val total     = result.chargingSlots.size
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

    /* bottom‑sheet filters (unchanged) */
}

/* ---------- small helper ---------- */
@Composable
private fun CenterBox(content: @Composable BoxScope.() -> Unit) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
