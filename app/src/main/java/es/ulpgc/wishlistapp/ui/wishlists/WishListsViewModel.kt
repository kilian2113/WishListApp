package es.ulpgc.wishlistapp.ui.wishlists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.repository.WishListRepository
import kotlinx.coroutines.launch

class WishListsViewModel(
    private val wishListRepository: WishListRepository
) : ViewModel() {

    private val _state = MutableLiveData(WishListsState())
    val state: LiveData<WishListsState> = _state

    init {
        loadLists()
    }

    fun onEvent(event: WishListsEvent) {
        when (event) {
            is WishListsEvent.SearchQueryChanged -> updateState {
                it.copy(searchQuery = event.value)
            }
            WishListsEvent.RetryLoadingLists -> loadLists()
            WishListsEvent.AddListClicked -> showAddDialog()
            WishListsEvent.DismissAddDialog -> dismissAddDialog()
            is WishListsEvent.NameChanged -> updateState {
                it.copy(newListName = event.value, newListNameError = null, operationError = null)
            }
            is WishListsEvent.DescriptionChanged -> updateState {
                it.copy(newListDescription = event.value)
            }
            WishListsEvent.ConfirmCreateList -> createList()
            WishListsEvent.SuccessMessageShown -> updateState {
                it.copy(successMessage = null)
            }
            is WishListsEvent.DeleteListClicked -> updateState {
                it.copy(listToDelete = event.wishList, deleteListError = null)
            }
            WishListsEvent.DismissDeleteDialog -> updateState {
                if (it.isDeletingList) it else it.copy(listToDelete = null, deleteListError = null)
            }
            WishListsEvent.ConfirmDeleteList -> deleteList()
        }
    }

    private fun loadLists() {
        updateState { it.copy(isLoadingLists = true, listsError = null) }
        viewModelScope.launch {
            wishListRepository.loadWishLists()
                .onSuccess { lists ->
                    updateState {
                        it.copy(
                            lists = lists,
                            isLoadingLists = false,
                            listsError = null
                        )
                    }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isLoadingLists = false,
                            listsError = "No se pudieron cargar las listas de deseos."
                        )
                    }
                }
        }
    }

    private fun showAddDialog() {
        updateState {
            it.copy(
                isAddDialogVisible = true,
                newListName = "",
                newListDescription = "",
                newListNameError = null,
                operationError = null,
                successMessage = null
            )
        }
    }

    private fun dismissAddDialog() {
        updateState {
            if (it.isCreatingList) {
                it
            } else {
                it.copy(
                    isAddDialogVisible = false,
                    newListName = "",
                    newListDescription = "",
                    newListNameError = null,
                    operationError = null
                )
            }
        }
    }

    private fun createList() {
        val name = _state.value?.newListName.orEmpty().trim()
        val description = _state.value?.newListDescription.orEmpty().trim()

        if (name.isBlank()) {
            updateState { it.copy(newListNameError = "El nombre de la lista es obligatorio.") }
            return
        }

        updateState {
            it.copy(
                isCreatingList = true,
                newListNameError = null,
                operationError = null
            )
        }

        viewModelScope.launch {
            wishListRepository.createWishList(name, description)
                .onSuccess {
                    updateState {
                        it.copy(
                            isAddDialogVisible = false,
                            newListName = "",
                            newListDescription = "",
                            isCreatingList = false,
                            successMessage = "Lista creada correctamente."
                        )
                    }
                    loadLists()
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isCreatingList = false,
                            operationError = exception.localizedMessage ?: "No se pudo crear la lista."
                        )
                    }
                }
        }
    }

    private fun deleteList() {
        val list = _state.value?.listToDelete ?: return
        updateState { it.copy(isDeletingList = true, deleteListError = null) }
        viewModelScope.launch {
            wishListRepository.deleteWishList(list.id)
                .onSuccess {
                    updateState {
                        it.copy(
                            lists = it.lists.filterNot { current -> current.id == list.id },
                            listToDelete = null,
                            isDeletingList = false,
                            deleteListError = null,
                            successMessage = "Lista eliminada correctamente."
                        )
                    }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isDeletingList = false,
                            deleteListError = "No se pudo eliminar la lista. Inténtalo de nuevo."
                        )
                    }
                }
        }
    }

    private fun updateState(transform: (WishListsState) -> WishListsState) {
        val current = _state.value ?: WishListsState()
        _state.value = transform(current)
    }
}
