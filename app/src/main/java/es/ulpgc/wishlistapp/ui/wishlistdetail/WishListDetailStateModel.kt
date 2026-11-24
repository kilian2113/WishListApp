package es.ulpgc.wishlistapp.ui.wishlistdetail

import es.ulpgc.wishlistapp.domain.model.Gift
import es.ulpgc.wishlistapp.domain.model.GiftPriority
import es.ulpgc.wishlistapp.domain.model.WishList

data class WishListDetailState(
    val wishList: WishList? = null,
    val gifts: List<Gift> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,

    // Header edit dialog
    val isEditHeaderDialogVisible: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val editEventDate: String = "",
    val editNameError: String? = null,
    val editEventDateError: String? = null,
    val isSavingHeader: Boolean = false,

    // Gift dialog (Create or Edit)
    val isGiftDialogVisible: Boolean = false,
    val editingGift: Gift? = null,
    val giftName: String = "",
    val giftPrice: String = "",
    val giftPriority: GiftPriority = GiftPriority.MEDIUM,
    val giftDescription: String = "",
    val giftProductUrl: String = "",
    val giftImageUrl: String = "",
    val giftNameError: String? = null,
    val giftPriceError: String? = null,
    val giftUrlError: String? = null,
    val isSavingGift: Boolean = false,

    // Delete Gift dialog
    val giftToDelete: Gift? = null,
    val isDeletingGift: Boolean = false
)
