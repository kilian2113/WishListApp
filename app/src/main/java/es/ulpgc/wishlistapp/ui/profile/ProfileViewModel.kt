package es.ulpgc.wishlistapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.model.EditableProfile
import es.ulpgc.wishlistapp.domain.model.UserProfile
import es.ulpgc.wishlistapp.domain.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableLiveData(ProfileState())
    val state: LiveData<ProfileState> = _state

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.RetryLoading -> loadProfile()
            ProfileEvent.StartEditing -> startEditing()
            ProfileEvent.CancelEditing -> cancelEditing()
            ProfileEvent.SaveChanges -> saveChanges()
            ProfileEvent.RegenerateFriendCode -> regenerateFriendCode()
            is ProfileEvent.UsernameChanged -> updateDraft {
                it.copy(username = event.value)
            }
            is ProfileEvent.NameChanged -> updateDraft {
                it.copy(name = event.value)
            }
            is ProfileEvent.DescriptionChanged -> updateDraft {
                it.copy(description = event.value)
            }
            is ProfileEvent.AvatarChanged -> updateDraft {
                it.copy(avatarId = event.value)
            }
            is ProfileEvent.HobbyToggled -> updateDraft {
                it.copy(hobbies = it.hobbies.toggle(event.value))
            }
            is ProfileEvent.ShirtSizeChanged -> updateDraft {
                it.copy(shirtSize = event.value)
            }
            is ProfileEvent.PantsSizeChanged -> updateDraft {
                it.copy(pantsSize = event.value)
            }
            is ProfileEvent.ShoeSizeChanged -> updateDraft {
                it.copy(shoeSize = event.value)
            }
        }
    }

    private fun loadProfile() {
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            profileRepository.loadOwnProfile()
                .onSuccess { profile ->
                    updateState {
                        it.copy(
                            profile = profile,
                            draft = null,
                            isLoading = false,
                            isEditing = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = exception.localizedMessage
                                ?: "No se pudo cargar el perfil."
                        )
                    }
                }
        }
    }

    private fun startEditing() {
        val profile = _state.value?.profile ?: return
        updateState {
            it.copy(
                draft = profile.toEditableProfile(),
                isEditing = true,
                nameError = null,
                usernameError = null,
                error = null
            )
        }
    }

    private fun cancelEditing() {
        updateState {
            it.copy(
                draft = null,
                isEditing = false,
                nameError = null,
                usernameError = null,
                error = null
            )
        }
    }

    private fun saveChanges() {
        val currentState = _state.value ?: return
        val draft = currentState.draft ?: return
        val nameError = if (draft.name.isBlank()) "El nombre es obligatorio." else null
        val usernameError = if (draft.username.isBlank()) {
            "El nombre de usuario es obligatorio."
        } else {
            null
        }

        if (nameError != null || usernameError != null) {
            updateState {
                it.copy(nameError = nameError, usernameError = usernameError)
            }
            return
        }

        val updatedDraft = draft.copy(
            name = draft.name.trim(),
            username = draft.username.trim(),
            description = draft.description.trim()
        )
        updateState { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            profileRepository.updateOwnProfile(updatedDraft)
                .onSuccess {
                    updateState { state ->
                        state.copy(
                            profile = state.profile?.withDetails(updatedDraft),
                            draft = null,
                            isEditing = false,
                            isSaving = false,
                            nameError = null,
                            usernameError = null,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isSaving = false,
                            error = exception.localizedMessage ?: "No se pudo guardar el perfil."
                        )
                    }
                }
        }
    }

    private fun regenerateFriendCode() {
        val currentState = _state.value ?: return
        if (currentState.isEditing || currentState.isRegeneratingFriendCode) return

        updateState { it.copy(isRegeneratingFriendCode = true, error = null) }
        viewModelScope.launch {
            profileRepository.regenerateFriendCode()
                .onSuccess { friendCode ->
                    updateState {
                        it.copy(
                            profile = it.profile?.copy(friendCode = friendCode),
                            isRegeneratingFriendCode = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isRegeneratingFriendCode = false,
                            error = exception.localizedMessage
                                ?: "No se pudo regenerar el código de amigo."
                        )
                    }
                }
        }
    }

    private fun updateDraft(transform: (EditableProfile) -> EditableProfile) {
        updateState { currentState ->
            val draft = currentState.draft
            if (!currentState.isEditing || draft == null) {
                currentState
            } else {
                currentState.copy(
                    draft = transform(draft),
                    nameError = null,
                    usernameError = null,
                    error = null
                )
            }
        }
    }

    private fun updateState(transform: (ProfileState) -> ProfileState) {
        val current = _state.value ?: ProfileState()
        _state.value = transform(current)
    }

    private fun List<String>.toggle(value: String): List<String> =
        if (value in this) filterNot { it == value } else this + value

    private fun UserProfile.toEditableProfile() = EditableProfile(
        username = username,
        name = name,
        description = description,
        hobbies = hobbies,
        avatarId = avatarId,
        shirtSize = shirtSize,
        pantsSize = pantsSize,
        shoeSize = shoeSize
    )

    private fun UserProfile.withDetails(details: EditableProfile) = copy(
        username = details.username,
        name = details.name,
        description = details.description,
        hobbies = details.hobbies,
        avatarId = details.avatarId,
        shirtSize = details.shirtSize,
        pantsSize = details.pantsSize,
        shoeSize = details.shoeSize
    )
}
