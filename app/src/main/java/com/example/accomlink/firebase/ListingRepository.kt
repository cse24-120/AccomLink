package com.example.accomlink.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import com.example.accomlink.models.Listing
import com.example.accomlink.models.ListingFilter
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ListingRepository(
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val storage: FirebaseStorage? = runCatching { FirebaseStorage.getInstance() }.getOrNull()
) {
    fun allListings(): Flow<List<Listing>> {
        val firestore = db ?: return flowOf(sampleListings)
        return callbackFlow {
            val listener = firestore.collection("listings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    val listings = snapshot?.documents?.map { doc ->
                        Listing.fromDocument(doc.id, doc.data ?: emptyMap())
                    }.orEmpty()
                    trySend(listings)
                }
            awaitClose { listener.remove() }
        }
    }

    fun availableListings(): Flow<List<Listing>> =
        allListings().map { listings -> listings.filter { it.isAvailable } }

    fun landlordListings(landlordId: String): Flow<List<Listing>> =
        allListings().map { listings -> listings.filter { it.landlordId == landlordId } }

    fun listing(listingId: String): Flow<Listing?> {
        if (listingId.isBlank()) return flowOf(null)
        return callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(sampleListings.firstOrNull { it.id == listingId })
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("listings").document(listingId)
            .addSnapshotListener { doc, _ ->
                trySend(if (doc != null && doc.exists()) Listing.fromDocument(doc.id, doc.data ?: emptyMap()) else null)
        }
        awaitClose { listener.remove() }
    }
    }

    fun search(filter: ListingFilter): Flow<List<Listing>> = availableListings().map { listings ->
        listings.filter { listing ->
            val text = filter.query.isBlank() ||
                listing.title.contains(filter.query, true) ||
                listing.description.contains(filter.query, true) ||
                listing.location.contains(filter.query, true)
            val price = listing.price in filter.minPrice..filter.maxPrice
            val area = filter.location.isBlank() || listing.location.contains(filter.location, true)
            val room = filter.roomType.isBlank() || listing.roomType.equals(filter.roomType, true)
            val amenities = filter.amenities.isEmpty() ||
                filter.amenities.all { selected -> listing.amenities.any { it.equals(selected, ignoreCase = true) } }
            val date = filter.availabilityDate == null || listing.availabilityDate <= filter.availabilityDate
            text && price && area && room && amenities && date
        }
    }

    suspend fun addListing(context: Context, listing: Listing, imageUris: List<Uri>): String {
        val firestore = db ?: error("Firebase is not configured.")
        val doc = firestore.collection("listings").document()
        val images = uploadImages(context, doc.id, imageUris)
        val savedListing = listing.copy(id = doc.id, images = listing.images + images)
        doc.set(savedListing.toMap()).await()
        return doc.id
    }

    suspend fun updateListing(context: Context, listing: Listing, imageUris: List<Uri>) {
        val firestore = db ?: error("Firebase is not configured.")
        val uploaded = uploadImages(context, listing.id, imageUris)
        firestore.collection("listings").document(listing.id)
            .set(listing.copy(images = listing.images + uploaded).toMap())
            .await()
    }

    suspend fun deleteListing(listingId: String) {
        db?.collection("listings")?.document(listingId)?.delete()?.await()
    }

    suspend fun setOccupied(listingId: String, occupied: Boolean) {
        db?.collection("listings")?.document(listingId)?.update(
            mapOf(
                "isOccupied" to occupied,
                "status" to if (occupied) "RESERVED" else "AVAILABLE"
            )
        )?.await()
    }

    private suspend fun uploadImages(context: Context, listingId: String, imageUris: List<Uri>): List<String> {
        if (imageUris.isEmpty()) return emptyList()
        return imageUris.mapIndexed { index, uri ->
            val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType) ?: "jpg"
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()
            val path = "listings/$listingId/image_${System.currentTimeMillis()}_$index.$extension"
            val store = storage
            if (store == null) {
                imageAsFirestoreDataUrl(context, uri)
            } else {
                runCatching {
                    uploadImageToFirstAvailableBucket(context, store, path, uri, metadata)
                }.getOrElse {
                    imageAsFirestoreDataUrl(context, uri)
                }
            }
        }
    }

    private suspend fun uploadImageToFirstAvailableBucket(
        context: Context,
        defaultStore: FirebaseStorage,
        path: String,
        uri: Uri,
        metadata: StorageMetadata
    ): String {
        val refs = storageReferences(defaultStore, path)
        var lastErrorMessage = "Unknown Firebase Storage error."
        var uploadSucceeded = false
        refs.forEach { ref ->
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Could not read selected image.")
                stream.use {
                    ref.putStream(it, metadata).await()
                }
                uploadSucceeded = true
                return ref.downloadUrlWithRetry().toString()
            } catch (error: StorageException) {
                lastErrorMessage = "bucket=${ref.bucket}, code=${error.errorCode}, ${error.message.orEmpty()}"
            }
        }
        throw IllegalStateException(
            if (uploadSucceeded) {
                "Image uploaded, but Firebase blocked the download link. Check Storage read rules. $lastErrorMessage"
            } else {
                "Firebase Storage rejected the image upload. Check Storage write rules. $lastErrorMessage"
            }
        )
    }

    private fun storageReferences(defaultStore: FirebaseStorage, path: String): List<StorageReference> {
        val defaultRef = defaultStore.reference.child(path)
        val projectId = FirebaseApp.getInstance().options.projectId
        val legacyRef = projectId
            ?.takeIf { it.isNotBlank() && !defaultRef.bucket.equals("$it.appspot.com", ignoreCase = true) }
            ?.let { runCatching { FirebaseStorage.getInstance("gs://$it.appspot.com").reference.child(path) }.getOrNull() }
        return listOfNotNull(defaultRef, legacyRef).distinctBy { it.bucket }
    }

    private suspend fun StorageReference.downloadUrlWithRetry(): Uri {
        var lastError: Exception? = null
        repeat(4) { attempt ->
            try {
                return downloadUrl.await()
            } catch (error: StorageException) {
                lastError = error
                if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw error
                delay(250L * (attempt + 1))
            }
        }
        throw lastError ?: error("Uploaded image could not be found in Firebase Storage.")
    }

    private fun imageAsFirestoreDataUrl(context: Context, uri: Uri): String {
        val source = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("Could not read selected image.")
        val scaled = source.scaledToFit(maxSide = 720)
        val bytes = ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 45, output)
            output.toByteArray()
        }
        if (scaled !== source) scaled.recycle()
        source.recycle()
        return "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    private fun Bitmap.scaledToFit(maxSide: Int): Bitmap {
        val largestSide = maxOf(width, height)
        if (largestSide <= maxSide) return this
        val scale = maxSide.toFloat() / largestSide
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    private companion object {
        private val areas = listOf("Block 3", "Block 5", "Block 6", "Block 7", "Block 8", "Phase 2", "Phase 4", "Mogoditshane", "Tlokweng", "Gaborone West", "Broadhurst", "Extension 2", "Phakalane", "Fairgrounds", "Village")
        private val roomTypes = listOf("Single room", "Double room", "Studio", "Bedsitter", "1-bed flat", "2-bed flat", "Shared house", "Bachelor pad", "En-suite", "Self-contained unit")
        private val amenitiesPool = listOf("Wi-Fi", "Water", "Electricity", "Security")
        private val photos = listOf(
            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2",
            "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267",
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85",
            "https://images.unsplash.com/photo-1484154218962-a197022b5858",
            "https://images.unsplash.com/photo-1493809842364-78817add7ffb"
        )

        val sampleListings = List(50) { index ->
            val area = areas[index % areas.size]
            val type = roomTypes[index % roomTypes.size]
            val price = 900.0 + ((index * 137) % 6600)
            val date = System.currentTimeMillis() + (index % 90) * 24L * 60L * 60L * 1000L
            Listing(
                id = "demo-listing-${index + 1}",
                landlordId = "demo-landlord",
                title = "$type in $area",
                description = "Comfortable student accommodation in $area with practical access to BAC routes and daily services.",
                price = price,
                location = area,
                roomType = type,
                furnished = false,
                latitude = -24.55 - ((index * 37) % 200) / 1000.0,
                longitude = 25.85 + ((index * 41) % 200) / 1000.0,
                amenities = List(4) { amenitiesPool[(index + it) % amenitiesPool.size] },
                images = List(2) { photos[(index + it) % photos.size] },
                isOccupied = index % 4 == 0,
                landlordPhone = "7000${(1000 + index)}",
                availabilityDate = date,
                depositAmount = price
            )
        }
    }
}
