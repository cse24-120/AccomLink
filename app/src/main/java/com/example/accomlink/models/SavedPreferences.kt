package com.example.accomlink.models

data class SavedPreferences(
    val userId: String = "",
    val filter: ListingFilter = ListingFilter(),
    val alertsEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "query" to filter.query,
        "minPrice" to filter.minPrice,
        "maxPrice" to filter.maxPrice,
        "location" to filter.location,
        "roomType" to filter.roomType,
        "amenities" to filter.amenities,
        "availabilityDate" to filter.availabilityDate,
        "alertsEnabled" to alertsEnabled,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromDocument(userId: String, data: Map<String, Any>): SavedPreferences = SavedPreferences(
            userId = userId,
            filter = ListingFilter(
                query = data["query"] as? String ?: "",
                minPrice = (data["minPrice"] as? Number)?.toDouble() ?: 0.0,
                maxPrice = (data["maxPrice"] as? Number)?.toDouble() ?: 10000.0,
                location = data["location"] as? String ?: "",
                roomType = data["roomType"] as? String ?: "",
                amenities = (data["amenities"] as? List<*>)
                    ?.mapNotNull { it?.toString() }
                    .orEmpty(),
                availabilityDate = (data["availabilityDate"] as? Number)?.toLong()
            ),
            alertsEnabled = data["alertsEnabled"] as? Boolean ?: false,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
