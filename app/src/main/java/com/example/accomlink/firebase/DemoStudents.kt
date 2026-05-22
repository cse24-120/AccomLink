package com.example.accomlink.firebase

import com.example.accomlink.models.UserProfile
import com.example.accomlink.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

object DemoStudents {
    const val SEED_PASSWORD = "12341234"

    private val names = listOf(
        "Kabelo Motsumi", "Naledi Dube", "Thabo Molefe", "Lerato Kgosi", "Neo Williams",
        "Amantle Ndlovu", "Tshepo Van Wyk", "Boitumelo Smith", "Mpho Moyo", "Katlego Brown",
        "Gaone Phiri", "Refilwe Johnson", "Tumelo Raditladi", "Kgomotso Adams", "Onalenna Sebina",
        "Palesa Morapedi", "Bakang Pretorius", "Gofaone Taylor", "Tebogo Mogapi", "Lesego Daniels",
        "Karabo Ramotswa", "Lorato Jacobs", "Olebile Mabiletsa", "Kedibone Campbell", "Bontle Moremi",
        "Aobakwe Williams", "Keitumetse Khan", "Omphile Cloete", "Tshiamo Ramatlhodi", "Atang Meyer",
        "Phenyo Sekgoma", "Dineo September", "Masego Kgafela", "Goitseone Botha", "Khumo Matlho",
        "Keneilwe Petersen", "Oarabile Moagi", "Rorisang Davids", "Kamogelo Motshwane", "Thatayaone Isaacs",
        "Botshelo Kgosidintsi", "Maitseo Ferreira", "Oratile Thema", "Wame Coetzee", "Bame Moloi",
        "Resego Swanepoel", "Tlotlo Makgato", "Kabo Olivier", "Reneilwe Charles", "Tumo Beukes"
    )

    val profiles: List<UserProfile> = names.mapIndexed { index, fullName ->
        val parts = fullName.lowercase().split(" ")
        UserProfile(
            id = "student-${index + 1}",
            name = fullName,
            email = "${parts.first()}.${parts.last()}@student.bac.bw",
            phone = "71${(100000 + index).toString().takeLast(6)}",
            role = UserRole.Student
        )
    }

    fun passwordHash(password: String = SEED_PASSWORD): String = password.sha256()

    suspend fun seedToFirestore(
        db: FirebaseFirestore = FirebaseFirestore.getInstance(),
        overwrite: Boolean = true
    ): Int {
        val hash = passwordHash()
        val batch = db.batch()
        var written = 0

        profiles.forEach { student ->
            val doc = db.collection("users").document(student.id)
            if (!overwrite) {
                val existing = doc.get().await()
                if (existing.exists()) return@forEach
            }
            val normalizedEmail = student.email.trim().lowercase()
            batch.set(
                doc,
                student.toMap() + mapOf(
                    "emailNormalized" to normalizedEmail,
                    "passwordHash" to hash
                )
            )
            written++
        }

        batch.commit().await()
        return written
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
