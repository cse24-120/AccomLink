package com.example.accomlink.models

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: UserRole = UserRole.Student,
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "roomId" to roomId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderRole" to senderRole.firestoreValue,
        "text" to text,
        "createdAt" to createdAt
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): ChatMessage = ChatMessage(
            id = id,
            roomId = data["roomId"] as? String ?: "",
            senderId = data["senderId"] as? String ?: "",
            senderName = data["senderName"] as? String ?: "",
            senderRole = UserRole.from(data["senderRole"] as? String),
            text = data["text"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
