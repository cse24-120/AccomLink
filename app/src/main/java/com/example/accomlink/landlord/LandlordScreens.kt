package com.example.accomlink.landlord

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.accomlink.listings.ListingsViewModel
import com.example.accomlink.maps.LocationPickerMap
import com.example.accomlink.models.Listing
import com.example.accomlink.models.Reservation
import com.example.accomlink.ui.components.EmptyState
import com.example.accomlink.ui.components.ListingPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LandlordDashboardScreen(viewModel: ListingsViewModel, onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val listings by viewModel.landlordListings.collectAsState()
    val reservations by viewModel.landlordReservations.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().padding(18.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Your Listings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("${listings.size} listing(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAdd) { Text("Add") }
            }
        }
        if (listings.isEmpty()) item { EmptyState("No spaces published", "Create your first accommodation listing for students to discover.") }
        items(listings, key = { listing -> listing.id.ifBlank { "${listing.landlordId}-${listing.title}-${listing.createdAt}" } }) { listing ->
            LandlordListingCard(
                listing = listing,
                reservation = reservations.firstOrNull { it.listingId == listing.id },
                onEdit = { onEdit(listing.id) },
                onToggleAvailability = { viewModel.setOccupied(listing.id, !listing.isOccupied) },
                onDelete = { viewModel.deleteListing(listing.id) }
            )
        }
    }
}

