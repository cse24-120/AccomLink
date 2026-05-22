package com.example.accomlink.student
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accomlink.listings.ListingsViewModel
import com.example.accomlink.maps.AccomMap
import com.example.accomlink.maps.RouteSuggestions
import com.example.accomlink.models.Listing
import com.example.accomlink.models.ListingFilter
import com.example.accomlink.ui.components.EmptyState
import com.example.accomlink.ui.components.ListingCard
import com.example.accomlink.ui.components.ListingPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentHomeScreen(
    viewModel: ListingsViewModel,
    onOpen: (String) -> Unit,
    onSearch: () -> Unit,
    onSaved: () -> Unit,
    onProfile: () -> Unit
) {
    val listings by viewModel.visibleListings.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Nearby student stays", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("Browse accommodation around BAC with quick distance estimates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        IconButton(onClick = onSaved) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Saved")
                        }
                        IconButton(onClick = onProfile) {
                            Icon(Icons.Outlined.Person, contentDescription = "Profile")
                        }
                    }
                }
                Button(onClick = onSearch, modifier = Modifier.padding(top = 12.dp)) {
                    Icon(Icons.Outlined.Search, null)
                    Text("Search and filter")
                }
            }
            if (listings.isEmpty()) {
                item { EmptyState("No listings yet", "When landlords publish accommodation it will appear here.") }
            }
            itemsIndexed(listings, key = { index, listing -> listing.id.ifBlank { "${listing.landlordId}-${listing.title}-${listing.createdAt}-$index" } }) { _, listing ->
                ListingCard(
                    listing = listing,
                    isFavorite = listing.id in favorites,
                    onOpen = { onOpen(listing.id) },
                    onFavorite = { viewModel.toggleFavorite(listing.id) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchScreen(viewModel: ListingsViewModel, onOpen: (String) -> Unit) {
    val filter by viewModel.filter.collectAsState()
    val savedPreferences by viewModel.savedPreferences.collectAsState()
    val state by viewModel.state.collectAsState()
    val listings by viewModel.visibleListings.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    var alerts by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var amenitiesExpanded by remember { mutableStateOf(false) }
    val minPrice = filter.minPrice.coerceIn(0.0, 10000.0)
    val maxPrice = filter.maxPrice.coerceIn(minPrice, 10000.0)
    LaunchedEffect(savedPreferences) {
        savedPreferences?.let {
            alerts = it.alertsEnabled
            viewModel.updateFilter(it.filter)
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(18.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        item {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { viewModel.updateFilter(filter.copy(query = it)) },
                label = { Text("Title, area or keyword") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = !locationExpanded }
            ) {
                OutlinedTextField(
                    value = filter.location.ifBlank { "All areas" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Location / area") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false }
                ) {
                    SearchAreaOptions.forEach { area ->
                        DropdownMenuItem(
                            text = { Text(area) },
                            onClick = {
                                viewModel.updateFilter(filter.copy(location = if (area == "All areas") "" else area))
                                locationExpanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            Text("Price: BWP ${minPrice.toInt()} - ${maxPrice.toInt()}")
            RangeSlider(
                value = minPrice.toFloat()..maxPrice.toFloat(),
                onValueChange = { viewModel.updateFilter(filter.copy(minPrice = it.start.toDouble(), maxPrice = it.endInclusive.toDouble())) },
                valueRange = 0f..10000f
            )
        }
        item {
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = filter.roomType.ifBlank { "All accommodation types" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Accommodation type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    AccommodationTypeOptions.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.updateFilter(filter.copy(roomType = if (type == "All accommodation types") "" else type))
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            ExposedDropdownMenuBox(
                expanded = amenitiesExpanded,
                onExpandedChange = { amenitiesExpanded = !amenitiesExpanded }
            ) {
                OutlinedTextField(
                    value = filter.amenities.joinToString(", ").ifBlank { "Any amenities" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Amenities") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = amenitiesExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = amenitiesExpanded,
                    onDismissRequest = { amenitiesExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Any amenities") },
                        onClick = {
                            viewModel.updateFilter(filter.copy(amenities = emptyList()))
                            amenitiesExpanded = false
                        }
                    )
                    AmenityOptions.forEach { amenity ->
                        val selected = amenity in filter.amenities
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = selected, onCheckedChange = null)
                                    Text(amenity)
                                }
                            },
                            onClick = {
                                val updated = if (selected) filter.amenities - amenity else filter.amenities + amenity
                                viewModel.updateFilter(filter.copy(amenities = updated))
                            }
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = filter.availabilityDate?.let { formatDate(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Available by") },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { showDatePicker = true }) { Text("Pick date") }
                TextButton(onClick = { viewModel.updateFilter(ListingFilter()) }) { Text("Clear filters") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Notifications on")
                Switch(checked = alerts, onCheckedChange = {
                    alerts = it
                    viewModel.savePreferences(filter, it)
                })
            }
        }
        item { Button(onClick = { viewModel.savePreferences(filter, alerts) }, modifier = Modifier.fillMaxWidth()) { Text("Save my preferences") } }
        if (state.error != null || state.message != null) {
            item {
                Text(
                    state.error ?: state.message.orEmpty(),
                    color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
        item { Text("${listings.size} listings found", fontWeight = FontWeight.Bold) }
        itemsIndexed(listings, key = { index, listing -> listing.id.ifBlank { "${listing.landlordId}-${listing.title}-${listing.createdAt}-$index" } }) { _, listing ->
            ListingCard(
                listing = listing,
                isFavorite = listing.id in favorites,
                onOpen = { onOpen(listing.id) },
                onFavorite = { viewModel.toggleFavorite(listing.id) }
            )
        }
    }
    if (showDatePicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFilter(filter.copy(availabilityDate = pickerState.selectedDateMillis))
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(pickerState) }
    }
}

@Composable
fun FavoritesScreen(viewModel: ListingsViewModel, onOpen: (String) -> Unit) {
    val listings by viewModel.favoriteListings.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Favorites", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        if (listings.isEmpty()) item { EmptyState("Nothing saved yet", "Tap the heart on a listing to keep it here.") }
        itemsIndexed(listings, key = { index, listing -> listing.id.ifBlank { "${listing.landlordId}-${listing.title}-${listing.createdAt}-$index" } }) { _, listing ->
            ListingCard(listing, true, { onOpen(listing.id) }, { viewModel.toggleFavorite(listing.id) })
        }
    }
}

@Composable
fun ListingDetailsScreen(listing: Listing, isFavorite: Boolean, onFavorite: () -> Unit, onContact: () -> Unit, onReserve: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(listing.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(listing.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        listing.images.firstOrNull()?.let { imageUrl ->
            item {
                ListingPhoto(
                    model = imageUrl,
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
        }
        item {
            AccomMap(listing, Modifier.fillMaxWidth().height(260.dp))
        }
        item {
            Text("BWP ${listing.price.toInt()}/month", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text(listing.description)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Deposit required: BWP ${listing.depositAmount.toInt()}", fontWeight = FontWeight.Bold)
                    Text("Available from: ${formatDate(listing.availabilityDate)}")
                }
            }
        }
        if (listing.isOccupied) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text("Status: Reserved", modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            AssistChip(onClick = {}, label = { Text("Distance to BAC: ${RouteSuggestions.distanceToBac(listing)}") })
            Text("Closest BAC Route:", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
            RouteSuggestions.suggestionsFor(listing).forEach { Text("- $it") }
        }
        item {
            Button(onClick = onReserve, enabled = !listing.isOccupied, modifier = Modifier.fillMaxWidth()) { Text("Reserve & Pay Deposit") }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onFavorite, enabled = !listing.isOccupied, modifier = Modifier.weight(1f)) { Text(if (isFavorite) "Saved" else "Save") }
                Button(onClick = onContact, enabled = !listing.isOccupied, modifier = Modifier.weight(1f)) { Text("Chat") }
            }
        }
    }
}

private fun formatDate(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))

private val SearchAreaOptions = listOf(
    "All areas",
    "Block 3",
    "Block 5",
    "Block 6",
    "Block 7",
    "Block 8",
    "Phase 2",
    "Phase 4",
    "Mogoditshane",
    "Tlokweng",
    "Gaborone West",
    "Broadhurst",
    "Extension 2",
    "Phakalane",
    "Fairgrounds",
    "Village"
)

private val AccommodationTypeOptions = listOf(
    "All accommodation types",
    "Single room",
    "Double room",
    "Studio",
    "Bedsitter",
    "1-bed flat",
    "2-bed flat",
    "Shared house",
    "Bachelor pad",
    "En-suite",
    "Self-contained unit"
)

private val AmenityOptions = listOf("Wi-Fi", "Water", "Electricity", "Security")
