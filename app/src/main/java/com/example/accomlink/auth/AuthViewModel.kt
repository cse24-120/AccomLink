package com.example.accomlink.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accomlink.firebase.AuthRepository
import com.example.accomlink.models.UserProfile
import com.example.accomlink.models.UserRole
import com.example.accomlink.utils.ResultState
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AuthViewModel(
    context: Context? = null,
    private val repository: AuthRepository = AuthRepository(context)
) : ViewModel() {
    private val _profile = MutableStateFlow(repository.cachedProfile())
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _authUserId = MutableStateFlow(repository.currentUserId)
    val authUserId: StateFlow<String?> = _authUserId.asStateFlow()

    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.restoreSession()
        }
        viewModelScope.launch {
            repository.authState().collect { uid ->
                _authUserId.value = uid
                _profile.value = uid?.let { repository.getProfile(it) }
            }
        }
    }

    fun login(email: String, password: String) = launchAuth {
        _profile.value = repository.login(email, password)
        _authUserId.value = _profile.value?.id
    }

    fun register(name: String, email: String, password: String, phone: String, role: UserRole) = launchAuth {
        _profile.value = repository.register(name, email, password, phone, role)
        _authUserId.value = _profile.value?.id
    }

    fun resetPassword(email: String) = launchAuth("Reset link sent when the email exists.") {
        repository.sendPasswordReset(email)
    }

    fun signOut() {
        repository.signOut()
        _profile.value = null
        _authUserId.value = repository.currentUserId
    }

    fun clearMessage() {
        _state.value = ResultState()
    }

    private fun launchAuth(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = ResultState(loading = true)
            runCatching { withTimeout(AuthTimeoutMillis) { block() } }
                .onSuccess { _state.value = ResultState(message = successMessage) }
                .onFailure { error ->
                    _state.value = ResultState(error = error.toAuthMessage())
                }
        }
    }

    private fun Throwable.toAuthMessage(): String =
        when (this) {
            is TimeoutCancellationException -> "Firebase is taking too long to respond. Check that Firestore is enabled and the rules are deployed for this project."
            else -> message ?: "Something went wrong"
        }

    private companion object {
        const val AuthTimeoutMillis = 15_000L
    }
}
