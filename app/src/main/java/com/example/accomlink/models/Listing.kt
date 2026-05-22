package com.example.accomlink.models

data class Listing(
    val id: String = "",
    val landlordId: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val location: String = "",
    val roomType: String = "Single room",
    val furnished: Boolean = true,
    val latitude: Double = BAC_LATITUDE,
    val longitude: Double = BAC_LONGITUDE,
    val amenities: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val isOccupied: Boolean = false,
    val status: String = if (isOccupied) STATUS_RESERVED else STATUS_AVAILABLE,
    val landlordPhone: String = "",
    val availabilityDate: Long = System.currentTimeMillis(),
    val depositAmount: Double = price,
    val createdAt: Long = System.currentTimeMillis()
) {
    val normalizedStatus: String
        get() = when {
            status.equals(STATUS_RESERVED, ignoreCase = true) -> STATUS_RESERVED
            status.equals("OCCUPIED", ignoreCase = true) -> STATUS_RESERVED
            isOccupied -> STATUS_RESERVED
            else -> STATUS_AVAILABLE
        }

    val statusLabel: String get() = normalizedStatus.lowercase().replaceFirstChar { it.uppercase() }
    val isAvailable: Boolean get() = normalizedStatus == STATUS_AVAILABLE

    fun toMap(): Map<String, Any> = mapOf(
        "landlordId" to landlordId,
        "title" to title,
        "description" to description,
        "price" to price,
        "location" to location,
        "roomType" to roomType,
        "furnished" to furnished,
        "latitude" to latitude,
        "longitude" to longitude,
        "amenities" to amenities,
        "images" to images,
        "isOccupied" to (normalizedStatus == STATUS_RESERVED),
        "status" to normalizedStatus,
        "landlordPhone" to landlordPhone,
        "availabilityDate" to availabilityDate,
        "depositAmount" to depositAmount,
        "createdAt" to createdAt
    )

    companion object {
        const val BAC_LATITUDE = -24.679977
        const val BAC_LONGITUDE = 25.926416
        const val STATUS_AVAILABLE = "AVAILABLE"
        const val STATUS_RESERVED = "RESERVED"

        fun fromDocument(id: String, data: Map<String, Any>): Listing {
            val storedStatus = data["status"] as? String
            val occupied = data["isOccupied"] as? Boolean
                ?: listOf("OCCUPIED", STATUS_RESERVED).any { storedStatus.equals(it, true) }
            val normalizedStatus = when {
                storedStatus.equals(STATUS_RESERVED, ignoreCase = true) -> STATUS_RESERVED
                storedStatus.equals("OCCUPIED", ignoreCase = true) -> STATUS_RESERVED
                occupied -> STATUS_RESERVED
                else -> STATUS_AVAILABLE
            }
            return Listing(
                id = id,
                landlordId = data["landlordId"] as? String ?: data["landlord_id"] as? String ?: "",
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                location = data["location"] as? String ?: "",
                roomType = data["roomType"] as? String ?: data["type"] as? String ?: "Single room",
                furnished = data["furnished"] as? Boolean ?: true,
                latitude = (data["latitude"] as? Number)?.toDouble() ?: BAC_LATITUDE,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: BAC_LONGITUDE,
                amenities = data.asStringList("amenities"),
                images = data.asStringList("images").ifEmpty { data.asStringList("imageUri") },
                isOccupied = normalizedStatus == STATUS_RESERVED,
                status = normalizedStatus,
                landlordPhone = data["landlordPhone"] as? String ?: "",
                availabilityDate = (data["availabilityDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                depositAmount = (data["depositAmount"] as? Number)?.toDouble() ?: ((data["price"] as? Number)?.toDouble() ?: 0.0),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }

        private fun Map<String, Any>.asStringList(key: String): List<String> =
            when (val value = this[key]) {
                is String -> value.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }
                is List<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }
    }
}
