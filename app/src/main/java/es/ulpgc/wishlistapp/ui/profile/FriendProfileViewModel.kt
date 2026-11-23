package es.ulpgc.wishlistapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FriendProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableLiveData(FriendProfileState())
    val state: LiveData<FriendProfileState> = _state
    private var loadJob: Job? = null

    fun loadFriendProfile(uid: String) {
        loadJob?.cancel()
        _state.value = FriendProfileState(isLoading = true)
        loadJob = viewModelScope.launch {
            profileRepository.loadFriendProfile(uid)
                .onSuccess { profile ->
                    _state.value = FriendProfileState(
                        profile = profile,
                        isLoading = false
                    )
                }
                .onFailure { exception ->
                    _state.value = FriendProfileState(
                        isLoading = false,
                        error = exception.localizedMessage
                            ?: "No se pudo cargar el perfil del amigo."
                    )
                }
        }
    }
}
