package com.example.accomlink.models

/** Confirmed student reservation created after a simulated deposit payment. */
data class Reservation(
    val id: String = "",
    val listingId: String = "",
    val landlordId: String = "",
    val listingTitle: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val studentPhone: String = "",
    val amountPaid: Double = 0.0,
    val depositPaid: Double = 0.0,
    val referenceNumber: String = "",
    val reservedAt: Long = System.currentTimeMillis(),
    val status: String = "CONFIRMED"
) {
    /** Converts the reservation to Firestore fields. */
    fun toMap(): Map<String, Any> = mapOf(
        "listingId" to listingId,
        "landlordId" to landlordId,
        "listingTitle" to listingTitle,
        "studentId" to studentId,
        "studentName" to studentName,
        "studentEmail" to studentEmail,
        "studentPhone" to studentPhone,
        "amountPaid" to amountPaid,
        "depositPaid" to depositPaid,
        "referenceNumber" to referenceNumber,
        "reservedAt" to reservedAt,
        "status" to status
    )

    companion object {
        /** Builds a reservation from Firestore data. */
        fun fromDocument(id: String, data: Map<String, Any>): Reservation = Reservation(
            id = id,
            listingId = data["listingId"] as? String ?: "",
            landlordId = data["landlordId"] as? String ?: "",
            listingTitle = data["listingTitle"] as? String ?: "",
            studentId = data["studentId"] as? String ?: "",
            studentName = data["studentName"] as? String ?: "",
            studentEmail = data["studentEmail"] as? String ?: "",
            studentPhone = data["studentPhone"] as? String ?: "",
            amountPaid = (data["amountPaid"] as? Number)?.toDouble()
                ?: (data["depositPaid"] as? Number)?.toDouble()
                ?: 0.0,
            depositPaid = (data["depositPaid"] as? Number)?.toDouble() ?: 0.0,
            referenceNumber = data["referenceNumber"] as? String ?: "",
            reservedAt = (data["reservedAt"] as? Number)?.toLong() ?: 0L,
            status = data["status"] as? String ?: "CONFIRMED"
        )
    }
}
