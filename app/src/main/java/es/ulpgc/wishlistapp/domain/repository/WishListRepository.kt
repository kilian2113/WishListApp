package es.ulpgc.wishlistapp.domain.repository

import es.ulpgc.wishlistapp.domain.model.Gift
import es.ulpgc.wishlistapp.domain.model.WishList

interface WishListRepository {
    suspend fun loadWishLists(): Result<List<WishList>>
    suspend fun createWishList(name: String, description: String): Result<WishList>
    suspend fun deleteWishList(listId: String): Result<Unit>

    suspend fun getWishList(listId: String): Result<WishList>
    suspend fun updateWishList(listId: String, name: String, description: String, eventDate: String): Result<Unit>
    suspend fun loadGifts(listId: String): Result<List<Gift>>
    suspend fun createGift(listId: String, gift: Gift): Result<Gift>
    suspend fun updateGift(listId: String, gift: Gift): Result<Unit>
    suspend fun deleteGift(listId: String, giftId: String): Result<Unit>
}
