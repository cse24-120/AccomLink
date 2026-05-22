package com.example.accomlink.models

data class Favorite(
    val id: String = "",
    val userId: String = "",
    val listingId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "listingId" to listingId,
        "createdAt" to createdAt
    )
}
