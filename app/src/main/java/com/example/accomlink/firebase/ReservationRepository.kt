package com.example.accomlink.firebase

import com.example.accomlink.models.Listing
import com.example.accomlink.models.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/** Handles reservation creation and lookup in Firestore. */
class ReservationRepository(
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    /** Streams reservations for a student ordered by latest first. */
    fun reservationsForStudent(studentId: String): Flow<List<Reservation>> {
        val firestore = db ?: return flowOf(emptyList())
        return callbackFlow {
            val listener = firestore.collection("reservations")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshot, _ ->
                    val reservations = snapshot?.documents
                        ?.map { Reservation.fromDocument(it.id, it.data ?: emptyMap()) }
                        ?.sortedByDescending { it.reservedAt }
                        .orEmpty()
                    trySend(reservations)
                }
            awaitClose { listener.remove() }
        }
    }

    /** Streams reservations for listings owned by a landlord. */
    fun reservationsForLandlord(landlordId: String): Flow<List<Reservation>> {
        val firestore = db ?: return flowOf(emptyList())
        return callbackFlow {
            val listener = firestore.collection("reservations")
                .whereEqualTo("landlordId", landlordId)
                .addSnapshotListener { snapshot, _ ->
                    val reservations = snapshot?.documents
                        ?.map { Reservation.fromDocument(it.id, it.data ?: emptyMap()) }
                        ?.sortedByDescending { it.reservedAt }
                        .orEmpty()
                    trySend(reservations)
                }
            awaitClose { listener.remove() }
        }
    }

    /** Loads one reservation by id. */
    suspend fun reservation(id: String): Reservation? {
        val doc = db?.collection("reservations")?.document(id)?.get()?.await() ?: return null
        return if (doc.exists()) Reservation.fromDocument(doc.id, doc.data ?: emptyMap()) else null
    }

    /** Creates a reservation and marks its listing as occupied atomically enough for this prototype. */
    suspend fun createReservation(
        listing: Listing,
        studentId: String,
        studentName: String,
        studentEmail: String,
        studentPhone: String,
        reference: String
    ): Reservation {
        val firestore = db ?: error("Firebase is not configured.")
        val latestListing = firestore.collection("listings").document(listing.id).get().await()
        val occupied = latestListing.getBoolean("isOccupied") ?: listing.isOccupied
        require(!occupied) { "Sorry, this listing was just reserved by someone else." }
        val doc = firestore.collection("reservations").document()
        val reservation = Reservation(
            id = doc.id,
            listingId = listing.id,
            landlordId = listing.landlordId,
            listingTitle = listing.title,
            studentId = studentId,
            studentName = studentName,
            studentEmail = studentEmail,
            studentPhone = studentPhone,
            amountPaid = listing.price,
            depositPaid = listing.depositAmount,
            referenceNumber = reference
        )
        doc.set(reservation.toMap()).await()
        firestore.collection("listings").document(listing.id).update(
            mapOf(
                "isOccupied" to true,
                "status" to "RESERVED"
            )
        ).await()
        return reservation
    }
}
