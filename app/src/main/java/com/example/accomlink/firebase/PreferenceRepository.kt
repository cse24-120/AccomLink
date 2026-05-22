package com.example.accomlink.firebase

import com.example.accomlink.models.ListingFilter
import com.example.accomlink.models.SavedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PreferenceRepository(
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    private val offlinePreferences = MutableStateFlow<Map<String, SavedPreferences>>(emptyMap())

    fun preferences(userId: String): Flow<SavedPreferences?> {
        val firestore = db ?: return offlinePreferences.map { it[userId] }
        return callbackFlow {
            val listener = firestore.collection("studentPreferences").document(userId)
                .addSnapshotListener { doc, _ ->
                    trySend(
                        if (doc != null && doc.exists()) {
                            SavedPreferences.fromDocument(userId, doc.data ?: emptyMap())
                        } else {
                            null
                        }
                    )
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun savePreferences(userId: String, filter: ListingFilter, alertsEnabled: Boolean) {
        require(userId.isNotBlank()) { "Please log in before saving preferences." }
        val preferences = SavedPreferences(
            userId = userId,
            filter = filter,
            alertsEnabled = alertsEnabled
        )
        val firestore = db
        if (firestore == null) {
            offlinePreferences.value = offlinePreferences.value + (userId to preferences)
            return
        }
        firestore.collection("studentPreferences").document(userId)
            .set(preferences.toMap())
            .await()
    }
}
