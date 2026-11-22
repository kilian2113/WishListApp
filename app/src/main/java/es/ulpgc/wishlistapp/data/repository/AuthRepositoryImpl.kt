package es.ulpgc.wishlistapp.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import es.ulpgc.wishlistapp.domain.model.User
import es.ulpgc.wishlistapp.domain.model.UsernamePolicy
import es.ulpgc.wishlistapp.domain.repository.AuthRepository
import es.ulpgc.wishlistapp.domain.repository.BackendPermissionDeniedException
import es.ulpgc.wishlistapp.domain.repository.UsernameAlreadyTakenException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val usernameRegistry = UsernameRegistry(firestore)

    override suspend fun signUpWithEmail(
        name: String,
        username: String,
        email: String,
        birthDate: String,
        password: String
    ): Result<User> {
        val normalizedUsername = UsernamePolicy.normalize(username)
        if (!UsernamePolicy.isValid(normalizedUsername)) {
            return Result.failure(
                IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 20 caracteres: letras, números o guion bajo."
                )
            )
        }

        var createdUser: FirebaseUser? = null
        return try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("No se pudo obtener el usuario creado."))
            createdUser = firebaseUser

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val userReference = firestore.collection("users").document(firebaseUser.uid)
            val registrationResult = firestore.runTransaction { transaction ->
                if (transaction.get(userReference).exists()) {
                    return@runTransaction RegistrationResult.ProfileAlreadyExists
                }

                if (usernameRegistry.reserve(
                        transaction = transaction,
                        uid = firebaseUser.uid,
                        currentUsername = null,
                        newUsername = normalizedUsername
                    ) == UsernameRegistry.ReservationResult.AlreadyTaken
                ) {
                    return@runTransaction RegistrationResult.UsernameTaken
                }

                transaction.set(
                    userReference,
                    mapOf(
                        "uid" to firebaseUser.uid,
                        "name" to name,
                        "username" to normalizedUsername,
                        "email" to email,
                        "birthDate" to birthDate
                    )
                )
                RegistrationResult.Created
            }.await()

            when (registrationResult) {
                RegistrationResult.Created -> Result.success(
                    User(
                        uid = firebaseUser.uid,
                        name = name,
                        username = normalizedUsername,
                        email = email,
                        birthDate = birthDate
                    )
                )
                RegistrationResult.UsernameTaken -> {
                    throw UsernameAlreadyTakenException()
                }
                RegistrationResult.ProfileAlreadyExists -> {
                    throw IllegalStateException("Ya existe un perfil para la cuenta creada.")
                }
            }
        } catch (exception: CancellationException) {
            createdUser?.let { user ->
                withContext(NonCancellable) {
                    runCatching { user.delete().await() }
                        .onFailure(exception::addSuppressed)
                }
            }
            throw exception
        } catch (exception: Exception) {
            val failure = if (
                exception is FirebaseFirestoreException &&
                exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                Log.e(TAG, "Firestore denied account registration.", exception)
                BackendPermissionDeniedException(exception)
            } else {
                exception
            }
            val user = createdUser
            if (user != null) {
                try {
                    user.delete().await()
                } catch (cleanupException: Exception) {
                    return Result.failure(
                        IllegalStateException(
                            "No se pudo completar el registro ni eliminar la cuenta recién creada.",
                            cleanupException
                        ).also { it.addSuppressed(failure) }
                    )
                }
            }
            Result.failure(failure)
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<User> {
        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        if (firebaseUser != null) {
                            firestore.collection("users")
                                .document(firebaseUser.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    val name = document.getString("name") ?: firebaseUser.displayName ?: ""
                                    val username = document.getString("username") ?: ""
                                    val birthDate = document.getString("birthDate") ?: ""
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
                                .addOnFailureListener {
                                    val user = User(
                                        uid = firebaseUser.uid,
                                        name = firebaseUser.displayName ?: "",
                                        username = "",
                                        email = email,
                                        birthDate = ""
                                    )
                                    if (continuation.isActive) {
                                        continuation.resume(Result.success(user))
                                    }
                                }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Error al obtener información de inicio de sesión.")))
                            }
                        }
                    } else {
                        val exception = task.exception ?: Exception("Error de inicio de sesión. Revisa tus credenciales.")
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
                }
        }
    }

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (continuation.isActive) {
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resumeWithException(
                            task.exception ?: IllegalStateException("La operación de Firebase falló.")
                        )
                    }
                }
            }
        }

    private sealed interface RegistrationResult {
        data object Created : RegistrationResult
        data object UsernameTaken : RegistrationResult
        data object ProfileAlreadyExists : RegistrationResult
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
