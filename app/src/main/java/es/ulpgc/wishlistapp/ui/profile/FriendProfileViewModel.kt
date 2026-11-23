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

    private val _state = MutableLiveData(ProfileState())
    val state: LiveData<ProfileState> = _state
    private var loadJob: Job? = null

    fun loadProfile(uid: String) {
        loadJob?.cancel()
        _state.value = ProfileState(isLoading = true)
        loadJob = viewModelScope.launch {
            profileRepository.loadProfile(uid)
                .onSuccess { profile ->
                    _state.value = ProfileState(
                        profile = profile,
                        isLoading = false
                    )
                }
                .onFailure { exception ->
                    _state.value = ProfileState(
                        isLoading = false,
                        error = exception.localizedMessage
                            ?: "No se pudo cargar el perfil del amigo."
                    )
                }
        }
    }
}
