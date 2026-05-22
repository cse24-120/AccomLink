package com.example.accomlink.firebase

import com.example.accomlink.models.UserProfile
import com.example.accomlink.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object DemoLandlords {
    private val names = listOf(
        "MmaKgosi Letlole", "Tumelo Raditladi", "Grace Mooketsi", "Oaitse Ramorwa", "Kelebogile Moagi",
        "Patrick Molebatsi", "Dineo Seretse", "Mpho Letsholo", "Thato Kenosi", "Bontle Sebina"
    )

    val profiles: List<UserProfile> = names.mapIndexed { index, fullName ->
        val parts = fullName.lowercase().split(" ")
        UserProfile(
            id = "landlord-${index + 1}",
            name = fullName,
            email = "${parts.first()}.${parts.last()}@landlord.accomlink.bw",
            phone = "73${(200000 + index).toString().takeLast(6)}",
            role = UserRole.Landlord
        )
    }

    suspend fun seedToFirestore(
        db: FirebaseFirestore = FirebaseFirestore.getInstance(),
        overwrite: Boolean = true
    ): Int {
        val hash = DemoStudents.passwordHash()
        val batch = db.batch()
        var written = 0

        profiles.forEach { landlord ->
            val doc = db.collection("users").document(landlord.id)
            if (!overwrite) {
                val existing = doc.get().await()
                if (existing.exists()) return@forEach
            }
            val normalizedEmail = landlord.email.trim().lowercase()
            batch.set(
                doc,
                landlord.toMap() + mapOf(
                    "emailNormalized" to normalizedEmail,
                    "passwordHash" to hash
                )
            )
            written++
        }

        batch.commit().await()
        return written
    }
}
