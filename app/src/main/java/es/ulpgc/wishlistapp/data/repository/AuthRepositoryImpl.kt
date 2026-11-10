package es.ulpgc.wishlistapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import es.ulpgc.wishlistapp.domain.model.User
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun signUpWithEmail(
        name: String,
        username: String,
        email: String,
        birthDate: String,
        password: String
    ): Result<User> {
        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        if (firebaseUser != null) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()
                            firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener {
                                    val user = User(
                                        uid = firebaseUser.uid,
                                        name = name,
                                        username = username,
                                        email = email,
                                        birthDate = birthDate
                                    )
                                    if (continuation.isActive) {
                                        continuation.resume(Result.success(user))
                                    }
                                }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Error al obtener información del usuario creado.")))
                            }
                        }
                    } else {
                        val exception = task.exception ?: Exception("Error al registrar el usuario en Firebase.")
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
                }
        }
    }
}
