package com.example.accomlink.models

data class ChatRoom(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val landlordId: String = "",
    val landlordName: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val participantIds: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "listingId" to listingId,
        "listingTitle" to listingTitle,
        "landlordId" to landlordId,
        "landlordName" to landlordName,
        "studentId" to studentId,
        "studentName" to studentName,
        "participantIds" to participantIds,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): ChatRoom = ChatRoom(
            id = id,
            listingId = data["listingId"] as? String ?: "",
            listingTitle = data["listingTitle"] as? String ?: "",
            landlordId = data["landlordId"] as? String ?: "",
            landlordName = data["landlordName"] as? String ?: "",
            studentId = data["studentId"] as? String ?: "",
            studentName = data["studentName"] as? String ?: "",
            participantIds = (data["participantIds"] as? List<*>)
                ?.mapNotNull { it?.toString() }
                .orEmpty(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        )

        fun idFor(listingId: String, studentId: String, landlordId: String): String =
            listOf(listingId, studentId, landlordId)
                .joinToString("_")
                .replace(Regex("""[^A-Za-z0-9_-]"""), "_")
    }
}
