package es.ulpgc.wishlistapp.ui.friends

import es.ulpgc.wishlistapp.domain.model.Friend
import java.util.Locale

data class FriendsState(
    val friends: List<Friend> = emptyList(),
    val searchQuery: String = "",
    val isLoadingFriends: Boolean = true,
    val friendsError: String? = null,
    val isAddDialogVisible: Boolean = false,
    val friendCode: String = "",
    val friendCodeError: String? = null,
    val operationError: String? = null,
    val successMessage: String? = null,
    val isAddingFriend: Boolean = false
) {
    val filteredFriends: List<Friend>
        get() {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            return if (query.isBlank()) {
                friends
            } else {
                friends.filter { friend ->
                    friend.name.lowercase(Locale.ROOT).contains(query) ||
                        friend.username.lowercase(Locale.ROOT).contains(query)
                }
            }
        }
}

sealed interface FriendsEvent {
    data class SearchQueryChanged(val value: String) : FriendsEvent
    data object RetryLoadingFriends : FriendsEvent
    data object AddFriendClicked : FriendsEvent
    data object DismissAddDialog : FriendsEvent
    data object ConfirmAddFriend : FriendsEvent
    data object SuccessMessageShown : FriendsEvent
    data class FriendCodeChanged(val value: String) : FriendsEvent
}
