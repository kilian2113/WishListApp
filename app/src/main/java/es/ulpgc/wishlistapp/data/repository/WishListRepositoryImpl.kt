package es.ulpgc.wishlistapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import es.ulpgc.wishlistapp.domain.model.WishList
import es.ulpgc.wishlistapp.domain.repository.WishListRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WishListRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : WishListRepository {

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun loadWishLists(): Result<List<WishList>> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val lists = querySnapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name").orEmpty()
                        val description = doc.getString("description").orEmpty()
                        val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        WishList(
                            id = id,
                            userId = userId,
                            name = name,
                            description = description,
                            itemCount = itemCount,
                            createdAt = createdAt
                        )
                    }
                    if (continuation.isActive) {
                        continuation.resume(Result.success(lists))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }

    override suspend fun createWishList(name: String, description: String): Result<WishList> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document()

            val now = System.currentTimeMillis()
            val listData = hashMapOf(
                "id" to docRef.id,
                "userId" to userId,
                "name" to name,
                "description" to description,
                "itemCount" to 0,
                "createdAt" to now
            )

            docRef.set(listData)
                .addOnSuccessListener {
                    val wishList = WishList(
                        id = docRef.id,
                        userId = userId,
                        name = name,
                        description = description,
                        itemCount = 0,
                        createdAt = now
                    )
                    if (continuation.isActive) {
                        continuation.resume(Result.success(wishList))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }

    override suspend fun deleteWishList(listId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)
                .delete()
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }
}
