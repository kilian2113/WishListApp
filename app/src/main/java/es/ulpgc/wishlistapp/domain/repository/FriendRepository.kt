package es.ulpgc.wishlistapp.domain.repository

import es.ulpgc.wishlistapp.domain.model.Friend

interface FriendRepository {
    suspend fun loadFriends(): Result<List<Friend>>

    suspend fun addFriendByCode(code: String): Result<Unit>
}
