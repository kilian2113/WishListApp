package es.ulpgc.wishlistapp.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import es.ulpgc.wishlistapp.domain.model.FriendCodePolicy
import es.ulpgc.wishlistapp.domain.repository.CannotAddYourselfException
import es.ulpgc.wishlistapp.domain.repository.FriendAlreadyAddedException
import es.ulpgc.wishlistapp.domain.repository.FriendCodeNotFoundException
import es.ulpgc.wishlistapp.domain.repository.FriendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FriendRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FriendRepository {

    override suspend fun addFriendByCode(code: String): Result<Unit> {
        val currentUid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))
        val normalizedCode = FriendCodePolicy.normalize(code)
        if (!FriendCodePolicy.isValid(normalizedCode)) {
            return Result.failure(IllegalArgumentException("El código de amigo no tiene un formato válido."))
        }

        return try {
            when (firestore.runTransaction { transaction ->
                val codeReference = firestore.collection("friendCodes").document(normalizedCode)
                val codeDocument = transaction.get(codeReference)
                val friendUid = codeDocument.getString("uid")
                    ?: return@runTransaction AddFriendResult.CodeNotFound

                if (friendUid == currentUid) {
                    return@runTransaction AddFriendResult.CannotAddSelf
                }

                val friendReference = firestore.collection("users")
                    .document(currentUid)
                    .collection("friends")
                    .document(friendUid)
                if (transaction.get(friendReference).exists()) {
                    return@runTransaction AddFriendResult.AlreadyAdded
                }

                transaction.set(
                    friendReference,
                    mapOf(
                        "uid" to friendUid,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                )
                AddFriendResult.Added
            }.await()) {
                AddFriendResult.Added -> Result.success(Unit)
                AddFriendResult.CodeNotFound -> Result.failure(FriendCodeNotFoundException())
                AddFriendResult.AlreadyAdded -> Result.failure(FriendAlreadyAddedException())
                AddFriendResult.CannotAddSelf -> Result.failure(CannotAddYourselfException())
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
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

    private sealed interface AddFriendResult {
        data object Added : AddFriendResult
        data object CodeNotFound : AddFriendResult
        data object AlreadyAdded : AddFriendResult
        data object CannotAddSelf : AddFriendResult
    }
}
