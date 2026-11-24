package es.ulpgc.wishlistapp.ui.wishlistdetail

import es.ulpgc.wishlistapp.domain.model.Gift
import es.ulpgc.wishlistapp.domain.model.GiftPriority

sealed interface WishListDetailEvent {
    data object RetryLoading : WishListDetailEvent
    data object EditHeaderClicked : WishListDetailEvent
    data object DismissEditHeaderDialog : WishListDetailEvent
    data class EditNameChanged(val value: String) : WishListDetailEvent
    data class EditDescriptionChanged(val value: String) : WishListDetailEvent
    data class EditEventDateChanged(val value: String) : WishListDetailEvent
    data object ConfirmSaveHeader : WishListDetailEvent

    data object AddGiftClicked : WishListDetailEvent
    data class EditGiftClicked(val gift: Gift) : WishListDetailEvent
    data object DismissGiftDialog : WishListDetailEvent
    data class GiftNameChanged(val value: String) : WishListDetailEvent
    data class GiftPriceChanged(val value: String) : WishListDetailEvent
    data class GiftPriorityChanged(val value: GiftPriority) : WishListDetailEvent
    data class GiftDescriptionChanged(val value: String) : WishListDetailEvent
    data class GiftProductUrlChanged(val value: String) : WishListDetailEvent
    data class GiftImageUrlChanged(val value: String) : WishListDetailEvent
    data object ConfirmSaveGift : WishListDetailEvent

    data class DeleteGiftClicked(val gift: Gift) : WishListDetailEvent
    data object DismissDeleteGiftDialog : WishListDetailEvent
    data object ConfirmDeleteGift : WishListDetailEvent

    data object SuccessMessageShown : WishListDetailEvent
}
