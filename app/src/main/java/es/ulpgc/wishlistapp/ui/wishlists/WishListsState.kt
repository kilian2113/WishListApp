package es.ulpgc.wishlistapp.ui.wishlists

import es.ulpgc.wishlistapp.domain.model.WishList
import java.util.Locale

data class WishListsState(
    val lists: List<WishList> = emptyList(),
    val searchQuery: String = "",
    val isLoadingLists: Boolean = true,
    val listsError: String? = null,
    val isAddDialogVisible: Boolean = false,
    val newListName: String = "",
    val newListDescription: String = "",
    val newListNameError: String? = null,
    val operationError: String? = null,
    val successMessage: String? = null,
    val isCreatingList: Boolean = false,
    val listToDelete: WishList? = null,
    val isDeletingList: Boolean = false,
    val deleteListError: String? = null
) {
    val filteredLists: List<WishList>
        get() {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            return if (query.isBlank()) {
                lists
            } else {
                lists.filter { list ->
                    list.name.lowercase(Locale.ROOT).contains(query) ||
                        list.description.lowercase(Locale.ROOT).contains(query)
                }
            }
        }
}

sealed interface WishListsEvent {
    data class SearchQueryChanged(val value: String) : WishListsEvent
    data object RetryLoadingLists : WishListsEvent
    data object AddListClicked : WishListsEvent
    data object DismissAddDialog : WishListsEvent
    data class NameChanged(val value: String) : WishListsEvent
    data class DescriptionChanged(val value: String) : WishListsEvent
    data object ConfirmCreateList : WishListsEvent
    data object SuccessMessageShown : WishListsEvent
    data class DeleteListClicked(val wishList: WishList) : WishListsEvent
    data object DismissDeleteDialog : WishListsEvent
    data object ConfirmDeleteList : WishListsEvent
}
