package com.example.accomlink.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accomlink.models.Listing
import com.example.accomlink.models.Reservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shows a successful reservation payment receipt. */
@Composable
fun ReceiptScreen(reservation: Reservation?, listing: Listing?, onReservations: () -> Unit, onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Reservation confirmed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(listing?.title.orEmpty(), fontWeight = FontWeight.Bold)
        Text(listing?.location.orEmpty())
        Text("Deposit paid: BWP ${reservation?.depositPaid?.toInt() ?: 0}")
        Text("Date: ${formatDate(reservation?.reservedAt ?: System.currentTimeMillis())}")
        Text("Reference: ${reservation?.referenceNumber.orEmpty()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Button(onClick = onReservations, modifier = Modifier.fillMaxWidth()) { Text("View My Reservations") }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Back to Home") }
    }
}

private fun formatDate(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))
