package com.example.accomlink.listings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accomlink.firebase.AuthRepository
import com.example.accomlink.firebase.FavoriteRepository
import com.example.accomlink.firebase.ListingRepository
import com.example.accomlink.firebase.PreferenceRepository
import com.example.accomlink.firebase.ReservationRepository
import com.example.accomlink.models.Listing
import com.example.accomlink.models.ListingFilter
import com.example.accomlink.models.SavedPreferences
import com.example.accomlink.utils.ResultState
import com.example.accomlink.models.Reservation
import com.example.accomlink.utils.NotificationHelper
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
class ListingsViewModel(
    private val listings: ListingRepository = ListingRepository(),
    private val favorites: FavoriteRepository = FavoriteRepository(listingRepository = listings),
    private val auth: AuthRepository = AuthRepository(),
    private val preferences: PreferenceRepository = PreferenceRepository(),
    private val reservations: ReservationRepository = ReservationRepository()
) : ViewModel() {
    private val _filter = MutableStateFlow(ListingFilter())
    val filter: StateFlow<ListingFilter> = _filter.asStateFlow()

    val visibleListings: StateFlow<List<Listing>> = _filter
        .flatMapLatest { listings.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allListings: StateFlow<List<Listing>> = listings.allListings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landlordListings: StateFlow<List<Listing>> = auth.authState()
        .flatMapLatest { uid -> if (uid.isNullOrBlank()) flowOf(emptyList()) else listings.landlordListings(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = auth.authState()
        .flatMapLatest { uid -> if (uid.isNullOrBlank()) flowOf(emptySet()) else favorites.favoriteIds(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteListings: StateFlow<List<Listing>> = auth.authState()
        .flatMapLatest { uid -> if (uid.isNullOrBlank()) flowOf(emptyList()) else favorites.favoriteListings(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myReservations: StateFlow<List<Reservation>> = auth.authState()
        .flatMapLatest { uid -> if (uid.isNullOrBlank()) flowOf(emptyList()) else reservations.reservationsForStudent(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landlordReservations: StateFlow<List<Reservation>> = auth.authState()
        .flatMapLatest { uid -> if (uid.isNullOrBlank()) flowOf(emptyList()) else reservations.reservationsForLandlord(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPreferences: StateFlow<SavedPreferences?> = auth.authState()
        .flatMapLatest { uid -> if (uid.isNullOrBlank()) flowOf(null) else preferences.preferences(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state.asStateFlow()

    fun updateFilter(filter: ListingFilter) {
        _filter.value = filter
    }

    fun listing(listingId: String) = listings.listing(listingId)

    fun toggleFavorite(listingId: String) = viewModelScope.launch {
        val uid = auth.currentUserId ?: return@launch
        favorites.toggleFavorite(uid, listingId, listingId in favoriteIds.value)
    }

    fun savePreferences(filter: ListingFilter, enabled: Boolean) = viewModelScope.launch {
        _state.value = ResultState(loading = true)
        runCatching {
            val uid = auth.currentUserId.orEmpty()
            preferences.savePreferences(uid, filter, enabled)
        }.onSuccess {
            _state.value = ResultState(message = "Preferences saved")
        }.onFailure {
            _state.value = ResultState(error = it.message ?: "Could not save preferences")
        }
    }

    fun saveListing(context: Context, listing: Listing, imageUris: List<Uri>, editing: Boolean, onSaved: () -> Unit = {}) = viewModelScope.launch {
        _state.value = ResultState(loading = true)
        runCatching {
            val ownerId = auth.currentUserId.orEmpty()
            require(ownerId.isNotBlank()) { "Please log in before saving a listing." }
            val phone = auth.getProfile()?.phone.orEmpty()
            val payload = listing.copy(
                landlordId = listing.landlordId.ifBlank { ownerId },
                landlordPhone = listing.landlordPhone.ifBlank { phone }
            )
            val savedId = if (editing) {
                listings.updateListing(context.applicationContext, payload, imageUris)
                payload.id
            } else {
                listings.addListing(context.applicationContext, payload, imageUris)
            }
            payload.copy(id = savedId)
        }.onSuccess {
            _state.value = ResultState(message = "Listing saved")
            val saved = savedPreferences.value
            if (saved?.alertsEnabled == true && NotificationHelper.matches(saved.filter, it)) {
                NotificationHelper.notifyMatch(context.applicationContext, it)
            }
            onSaved()
        }.onFailure {
            _state.value = ResultState(error = it.message ?: "Could not save listing")
        }
    }

    fun deleteListing(listingId: String) = viewModelScope.launch {
        runCatching { listings.deleteListing(listingId) }
    }

    fun setOccupied(listingId: String, occupied: Boolean) = viewModelScope.launch {
        runCatching { listings.setOccupied(listingId, occupied) }
    }

    fun clearState() {
        _state.value = ResultState()
    }
}
