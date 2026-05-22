package com.example.accomlink.models

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.Student,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "email" to email,
        "phone" to phone,
        "role" to role.firestoreValue,
        "createdAt" to createdAt
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): UserProfile = UserProfile(
            id = id,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            role = UserRole.from(data["role"] as? String),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
