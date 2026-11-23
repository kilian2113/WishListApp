package es.ulpgc.wishlistapp.domain.repository

import es.ulpgc.wishlistapp.domain.model.EditableProfile
import es.ulpgc.wishlistapp.domain.model.UserProfile

interface ProfileRepository {
    suspend fun loadOwnProfile(): Result<UserProfile>

    suspend fun loadFriendProfile(friendUid: String): Result<UserProfile>

    suspend fun updateOwnProfile(profile: EditableProfile): Result<Unit>

    suspend fun regenerateFriendCode(): Result<String>
}
