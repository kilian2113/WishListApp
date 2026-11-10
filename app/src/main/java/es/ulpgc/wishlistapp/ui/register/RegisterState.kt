package es.ulpgc.wishlistapp.ui.register

data class RegisterState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val birthDate: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val nameError: String? = null,
    val usernameError: String? = null,
    val emailError: String? = null,
    val birthDateError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val registeredUserName: String? = null,
    val error: String? = null
)
