package es.ulpgc.wishlistapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import es.ulpgc.wishlistapp.domain.model.UsernamePolicy

internal class UsernameRegistry(
    private val firestore: FirebaseFirestore
) {
    fun reserve(
        transaction: Transaction,
        uid: String,
        currentUsername: String?,
        newUsername: String
    ): ReservationResult {
        val newUsernameReference = firestore.collection("usernames").document(newUsername)
        val newUsernameDocument = transaction.get(newUsernameReference)
        val oldUsernameReference = currentUsername
            ?.takeIf { UsernamePolicy.isValid(it) && it != newUsername }
            ?.let { firestore.collection("usernames").document(it) }
        val oldUsernameDocument = oldUsernameReference?.let(transaction::get)

        if (newUsernameDocument.exists() &&
            newUsernameDocument.getString("uid") != uid
        ) {
            return ReservationResult.AlreadyTaken
        }

        if (!newUsernameDocument.exists()) {
            transaction.set(newUsernameReference, mapOf("uid" to uid))
        }
        if (oldUsernameDocument?.getString("uid") == uid) {
            oldUsernameReference?.let(transaction::delete)
        }

        return ReservationResult.Reserved
    }

    sealed interface ReservationResult {
        data object Reserved : ReservationResult
        data object AlreadyTaken : ReservationResult
    }
}
