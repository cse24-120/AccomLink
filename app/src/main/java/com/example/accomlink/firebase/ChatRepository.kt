package com.example.accomlink.firebase

import com.example.accomlink.models.ChatMessage
import com.example.accomlink.models.ChatRoom
import com.example.accomlink.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    fun roomsForUser(userId: String): Flow<List<ChatRoom>> {
        val firestore = db ?: return flowOf(emptyList())
        if (userId.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val listener = firestore.collection("chatRooms")
                .whereArrayContains("participantIds", userId)
                .addSnapshotListener { snapshot, _ ->
                    val rooms = snapshot?.documents
                        ?.map { doc -> ChatRoom.fromDocument(doc.id, doc.data ?: emptyMap()) }
                        ?.sortedByDescending { it.updatedAt }
                        .orEmpty()
                    trySend(rooms)
                }
            awaitClose { listener.remove() }
        }
    }

    fun messages(roomId: String): Flow<List<ChatMessage>> {
        val firestore = db ?: return flowOf(emptyList())
        if (roomId.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val listener = firestore.collection("chatRooms")
                .document(roomId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    val messages = snapshot?.documents?.map { doc ->
                        ChatMessage.fromDocument(doc.id, doc.data ?: emptyMap())
                    }.orEmpty()
                    trySend(messages)
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun sendMessage(room: ChatRoom, text: String, sender: UserProfile) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val firestore = db ?: error("Firebase is not configured.")
        val senderName = sender.name.ifBlank { sender.email.ifBlank { "AccomLink user" } }
        val updatedRoom = room.copy(
            landlordName = if (sender.id == room.landlordId) senderName else room.landlordName,
            studentName = if (sender.id == room.studentId) senderName else room.studentName,
            participantIds = listOf(room.studentId, room.landlordId).filter { it.isNotBlank() }.distinct(),
            updatedAt = System.currentTimeMillis()
        )
        val roomRef = firestore.collection("chatRooms").document(updatedRoom.id)
        roomRef.set(updatedRoom.toMap()).await()
        roomRef.collection("messages").add(
            ChatMessage(
                roomId = updatedRoom.id,
                senderId = sender.id,
                senderName = senderName,
                senderRole = sender.role,
                text = cleanText
            ).toMap()
        ).await()
    }
}
