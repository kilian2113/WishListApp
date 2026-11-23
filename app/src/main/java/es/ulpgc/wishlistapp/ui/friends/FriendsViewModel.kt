package es.ulpgc.wishlistapp.ui.friends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.model.FriendCodePolicy
import es.ulpgc.wishlistapp.domain.repository.CannotAddYourselfException
import es.ulpgc.wishlistapp.domain.repository.FriendAlreadyAddedException
import es.ulpgc.wishlistapp.domain.repository.FriendCodeNotFoundException
import es.ulpgc.wishlistapp.domain.repository.FriendRepository
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _state = MutableLiveData(FriendsState())
    val state: LiveData<FriendsState> = _state

    init {
        loadFriends()
    }

    fun onEvent(event: FriendsEvent) {
        when (event) {
            is FriendsEvent.SearchQueryChanged -> updateState {
                it.copy(searchQuery = event.value)
            }
            FriendsEvent.RetryLoadingFriends -> loadFriends()
            FriendsEvent.AddFriendClicked -> showAddDialog()
            FriendsEvent.DismissAddDialog -> dismissAddDialog()
            FriendsEvent.ConfirmAddFriend -> addFriend()
            FriendsEvent.SuccessMessageShown -> updateState {
                it.copy(successMessage = null)
            }
            is FriendsEvent.FriendCodeChanged -> updateState {
                it.copy(
                    friendCode = FriendCodePolicy.normalize(event.value),
                    friendCodeError = null,
                    operationError = null
                )
            }
            is FriendsEvent.RemoveFriendClicked -> updateState {
                it.copy(friendToRemove = event.friend, removeFriendError = null)
            }
            FriendsEvent.DismissRemoveFriendDialog -> updateState {
                if (it.isRemovingFriend) {
                    it
                } else {
                    it.copy(friendToRemove = null, removeFriendError = null)
                }
            }
            FriendsEvent.ConfirmRemoveFriend -> removeFriend()
        }
    }

    private fun showAddDialog() {
        updateState {
            it.copy(
                isAddDialogVisible = true,
                friendCode = "",
                friendCodeError = null,
                operationError = null,
                successMessage = null
            )
        }
    }

    private fun loadFriends() {
        updateState {
            it.copy(isLoadingFriends = true, friendsError = null)
        }
        viewModelScope.launch {
            friendRepository.loadFriends()
                .onSuccess { friends ->
                    updateState {
                        it.copy(
                            friends = friends,
                            isLoadingFriends = false,
                            friendsError = null
                        )
                    }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isLoadingFriends = false,
                            friendsError = "No se pudo cargar la lista de amigos."
                        )
                    }
                }
        }
    }

    private fun dismissAddDialog() {
        updateState {
            if (it.isAddingFriend) {
                it
            } else {
                it.copy(
                    isAddDialogVisible = false,
                    friendCode = "",
                    friendCodeError = null,
                    operationError = null
                )
            }
        }
    }

    private fun addFriend() {
        val code = _state.value?.friendCode.orEmpty()
        val codeError = when {
            code.isBlank() -> "Introduce el código de amigo."
            !FriendCodePolicy.isValid(code) -> "El código debe tener 8 caracteres válidos."
            else -> null
        }
        if (codeError != null) {
            updateState { it.copy(friendCodeError = codeError, operationError = null) }
            return
        }

        updateState {
            it.copy(
                isAddingFriend = true,
                friendCodeError = null,
                operationError = null
            )
        }
        viewModelScope.launch {
            friendRepository.addFriendByCode(code)
                .onSuccess {
                    updateState {
                        it.copy(
                            isAddDialogVisible = false,
                            friendCode = "",
                            isAddingFriend = false,
                            successMessage = "Amigo añadido correctamente."
                        )
                    }
                    loadFriends()
                }
                .onFailure { exception ->
                    updateState {
                        val fieldError = when (exception) {
                            is FriendCodeNotFoundException,
                            is FriendAlreadyAddedException,
                            is CannotAddYourselfException -> exception.localizedMessage
                            else -> null
                        }
                        it.copy(
                            isAddingFriend = false,
                            friendCodeError = fieldError,
                            operationError = if (fieldError == null) {
                                "No se pudo añadir al amigo. Inténtalo de nuevo más tarde."
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    private fun removeFriend() {
        val friend = _state.value?.friendToRemove ?: return
        updateState {
            it.copy(isRemovingFriend = true, removeFriendError = null)
        }
        viewModelScope.launch {
            friendRepository.removeFriend(friend.uid)
                .onSuccess {
                    updateState {
                        it.copy(
                            friends = it.friends.filterNot { currentFriend ->
                                currentFriend.uid == friend.uid
                            },
                            friendToRemove = null,
                            isRemovingFriend = false,
                            removeFriendError = null,
                            successMessage = "Amigo eliminado correctamente."
                        )
                    }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isRemovingFriend = false,
                            removeFriendError = "No se pudo eliminar a ${friend.name}. Inténtalo de nuevo."
                        )
                    }
                }
        }
    }

    private fun updateState(transform: (FriendsState) -> FriendsState) {
        val current = _state.value ?: FriendsState()
        _state.value = transform(current)
    }
}
