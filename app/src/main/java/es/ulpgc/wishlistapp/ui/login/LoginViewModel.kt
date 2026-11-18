package es.ulpgc.wishlistapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData(LoginState())
    val state: LiveData<LoginState> = _state

    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

    fun onEmailChanged(email: String) {
        updateState { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        updateState { it.copy(password = password, passwordError = null) }
    }

    fun togglePasswordVisibility() {
        updateState { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val currentState = _state.value ?: LoginState()
        var hasError = false

        var emailErr: String? = null
        var passErr: String? = null

        if (currentState.email.isBlank()) {
            emailErr = "El correo electrónico es obligatorio"
            hasError = true
        } else if (!emailPattern.matches(currentState.email.trim())) {
            emailErr = "El correo electrónico no tiene un formato válido"
            hasError = true
        }

        if (currentState.password.isBlank()) {
            passErr = "La contraseña es obligatoria"
            hasError = true
        }

        if (hasError) {
            updateState {
                it.copy(
                    emailError = emailErr,
                    passwordError = passErr,
                    error = null
                )
            }
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithEmail(
                email = currentState.email.trim(),
                password = currentState.password
            )

            result.onSuccess { user ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        loggedInUserName = if (user.name.isNotBlank()) user.name else user.email
                    )
                }
            }.onFailure { exception ->
                val errorMessage = when {
                    exception.message?.contains("The password is invalid") == true -> "Contraseña incorrecta"
                    exception.message?.contains("There is no user record") == true -> "No existe ninguna cuenta asociada a este correo"
                    else -> exception.localizedMessage ?: "Error al iniciar sesión. Revisa tus credenciales."
                }
                updateState {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    private fun updateState(transform: (LoginState) -> LoginState) {
        val current = _state.value ?: LoginState()
        _state.value = transform(current)
    }
}
