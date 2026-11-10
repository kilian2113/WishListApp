package es.ulpgc.wishlistapp.domain.model

data class User(
    val uid: String,
    val name: String,
    val username: String,
    val email: String,
    val birthDate: String
)
