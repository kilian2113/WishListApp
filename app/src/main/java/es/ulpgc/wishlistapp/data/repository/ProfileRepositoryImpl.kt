package es.ulpgc.wishlistapp.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.ulpgc.wishlistapp.domain.model.EditableProfile
import es.ulpgc.wishlistapp.domain.model.UserProfile
import es.ulpgc.wishlistapp.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FRIEND_CODE_LENGTH = 8
private const val MAX_FRIEND_CODE_ATTEMPTS = 10
private const val FRIEND_CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private const val DEFAULT_AVATAR_ID = "person"

class ProfileRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    private val secureRandom = SecureRandom()

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
                            userDocument.toUserProfile(firebaseUser, currentCode)
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
                        userDocument.toUserProfile(firebaseUser, candidateCode)
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

    override suspend fun updateOwnProfile(profile: EditableProfile): Result<Unit> {
        val firebaseUser = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada."))

        return try {
            firestore.collection("users")
                .document(firebaseUser.uid)
                .update(profile.toFirestoreFields())
                .await()
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
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
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        friendCode: String
    ) = UserProfile(
        uid = firebaseUser.uid,
        username = getString("username").orEmpty(),
        name = getString("name") ?: firebaseUser.displayName.orEmpty(),
        friendCode = friendCode,
        description = getString("description").orEmpty(),
        hobbies = getStringList("hobbies"),
        avatarId = getString("avatarId") ?: DEFAULT_AVATAR_ID,
        shirtSize = getString("shirtSize").orEmpty(),
        pantsSize = getString("pantsSize").orEmpty(),
        shoeSize = getString("shoeSize").orEmpty()
    )

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun EditableProfile.toFirestoreFields(): Map<String, Any> = mapOf(
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

    private sealed interface FriendCodeRegenerationResult {
        data class Regenerated(val code: String) : FriendCodeRegenerationResult
        data object CodeTaken : FriendCodeRegenerationResult
        data object ProfileMissing : FriendCodeRegenerationResult
    }
}
