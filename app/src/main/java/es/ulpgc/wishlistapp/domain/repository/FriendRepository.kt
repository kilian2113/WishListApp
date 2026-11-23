package es.ulpgc.wishlistapp.domain.repository

interface FriendRepository {
    suspend fun addFriendByCode(code: String): Result<Unit>
}
