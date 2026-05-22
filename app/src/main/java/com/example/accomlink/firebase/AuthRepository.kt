package com.example.accomlink.firebase

import android.content.Context
import com.example.accomlink.models.UserProfile
import com.example.accomlink.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val context: Context? = null,
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    companion object {
        val profile = MutableStateFlow<UserProfile?>(null)
        val demoStudents: List<UserProfile> = DemoStudents.profiles
        val demoLandlords: List<UserProfile> = DemoLandlords.profiles

        private const val SessionPreferences = "accomlink_session"
        private const val SessionUserIdKey = "user_id"
        private const val SessionNameKey = "name"
        private const val SessionEmailKey = "email"
        private const val SessionPhoneKey = "phone"
        private const val SessionRoleKey = "role"
    }

    val currentUserId: String? get() = profile.value?.id ?: savedUserId()
    val currentEmail: String get() = profile.value?.email.orEmpty()

    fun cachedProfile(): UserProfile? = profile.value ?: savedProfile()

    fun authState(): Flow<String?> = profile.map { it?.id }

    suspend fun restoreSession(): UserProfile? {
        val uid = savedUserId().orEmpty()
        if (uid.isBlank()) return null
        return getProfile(uid)?.also { profile.value = it }
    }

    suspend fun login(email: String, password: String): UserProfile {
        val firestore = db ?: error("Firebase is not configured.")
        val normalizedEmail = email.normalizedEmail()
        require(normalizedEmail.isNotBlank()) { "Email is required." }
        require(password.isNotBlank()) { "Password is required." }

        val snapshot = firestore.collection("users")
            .whereEqualTo("emailNormalized", normalizedEmail)
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: error("No account found for this email.")
        val savedHash = doc.getString("passwordHash").orEmpty()
        require(savedHash == password.sha256()) { "Incorrect password." }

        return UserProfile.fromDocument(doc.id, doc.data ?: emptyMap()).also {
            saveProfile(it)
            profile.value = it
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: UserRole
    ): UserProfile {
        val firestore = db ?: error("Firebase is not configured.")
        val normalizedEmail = email.normalizedEmail()
        require(name.isNotBlank()) { "Full name is required." }
        require(normalizedEmail.isNotBlank()) { "Email is required." }
        require(password.length >= 6) { "Password must be at least 6 characters." }

        val existing = firestore.collection("users")
            .whereEqualTo("emailNormalized", normalizedEmail)
            .limit(1)
            .get()
            .await()
        require(existing.isEmpty) { "An account with this email already exists." }

        val doc = firestore.collection("users").document()
        val updated = UserProfile(
            id = doc.id,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            role = role
        )
        val data = updated.toMap() + mapOf(
            "emailNormalized" to normalizedEmail,
            "passwordHash" to password.sha256()
        )
        doc.set(data).await()
        saveProfile(updated)
        profile.value = updated
        return updated
    }

    suspend fun sendPasswordReset(email: String) {
        error("Password reset is not available without Firebase Authentication.")
    }

    suspend fun getProfile(uid: String = currentUserId.orEmpty()): UserProfile? {
        val current = profile.value
        if (uid.isBlank() || current?.id == uid) return current
        val doc = db?.collection("users")?.document(uid)?.get()?.await() ?: return null
        return if (doc.exists()) UserProfile.fromDocument(doc.id, doc.data ?: emptyMap()) else null
    }

    fun signOut() {
        saveProfile(null)
        profile.value = null
    }

    private fun savedUserId(): String? =
        context?.getSharedPreferences(SessionPreferences, Context.MODE_PRIVATE)
            ?.getString(SessionUserIdKey, null)

    private fun savedProfile(): UserProfile? {
        val prefs = context?.getSharedPreferences(SessionPreferences, Context.MODE_PRIVATE) ?: return null
        val uid = prefs.getString(SessionUserIdKey, null).orEmpty()
        if (uid.isBlank()) return null
        return UserProfile(
            id = uid,
            name = prefs.getString(SessionNameKey, "").orEmpty(),
            email = prefs.getString(SessionEmailKey, "").orEmpty(),
            phone = prefs.getString(SessionPhoneKey, "").orEmpty(),
            role = UserRole.from(prefs.getString(SessionRoleKey, null))
        )
    }

    private fun saveProfile(userProfile: UserProfile?) {
        context?.getSharedPreferences(SessionPreferences, Context.MODE_PRIVATE)
            ?.edit()
            ?.apply {
                if (userProfile == null) {
                    clear()
                } else {
                    putString(SessionUserIdKey, userProfile.id)
                    putString(SessionNameKey, userProfile.name)
                    putString(SessionEmailKey, userProfile.email)
                    putString(SessionPhoneKey, userProfile.phone)
                    putString(SessionRoleKey, userProfile.role.firestoreValue)
                }
            }
            ?.apply()
    }

    private fun String.normalizedEmail(): String = trim().lowercase()

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
