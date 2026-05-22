package com.example.accomlink.models

data class ListingFilter(
    val query: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 10000.0,
    val location: String = "",
    val roomType: String = "",
    val amenities: List<String> = emptyList(),
    val availabilityDate: Long? = null
)
