package com.example.accomlink.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accomlink.models.Listing
import com.example.accomlink.models.Reservation
import com.example.accomlink.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lists reservations for the current student. */
@Composable
fun ReservationsScreen(
    reservations: List<Reservation>,
    listings: List<Listing>,
    onOpen: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("My Reservations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        if (reservations.isEmpty()) item { EmptyState("No reservations yet", "Reserved rooms and payment references will appear here.") }
        items(reservations, key = { reservation -> reservation.id.ifBlank { "${reservation.listingId}-${reservation.referenceNumber}" } }) { reservation ->
            val listing = listings.firstOrNull { it.id == reservation.listingId }
            Card(
                onClick = { onOpen(reservation.listingId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReservationRow("Reservation ID", reservation.referenceNumber.ifBlank { reservation.id })
                    ReservationRow("Property", listing?.title ?: reservation.listingTitle.ifBlank { "Reserved listing" }, valueWeight = FontWeight.Bold)
                    ReservationRow("Amount Paid", formatCurrency(reservation.amountPaid), valueColor = MaterialTheme.colorScheme.primary, valueWeight = FontWeight.Black)
                    ReservationRow("Deposit", formatCurrency(reservation.depositPaid))
                    ReservationRow("Date", formatDate(reservation.reservedAt))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Status", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Surface(
                            color = Color(0xFF31D267),
                            contentColor = Color.Black,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(reservation.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueWeight: FontWeight = FontWeight.SemiBold
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(value, color = valueColor, fontWeight = valueWeight)
    }
}

private fun formatCurrency(value: Double): String = "P${"%,.2f".format(value)}"

private fun formatDate(value: Long): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(value))