@Composable
private fun LandlordListingCard(
    listing: Listing,
    reservation: Reservation?,
    onEdit: () -> Unit,
    onToggleAvailability: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(listing.id) { mutableStateOf(false) }

    Card(
        onClick = { detailsExpanded = !detailsExpanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListingPhoto(
                model = listing.images.firstOrNull(),
                contentDescription = listing.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(118.dp)
                    .height(118.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    listing.title.ifBlank { "Room" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listing.location,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listing.roomType,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "BWP ${listing.price.toInt()}/month",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    listing.statusLabel,
                    color = if (listing.isOccupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Listing actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Listing") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (listing.isOccupied) "Mark as Available" else "Mark as Reserved") },
                        leadingIcon = {
                            Icon(
                                if (listing.isOccupied) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleAvailability()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Listing") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
        if (detailsExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Reservation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                if (reservation == null) {
                    Text("No student has reserved this listing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Reserved by: ${reservation.studentName.ifBlank { reservation.studentId }}", fontWeight = FontWeight.SemiBold)
                    if (reservation.studentEmail.isNotBlank()) Text("Email: ${reservation.studentEmail}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (reservation.studentPhone.isNotBlank()) Text("Phone: ${reservation.studentPhone}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Reference: ${reservation.referenceNumber}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Deposit paid: P${"%,.2f".format(reservation.depositPaid)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ListingEditorScreen(
    viewModel: ListingsViewModel,
    existing: Listing?,
    onDone: () -> Unit
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var price by rememberSaveable(existing?.id) { mutableStateOf(existing?.price?.toInt()?.toString().orEmpty()) }
    var deposit by rememberSaveable(existing?.id) { mutableStateOf(existing?.depositAmount?.toInt()?.toString().orEmpty()) }
    var availabilityDate by rememberSaveable(existing?.id) { mutableStateOf(existing?.availabilityDate ?: System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var location by rememberSaveable(existing?.id) { mutableStateOf(existing?.location.orEmpty()) }
    var roomType by rememberSaveable(existing?.id) { mutableStateOf(existing?.roomType ?: "Single room") }
    var occupied by rememberSaveable(existing?.id) { mutableStateOf(existing?.isOccupied ?: false) }
    var latitude by rememberSaveable(existing?.id) { mutableStateOf(existing?.latitude ?: Listing.BAC_LATITUDE) }
    var longitude by rememberSaveable(existing?.id) { mutableStateOf(existing?.longitude ?: Listing.BAC_LONGITUDE) }
    var locationStatus by rememberSaveable(existing?.id) { mutableStateOf("") }
    var validationError by rememberSaveable(existing?.id) { mutableStateOf("") }
    var locationExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val selectedAmenities = remember(existing?.id) {
        mutableStateListOf<String>().apply { addAll(existing?.amenities.orEmpty().filter { it in AmenityOptions }) }
    }
    val pickedImages = remember { mutableStateListOf<Uri>() }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        pickedImages.clear()
        pickedImages.addAll(uris)
    }

    LaunchedEffect(location) {
        val query = location.trim()
        if (query.length < 3) return@LaunchedEffect
        delay(700)
        val point = geocodeLocation(context, query)
        if (point != null) {
            latitude = point.first
            longitude = point.second
            locationStatus = "Map moved to $query. Tap the exact house to place the pin."
        } else {
            locationStatus = "Location not found. Try adding Gaborone or Botswana."
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (existing == null) "Add listing" else "Edit listing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        item { OutlinedTextField(title, { title = it; validationError = "" }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(description, { description = it; validationError = "" }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
        item { OutlinedTextField(price, { price = it.filter(Char::isDigit); validationError = "" }, label = { Text("Price per month") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(deposit, { deposit = it.filter(Char::isDigit); validationError = "" }, label = { Text("Deposit amount") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(
                value = formatListingDate(availabilityDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Available date") },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { showDatePicker = true }) {
                Text("Pick available date")
            }
        }
        item {
            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = !locationExpanded }
            ) {
                OutlinedTextField(
                    value = location.ifBlank { "Select area" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                    supportingText = { Text(locationStatus.ifBlank { "Select an area, then pick the exact house on the map." }) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false }
                ) {
                    ListingAreaOptions.forEach { area ->
                        DropdownMenuItem(
                            text = { Text(area) },
                            onClick = {
                                location = area
                                validationError = ""
                                locationExpanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = roomType,
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
                                roomType = type
                                validationError = ""
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amenities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AmenityOptions.chunked(2).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { amenity ->
                        val selected = amenity in selectedAmenities
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedAmenities.remove(amenity) else selectedAmenities.add(amenity)
                            },
                            label = { Text(amenity) }
                        )
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Occupied")
                Switch(checked = occupied, onCheckedChange = { occupied = it })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Listing pin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Tap the exact house location after the map moves to the typed area.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    LocationPickerMap(
                        latitude = latitude,
                        longitude = longitude,
                        modifier = Modifier.fillMaxSize()
                    ) { pickedLatitude, pickedLongitude ->
                        latitude = pickedLatitude
                        longitude = pickedLongitude
                    }
                }
                Text(
                    "Selected: ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    validationError = ""
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (pickedImages.isEmpty()) "Pick listing images" else "${pickedImages.size} image(s) selected")
            }
        }
        if (pickedImages.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(pickedImages) { index, uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected listing photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(104.dp)
                        )
                    }
                }
            }
        }
        existing?.images?.firstOrNull()?.let { imageUrl ->
            item {
                ListingPhoto(
                    model = imageUrl,
                    contentDescription = "Current listing photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
        item {
            if (validationError.isNotBlank()) {
                Text(validationError, color = MaterialTheme.colorScheme.error)
            }
            if (state.error != null || state.message != null) Text(state.error ?: state.message.orEmpty())
            Button(
                onClick = {
                    val priceValue = price.toDoubleOrNull()
                    val depositValue = deposit.toDoubleOrNull()
                    val hasListingImage = pickedImages.isNotEmpty() || existing?.images?.isNotEmpty() == true
                    validationError = when {
                        title.isBlank() -> "Title is required."
                        description.isBlank() -> "Description is required."
                        priceValue == null || priceValue <= 0.0 -> "Enter a valid monthly price."
                        depositValue == null || depositValue <= 0.0 -> "Enter a valid deposit amount."
                        location.isBlank() -> "Location is required."
                        roomType.isBlank() -> "Accommodation type is required."
                        !hasListingImage -> "Add at least one listing image."
                        else -> ""
                    }
                    if (validationError.isNotBlank()) return@Button
                    val validPrice = priceValue ?: return@Button
                    val validDeposit = depositValue ?: return@Button
                    val listing = Listing(
                        id = existing?.id.orEmpty(),
                        landlordId = existing?.landlordId.orEmpty(),
                        title = title,
                        description = description,
                        price = validPrice,
                        location = location,
                        roomType = roomType,
                        furnished = false,
                        latitude = latitude,
                        longitude = longitude,
                        amenities = selectedAmenities.toList(),
                        images = existing?.images.orEmpty(),
                        isOccupied = occupied,
                        availabilityDate = availabilityDate,
                        depositAmount = validDeposit
                    )
                    viewModel.saveListing(context, listing, pickedImages, existing != null, onDone)
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.loading) "Saving..." else if (existing == null) "Add listing" else "Save listing") }
        }
    }
    if (showDatePicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = availabilityDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    availabilityDate = pickerState.selectedDateMillis ?: availabilityDate
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(pickerState) }
    }
}

@Suppress("DEPRECATION")
private suspend fun geocodeLocation(context: Context, query: String): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val searches = listOf(query, "$query, Gaborone", "$query, Botswana")
            searches.firstNotNullOfOrNull { search ->
                geocoder.getFromLocationName(search, 1)
                    ?.firstOrNull()
                    ?.let { it.latitude to it.longitude }
            }
        }.getOrNull()
    }

private fun formatListingDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))

private val ListingAreaOptions = listOf(
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
