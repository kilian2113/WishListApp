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

    fun onEvent(event: FriendsEvent) {
        when (event) {
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

    private fun updateState(transform: (FriendsState) -> FriendsState) {
        val current = _state.value ?: FriendsState()
        _state.value = transform(current)
    }
}
