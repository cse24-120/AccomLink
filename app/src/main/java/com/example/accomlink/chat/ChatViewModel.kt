package com.example.accomlink.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accomlink.firebase.AuthRepository
import com.example.accomlink.firebase.ChatRepository
import com.example.accomlink.models.ChatMessage
import com.example.accomlink.models.ChatRoom
import com.example.accomlink.models.Listing
import com.example.accomlink.models.UserProfile
import com.example.accomlink.utils.ResultState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _activeRoom = MutableStateFlow<ChatRoom?>(null)
    val activeRoom: StateFlow<ChatRoom?> = _activeRoom.asStateFlow()

    val currentProfile: StateFlow<UserProfile?> = AuthRepository.profile.asStateFlow()

    private val _activePeerName = MutableStateFlow("")
    val activePeerName: StateFlow<String> = _activePeerName.asStateFlow()

    val rooms: StateFlow<List<ChatRoom>> = authRepository.authState()
        .flatMapLatest { userId -> chatRepository.roomsForUser(userId.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessage>> = _activeRoom
        .flatMapLatest { room -> room?.let { chatRepository.messages(it.id) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state.asStateFlow()

    fun openRoom(room: ChatRoom) {
        _activeRoom.value = room
        _activePeerName.value = room.peerNameFor(currentProfile.value?.id.orEmpty())
        _state.value = ResultState()
        resolvePeerName(room)
    }

    fun openListingRoom(listing: Listing) = viewModelScope.launch {
        val profile = authRepository.getProfile() ?: return@launch
        val studentId = if (profile.id == listing.landlordId) "" else profile.id
        val landlord = authRepository.getProfile(listing.landlordId)
        val room = ChatRoom(
            id = ChatRoom.idFor(listing.id, studentId, listing.landlordId),
            listingId = listing.id,
            listingTitle = listing.title,
            landlordId = listing.landlordId,
            landlordName = landlord.displayName(),
            studentId = studentId,
            studentName = if (studentId.isBlank()) "" else profile.displayName(),
            participantIds = listOf(studentId, listing.landlordId).filter { it.isNotBlank() }
        )
        openRoom(room)
    }

    fun closeRoom() {
        _activeRoom.value = null
        _activePeerName.value = ""
        _state.value = ResultState()
    }

    fun send(text: String) = viewModelScope.launch {
        _state.value = ResultState(loading = true)
        runCatching {
            val profile = authRepository.getProfile() ?: error("Please log in first.")
            val room = activeRoom.value ?: error("Open a chat room first.")
            chatRepository.sendMessage(room, text, profile)
        }.onSuccess {
            _state.value = ResultState()
        }.onFailure {
            _state.value = ResultState(error = it.message ?: "Could not send message")
        }
    }

    private fun resolvePeerName(room: ChatRoom) = viewModelScope.launch {
        val viewer = authRepository.getProfile() ?: return@launch
        val peerId = if (viewer.id == room.landlordId) room.studentId else room.landlordId
        if (peerId.isBlank()) return@launch
        val peer = authRepository.getProfile(peerId)
        if (_activeRoom.value?.id == room.id) {
            _activePeerName.value = peer.displayName().ifBlank { _activePeerName.value }
        }
    }

    private fun ChatRoom.peerNameFor(currentUserId: String): String =
        if (currentUserId == landlordId) studentName else landlordName

    private fun UserProfile?.displayName(): String =
        this?.let { profile -> profile.name.ifBlank { profile.email } }.orEmpty()
}
