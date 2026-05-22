package com.example.accomlink.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accomlink.firebase.AuthRepository
import com.example.accomlink.firebase.ReservationRepository
import com.example.accomlink.models.Listing
import com.example.accomlink.models.Reservation
import com.example.accomlink.utils.ResultState
import java.util.Calendar
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Coordinates simulated deposit payment and reservation creation. */
class PaymentViewModel(
    private val reservations: ReservationRepository = ReservationRepository(),
    private val auth: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state.asStateFlow()

    private val _reservation = MutableStateFlow<Reservation?>(null)
    val reservation: StateFlow<Reservation?> = _reservation.asStateFlow()

    fun loadReservation(reservationId: String) {
        if (reservationId.isBlank() || _reservation.value?.id == reservationId) return
        viewModelScope.launch {
            runCatching { reservations.reservation(reservationId) }
                .onSuccess { _reservation.value = it }
        }
    }

    fun clearState() {
        _state.value = ResultState()
    }

    /** Validates card details, simulates processing, and writes the reservation. */
    fun pay(
        listing: Listing,
        cardholder: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        onPaid: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = ResultState(loading = true)
            runCatching {
                require(cardholder.trim().isNotBlank()) { "Cardholder name is required." }
                require(cardNumber.filter(Char::isDigit).length == 16) { "Enter any 16-digit card number." }
                require(Regex("""\d{2}/\d{2}""").matches(expiry)) { "Expiry must be MM/YY." }
                require(!isExpired(expiry)) { "Card expiry must be in the future." }
                require(cvv.filter(Char::isDigit).length == 3) { "CVV must be 3 digits." }
                val studentId = auth.currentUserId ?: error("Please log in before reserving.")
                val student = auth.getProfile(studentId)
                delay(1500)
                val reservation = reservations.createReservation(
                    listing = listing,
                    studentId = studentId,
                    studentName = student?.name.orEmpty(),
                    studentEmail = student?.email.orEmpty(),
                    studentPhone = student?.phone.orEmpty(),
                    reference = generateReference()
                )
                _reservation.value = reservation
                reservation
            }.onSuccess {
                _state.value = ResultState(message = "Payment successful")
                onPaid(it.id)
            }.onFailure {
                _state.value = ResultState(error = it.message ?: "Payment failed")
            }
        }
    }

    private fun generateReference(): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "AC-" + (1..8).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun isExpired(expiry: String): Boolean {
        val parts = expiry.split("/")
        val month = parts.getOrNull(0)?.toIntOrNull() ?: return true
        val year = parts.getOrNull(1)?.toIntOrNull() ?: return true
        if (month !in 1..12) return true
        val current = Calendar.getInstance()
        val currentYear = current.get(Calendar.YEAR) % 100
        val currentMonth = current.get(Calendar.MONTH) + 1
        return year < currentYear || (year == currentYear && month < currentMonth)
    }
}
