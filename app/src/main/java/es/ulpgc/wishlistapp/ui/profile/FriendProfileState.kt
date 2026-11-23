package es.ulpgc.wishlistapp.ui.profile

import es.ulpgc.wishlistapp.domain.model.UserProfile

data class FriendProfileState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
