package es.ulpgc.wishlistapp.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import es.ulpgc.wishlistapp.domain.model.DEFAULT_PROFILE_AVATAR_ID
import es.ulpgc.wishlistapp.domain.model.EditableProfile
import es.ulpgc.wishlistapp.domain.model.UsernamePolicy
import es.ulpgc.wishlistapp.domain.model.UserProfile
import es.ulpgc.wishlistapp.domain.repository.BackendPermissionDeniedException
import es.ulpgc.wishlistapp.domain.repository.FriendProfileNotFoundException
import es.ulpgc.wishlistapp.domain.repository.FriendRelationshipNotFoundException
import es.ulpgc.wishlistapp.domain.repository.ProfileRepository
import es.ulpgc.wishlistapp.domain.repository.UsernameAlreadyTakenException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FRIEND_CODE_LENGTH = 8
private const val MAX_FRIEND_CODE_ATTEMPTS = 10
private const val FRIEND_CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
class ProfileRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    private val secureRandom = SecureRandom()
    private val usernameRegistry = UsernameRegistry(firestore)

    override suspend fun loadOwnProfile(): Result<UserProfile> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))
        val userReference = firestore.collection("users").document(firebaseUser.uid)

        repeat(MAX_FRIEND_CODE_ATTEMPTS) {
            val candidateCode = generateFriendCode()
            try {
                when (val result = firestore.runTransaction { transaction ->
                    val userDocument = transaction.get(userReference)
                    val currentCode = userDocument.getString("friendCode").orEmpty()
                    val currentCodeReference = currentCode
                        .takeIf(String::isNotBlank)
                        ?.let { firestore.collection("friendCodes").document(it) }
                    val currentCodeDocument = currentCodeReference?.let(transaction::get)

                    if (userDocument.exists() &&
                        currentCodeDocument?.getString("uid") == firebaseUser.uid
                    ) {
                        return@runTransaction ProfileInitializationResult.Loaded(
                            userDocument.toUserProfile(
                                uid = firebaseUser.uid,
                                fallbackName = firebaseUser.displayName.orEmpty(),
                                friendCode = currentCode
                            )
                        )
                    }

                    val candidateReference = firestore.collection("friendCodes").document(candidateCode)
                    val candidateDocument = if (candidateCode == currentCode) {
                        currentCodeDocument ?: transaction.get(candidateReference)
                    } else {
                        transaction.get(candidateReference)
                    }
                    if (candidateDocument.exists() &&
                        candidateDocument.getString("uid") != firebaseUser.uid
                    ) {
                        return@runTransaction ProfileInitializationResult.CodeTaken
                    }

                    if (candidateDocument.exists().not()) {
                        transaction.set(candidateReference, mapOf("uid" to firebaseUser.uid))
                    }
                    if (currentCode.isNotBlank() &&
                        currentCode != candidateCode &&
                        currentCodeDocument?.getString("uid") == firebaseUser.uid
                    ) {
                        currentCodeReference?.let(transaction::delete)
                    }
                    transaction.set(
                        userReference,
                        mapOf(
                            "uid" to firebaseUser.uid,
                            "friendCode" to candidateCode
                        ),
                        SetOptions.merge()
                    )

                    ProfileInitializationResult.Loaded(
                        userDocument.toUserProfile(
                            uid = firebaseUser.uid,
                            fallbackName = firebaseUser.displayName.orEmpty(),
                            friendCode = candidateCode
                        )
                    )
                }.await()) {
                    is ProfileInitializationResult.Loaded -> return Result.success(result.profile)
                    ProfileInitializationResult.CodeTaken -> Unit
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                return Result.failure(exception)
            }
        }

        return Result.failure(IllegalStateException("No se pudo generar un código de amigo único."))
    }

    override suspend fun loadFriendProfile(friendUid: String): Result<UserProfile> {
        val currentUid = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))
        if (friendUid.isBlank() || friendUid == currentUid) {
            return Result.failure(IllegalArgumentException("El identificador del perfil no es válido."))
        }

        return try {
            val friendship = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(friendUid)
                .get()
                .await()
            if (!friendship.exists()) {
                return Result.failure(FriendRelationshipNotFoundException())
            }

            val profileDocument = firestore.collection("users")
                .document(friendUid)
                .get()
                .await()

            if (!profileDocument.exists()) {
                Result.failure(FriendProfileNotFoundException())
            } else {
                Result.success(
                    profileDocument.toUserProfile(
                        uid = friendUid,
                        friendCode = profileDocument.getString("friendCode").orEmpty()
                    )
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun updateOwnProfile(profile: EditableProfile): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))
        val username = UsernamePolicy.normalize(profile.username)
        if (!UsernamePolicy.isValid(username)) {
            return Result.failure(
                IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 20 caracteres: letras, números o guion bajo."
                )
            )
        }

        return try {
            when (firestore.runTransaction { transaction ->
                val userReference = firestore.collection("users").document(firebaseUser.uid)
                val userDocument = transaction.get(userReference)
                if (!userDocument.exists()) {
                    return@runTransaction ProfileUpdateResult.ProfileMissing
                }

                val currentUsername = userDocument.getString("username").orEmpty()
                if (usernameRegistry.reserve(
                        transaction = transaction,
                        uid = firebaseUser.uid,
                        currentUsername = currentUsername,
                        newUsername = username
                    ) == UsernameRegistry.ReservationResult.AlreadyTaken
                ) {
                    return@runTransaction ProfileUpdateResult.UsernameTaken
                }

                transaction.update(userReference, profile.toFirestoreFields(username))

                ProfileUpdateResult.Updated
            }.await()) {
                ProfileUpdateResult.Updated -> Result.success(Unit)
                ProfileUpdateResult.UsernameTaken -> Result.failure(UsernameAlreadyTakenException())
                ProfileUpdateResult.ProfileMissing -> {
                    Result.failure(IllegalStateException("No se encontró el perfil del usuario."))
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (
                exception is FirebaseFirestoreException &&
                exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                Log.e(TAG, "Firestore denied profile update.", exception)
                Result.failure(BackendPermissionDeniedException(exception))
            } else {
                Result.failure(exception)
            }
        }
    }

    override suspend fun regenerateFriendCode(): Result<String> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))
        val userReference = firestore.collection("users").document(firebaseUser.uid)

        repeat(MAX_FRIEND_CODE_ATTEMPTS) {
            val candidateCode = generateFriendCode()
            try {
                when (val result = firestore.runTransaction { transaction ->
                    val userDocument = transaction.get(userReference)
                    if (!userDocument.exists()) {
                        return@runTransaction FriendCodeRegenerationResult.ProfileMissing
                    }

                    val currentCode = userDocument.getString("friendCode").orEmpty()
                    if (candidateCode == currentCode) {
                        return@runTransaction FriendCodeRegenerationResult.CodeTaken
                    }

                    val currentCodeReference = currentCode
                        .takeIf(String::isNotBlank)
                        ?.let { firestore.collection("friendCodes").document(it) }
                    val currentCodeDocument = currentCodeReference?.let(transaction::get)
                    val candidateReference = firestore.collection("friendCodes").document(candidateCode)
                    val candidateDocument = transaction.get(candidateReference)
                    if (candidateDocument.exists() &&
                        candidateDocument.getString("uid") != firebaseUser.uid
                    ) {
                        return@runTransaction FriendCodeRegenerationResult.CodeTaken
                    }

                    if (candidateDocument.exists().not()) {
                        transaction.set(candidateReference, mapOf("uid" to firebaseUser.uid))
                    }
                    transaction.update(userReference, "friendCode", candidateCode)
                    if (currentCodeDocument?.getString("uid") == firebaseUser.uid) {
                        currentCodeReference?.let(transaction::delete)
                    }

                    FriendCodeRegenerationResult.Regenerated(candidateCode)
                }.await()) {
                    is FriendCodeRegenerationResult.Regenerated -> return Result.success(result.code)
                    FriendCodeRegenerationResult.CodeTaken -> Unit
                    FriendCodeRegenerationResult.ProfileMissing -> {
                        return Result.failure(IllegalStateException("No se encontró el perfil del usuario."))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                return Result.failure(exception)
            }
        }

        return Result.failure(IllegalStateException("No se pudo generar un código de amigo único."))
    }

    private fun generateFriendCode(): String = buildString(FRIEND_CODE_LENGTH) {
        repeat(FRIEND_CODE_LENGTH) {
            append(FRIEND_CODE_CHARACTERS[secureRandom.nextInt(FRIEND_CODE_CHARACTERS.length)])
        }
    }

    private fun DocumentSnapshot.toUserProfile(
        uid: String,
        fallbackName: String = "",
        friendCode: String = ""
    ) = UserProfile(
        uid = uid,
        username = getString("username").orEmpty(),
        name = getString("name") ?: fallbackName,
        friendCode = friendCode,
        description = getString("description").orEmpty(),
        hobbies = getStringList("hobbies"),
        avatarId = getString("avatarId") ?: DEFAULT_PROFILE_AVATAR_ID,
        shirtSize = getString("shirtSize").orEmpty(),
        pantsSize = getString("pantsSize").orEmpty(),
        shoeSize = getString("shoeSize").orEmpty()
    )

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun EditableProfile.toFirestoreFields(username: String): Map<String, Any> = mapOf(
        "username" to username,
        "name" to name,
        "description" to description,
        "hobbies" to hobbies,
        "avatarId" to avatarId,
        "shirtSize" to shirtSize,
        "pantsSize" to pantsSize,
        "shoeSize" to shoeSize
    )

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
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

    private sealed interface ProfileInitializationResult {
        data class Loaded(val profile: UserProfile) : ProfileInitializationResult
        data object CodeTaken : ProfileInitializationResult
    }

    private sealed interface ProfileUpdateResult {
        data object Updated : ProfileUpdateResult
        data object UsernameTaken : ProfileUpdateResult
        data object ProfileMissing : ProfileUpdateResult
    }

    private sealed interface FriendCodeRegenerationResult {
        data class Regenerated(val code: String) : FriendCodeRegenerationResult
        data object CodeTaken : FriendCodeRegenerationResult
        data object ProfileMissing : FriendCodeRegenerationResult
    }

    private companion object {
        const val TAG = "ProfileRepository"
    }
}
