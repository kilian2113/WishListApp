package es.ulpgc.wishlistapp.ui.friends

data class FriendsState(
    val isAddDialogVisible: Boolean = false,
    val friendCode: String = "",
    val friendCodeError: String? = null,
    val operationError: String? = null,
    val successMessage: String? = null,
    val isAddingFriend: Boolean = false
)

sealed interface FriendsEvent {
    data object AddFriendClicked : FriendsEvent
    data object DismissAddDialog : FriendsEvent
    data object ConfirmAddFriend : FriendsEvent
    data object SuccessMessageShown : FriendsEvent
    data class FriendCodeChanged(val value: String) : FriendsEvent
}
