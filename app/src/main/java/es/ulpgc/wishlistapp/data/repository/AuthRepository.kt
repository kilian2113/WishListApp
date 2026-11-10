package es.ulpgc.wishlistapp.data.repository

import es.ulpgc.wishlistapp.domain.model.User

interface AuthRepository {
    suspend fun signUpWithEmail(
        name: String,
        username: String,
        email: String,
        birthDate: String,
        password: String
    ): Result<User>
}
