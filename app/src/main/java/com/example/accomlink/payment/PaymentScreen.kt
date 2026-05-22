package com.example.accomlink.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accomlink.models.Listing

/** Simulated card payment form for paying a listing deposit. */
@Composable
fun PaymentScreen(listing: Listing, viewModel: PaymentViewModel, onPaid: (String) -> Unit) {
    var cardholder by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var cvv by rememberSaveable { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(listing.id) {
        viewModel.clearState()
    }

    LaunchedEffect(state.error, state.message) {
        state.error?.let { snackbar.showSnackbar(it) }
        state.message?.let { snackbar.showSnackbar(it) }
    }

    androidx.compose.material3.Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Pay deposit", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cardholder.ifBlank { "CARDHOLDER NAME" }, color = MaterialTheme.colorScheme.onPrimary)
                    Text(mask(cardNumber), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                    Text(expiry.ifBlank { "MM/YY" }, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(listing.title, fontWeight = FontWeight.Bold)
                    Text("Total due: BWP ${listing.depositAmount.toInt()} (deposit)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(cardholder, { cardholder = it }, label = { Text("Cardholder name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it.filter(Char::isDigit).take(16).chunked(4).joinToString(" ") },
                label = { Text("Card number (any 16 digits)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(expiry, { expiry = it.filter { ch -> ch.isDigit() || ch == '/' }.take(5) }, label = { Text("Expiry MM/YY") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cvv, { cvv = it.filter(Char::isDigit).take(3) }, label = { Text("CVV") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { viewModel.pay(listing, cardholder, cardNumber, expiry, cvv, onPaid) },
                enabled = !state.loading && !listing.isOccupied,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator() else Text(if (listing.isOccupied) "Reserved" else "Pay")
            }
        }
    }
}

private fun mask(number: String): String {
    val digits = number.filter(Char::isDigit)
    return if (digits.isBlank()) "0000 0000 0000 0000" else digits.padEnd(16, '*').chunked(4).joinToString(" ")
}
