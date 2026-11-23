package es.ulpgc.wishlistapp.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import es.ulpgc.wishlistapp.domain.model.DEFAULT_PROFILE_AVATAR_ID
import es.ulpgc.wishlistapp.domain.model.Friend
import es.ulpgc.wishlistapp.domain.model.FriendCodePolicy
import es.ulpgc.wishlistapp.domain.repository.CannotAddYourselfException
import es.ulpgc.wishlistapp.domain.repository.FriendAlreadyAddedException
import es.ulpgc.wishlistapp.domain.repository.FriendCodeNotFoundException
import es.ulpgc.wishlistapp.domain.repository.FriendProfileNotFoundException
import es.ulpgc.wishlistapp.domain.repository.FriendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FriendRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FriendRepository {

    override suspend fun loadFriends(): Result<List<Friend>> {
        val currentUid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))

        return try {
            val friendDocuments = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val friendUids = friendDocuments.documents.map { document ->
                document.getString("uid")
                    ?: throw IllegalStateException("Hay una amistad guardada sin UID.")
            }.distinct()

            if (friendUids.isEmpty()) {
                return Result.success(emptyList())
            }

            val friendProfiles = friendUids
                .chunked(MAX_PROFILE_LOOKUP_IDS)
                .flatMap { uids ->
                    coroutineScope {
                        uids.map { friendUid ->
                            async {
                                firestore.collection("users")
                                    .document(friendUid)
                                    .get()
                                    .await()
                            }
                        }.awaitAll()
                    }
                }
                .filter { it.exists() }
                .associateBy { it.id }

            Result.success(
                friendUids.map { friendUid ->
                    val profile = friendProfiles[friendUid]
                        ?: throw FriendProfileNotFoundException()
                    val username = profile.getString("username").orEmpty()
                    val name = profile.getString("name")
                        ?.takeIf(String::isNotBlank)
                        ?: username.takeIf(String::isNotBlank)
                        ?: throw FriendProfileNotFoundException()

                    Friend(
                        uid = friendUid,
                        name = name,
                        username = username,
                        avatarId = profile.getString("avatarId") ?: DEFAULT_PROFILE_AVATAR_ID
                    )
                }
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

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

    override suspend fun removeFriend(friendUid: String): Result<Unit> {
        val currentUid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))
        if (friendUid.isBlank() || friendUid == currentUid) {
            return Result.failure(IllegalArgumentException("El identificador del amigo no es válido."))
        }

        return try {
            firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(friendUid)
                .delete()
                .await()
            Result.success(Unit)
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

    private companion object {
        const val MAX_PROFILE_LOOKUP_IDS = 30
    }
}
