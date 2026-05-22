package com.example.accomlink.firebase

import com.example.accomlink.models.Favorite
import com.example.accomlink.models.Listing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FavoriteRepository(
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val listingRepository: ListingRepository = ListingRepository()
) {
    private val offlineFavoriteIds = MutableStateFlow(emptySet<String>())

    fun favoriteIds(userId: String): Flow<Set<String>> {
        val firestore = db ?: return offlineFavoriteIds.asStateFlow()
        return callbackFlow {
            val listener = firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    val ids = snapshot?.documents?.mapNotNull { it.getString("listingId") }?.toSet().orEmpty()
                    trySend(ids)
                }
            awaitClose { listener.remove() }
        }
    }

    fun favoriteListings(userId: String): Flow<List<Listing>> =
        combine(listingRepository.allListings(), favoriteIds(userId)) { listings, ids ->
            listings.filter { it.id in ids }
        }

    suspend fun toggleFavorite(userId: String, listingId: String, currentlyFavorite: Boolean) {
        val firestore = db
        if (firestore == null) {
            offlineFavoriteIds.value = if (currentlyFavorite) {
                offlineFavoriteIds.value - listingId
            } else {
                offlineFavoriteIds.value + listingId
            }
            return
        }
        if (currentlyFavorite) {
            val snapshot = firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            snapshot.documents.forEach { it.reference.delete().await() }
        } else {
            firestore.collection("favorites").add(Favorite(userId = userId, listingId = listingId).toMap()).await()
        }
    }
}
