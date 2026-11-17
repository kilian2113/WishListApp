package es.ulpgc.wishlistapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import es.ulpgc.wishlistapp.domain.model.User
import es.ulpgc.wishlistapp.domain.repository.AuthRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
                                    val userMap = hashMapOf(
                                        "uid" to firebaseUser.uid,
                                        "name" to name,
                                        "username" to username,
                                        "email" to email,
                                        "birthDate" to birthDate
                                    )

                                    // Guardar los datos en la tabla/colección "users" de Firestore
                                    firestore.collection("users")
                                        .document(firebaseUser.uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
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
                                        .addOnFailureListener { exception ->
                                            if (continuation.isActive) {
                                                continuation.resume(
                                                    Result.failure(
                                                        exception ?: Exception("Error al guardar los datos del usuario en la base de datos.")
                                                    )
                                                )
                                            }
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
