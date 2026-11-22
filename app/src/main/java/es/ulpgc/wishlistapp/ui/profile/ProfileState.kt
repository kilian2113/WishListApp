package es.ulpgc.wishlistapp.ui.profile

import es.ulpgc.wishlistapp.domain.model.EditableProfile
import es.ulpgc.wishlistapp.domain.model.UserProfile

data class ProfileState(
    val profile: UserProfile? = null,
    val draft: EditableProfile? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isRegeneratingFriendCode: Boolean = false,
    val nameError: String? = null,
    val usernameError: String? = null,
    val error: String? = null
)

sealed interface ProfileEvent {
    data object RetryLoading : ProfileEvent
    data object StartEditing : ProfileEvent
    data object CancelEditing : ProfileEvent
    data object SaveChanges : ProfileEvent
    data object RegenerateFriendCode : ProfileEvent
    data class UsernameChanged(val value: String) : ProfileEvent
    data class NameChanged(val value: String) : ProfileEvent
    data class DescriptionChanged(val value: String) : ProfileEvent
    data class AvatarChanged(val value: String) : ProfileEvent
    data class HobbyToggled(val value: String) : ProfileEvent
    data class ShirtSizeChanged(val value: String) : ProfileEvent
    data class PantsSizeChanged(val value: String) : ProfileEvent
    data class ShoeSizeChanged(val value: String) : ProfileEvent
}
