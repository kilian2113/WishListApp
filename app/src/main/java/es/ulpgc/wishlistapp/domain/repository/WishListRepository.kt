package es.ulpgc.wishlistapp.domain.repository

import es.ulpgc.wishlistapp.domain.model.WishList

interface WishListRepository {
    suspend fun loadWishLists(): Result<List<WishList>>
    suspend fun createWishList(name: String, description: String): Result<WishList>
    suspend fun deleteWishList(listId: String): Result<Unit>
}
