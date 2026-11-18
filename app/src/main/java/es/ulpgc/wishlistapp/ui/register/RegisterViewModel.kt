package es.ulpgc.wishlistapp.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData(RegisterState())
    val state: LiveData<RegisterState> = _state

    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

    fun onNameChanged(name: String) {
        updateState { it.copy(name = name, nameError = null) }
    }

    fun onUsernameChanged(username: String) {
        updateState { it.copy(username = username, usernameError = null) }
    }

    fun onEmailChanged(email: String) {
        updateState { it.copy(email = email, emailError = null) }
    }

    fun onBirthDateChanged(birthDate: String) {
        updateState { it.copy(birthDate = birthDate, birthDateError = null) }
    }

    fun onPasswordChanged(password: String) {
        updateState { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        updateState { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun onTermsAcceptedChanged(accepted: Boolean) {
        updateState { it.copy(termsAccepted = accepted, termsError = null) }
    }

    fun togglePasswordVisibility() {
        updateState { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        updateState { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    private fun validateBirthDate(birthDateStr: String): String? {
        val trimmed = birthDateStr.trim()
        if (trimmed.isBlank()) {
            return "La fecha de nacimiento es obligatoria"
        }

        val dateRegex = Regex("^\\d{2}/\\d{2}/\\d{4}\$")
        if (!dateRegex.matches(trimmed)) {
            return "Introduce una fecha válida (DD/MM/AAAA)"
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }

        val birthDate: Date = try {
            sdf.parse(trimmed) ?: return "Fecha de nacimiento no válida"
        } catch (_: Exception) {
            return "Fecha de nacimiento no válida"
        }

        val birthCalendar = Calendar.getInstance().apply {
            time = birthDate
        }
        val today = Calendar.getInstance()

        if (birthCalendar.after(today)) {
            return "La fecha de nacimiento no puede ser futura"
        }

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        if (age < 18) {
            return "Debes ser mayor de 18 años para registrarte"
        }

        return null
    }

    fun register() {
        val currentState = _state.value ?: RegisterState()
        var hasError = false

        var nameErr: String? = null
        var usernameErr: String? = null
        var emailErr: String? = null
        var birthDateErr: String? = null
        var passErr: String? = null
        var confirmErr: String? = null
        var termsErr: String? = null

        if (currentState.name.isBlank()) {
            nameErr = "El nombre es obligatorio"
            hasError = true
        }

        if (currentState.username.isBlank()) {
            usernameErr = "El nombre de usuario es obligatorio"
            hasError = true
        }

        if (currentState.email.isBlank()) {
            emailErr = "El correo electrónico es obligatorio"
            hasError = true
        } else if (!emailPattern.matches(currentState.email.trim())) {
            emailErr = "El correo electrónico no tiene un formato válido"
            hasError = true
        }

        val bDateError = validateBirthDate(currentState.birthDate)
        if (bDateError != null) {
            birthDateErr = bDateError
            hasError = true
        }

        if (currentState.password.isBlank()) {
            passErr = "La contraseña es obligatoria"
            hasError = true
        } else if (currentState.password.length < 6) {
            passErr = "La contraseña debe tener al menos 6 caracteres"
            hasError = true
        }

        if (currentState.confirmPassword.isBlank()) {
            confirmErr = "Debes confirmar la contraseña"
            hasError = true
        } else if (currentState.password != currentState.confirmPassword) {
            confirmErr = "Las contraseñas no coinciden"
            hasError = true
        }

        if (!currentState.termsAccepted) {
            termsErr = "Debes aceptar los Términos y Condiciones"
            hasError = true
        }

        if (hasError) {
            updateState {
                it.copy(
                    nameError = nameErr,
                    usernameError = usernameErr,
                    emailError = emailErr,
                    birthDateError = birthDateErr,
                    passwordError = passErr,
                    confirmPasswordError = confirmErr,
                    termsError = termsErr,
                    error = null
                )
            }
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(
                name = currentState.name.trim(),
                username = currentState.username.trim(),
                email = currentState.email.trim(),
                birthDate = currentState.birthDate.trim(),
                password = currentState.password
            )

            result.onSuccess { user ->
                updateState {
                    it.copy(
                        isLoading = false,
                        isRegistered = true,
                        registeredUserName = user.name
                    )
                }
            }.onFailure { exception ->
                updateState {
                    it.copy(
                        isLoading = false,
                        error = exception.localizedMessage ?: "Error al procesar el registro"
                    )
                }
            }
        }
    }

    private fun updateState(transform: (RegisterState) -> RegisterState) {
        val current = _state.value ?: RegisterState()
        _state.value = transform(current)
    }
}
