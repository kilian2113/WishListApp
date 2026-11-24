package es.ulpgc.wishlistapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import es.ulpgc.wishlistapp.domain.model.Gift
import es.ulpgc.wishlistapp.domain.model.GiftPriority
import es.ulpgc.wishlistapp.domain.model.WishList
import es.ulpgc.wishlistapp.domain.repository.WishListRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                        try {
                            val id = doc.id
                            val name = doc.getString("name").orEmpty()
                            val description = doc.getString("description").orEmpty()
                            val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                            val eventDate = when (val raw = doc.get("eventDate")) {
                                is String -> raw
                                is Number -> {
                                    val millis = raw.toLong()
                                    if (millis > 0) {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                                    } else ""
                                }
                                else -> ""
                            }
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            WishList(
                                id = id,
                                userId = userId,
                                name = name,
                                description = description,
                                itemCount = itemCount,
                                eventDate = eventDate,
                                createdAt = createdAt
                            )
                        } catch (_: Exception) {
                            null
                        }
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
                "eventDate" to "",
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
                        eventDate = "",
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

    override suspend fun getWishList(listId: String): Result<WishList> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)
                .get()
                .addOnSuccessListener { doc ->
                    try {
                        if (doc.exists()) {
                            val name = doc.getString("name").orEmpty()
                            val description = doc.getString("description").orEmpty()
                            val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                            val eventDate = when (val raw = doc.get("eventDate")) {
                                is String -> raw
                                is Number -> {
                                    val millis = raw.toLong()
                                    if (millis > 0) {
                                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                                    } else ""
                                }
                                else -> ""
                            }
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            val wishList = WishList(
                                id = listId,
                                userId = userId,
                                name = name,
                                description = description,
                                itemCount = itemCount,
                                eventDate = eventDate,
                                createdAt = createdAt
                            )
                            if (continuation.isActive) {
                                continuation.resume(Result.success(wishList))
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("La lista especificada no existe.")))
                            }
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(e))
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }

    override suspend fun updateWishList(
        listId: String,
        name: String,
        description: String,
        eventDate: String
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "description" to description,
                "eventDate" to eventDate
            )
            firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)
                .update(updates)
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

    override suspend fun loadGifts(listId: String): Result<List<Gift>> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)
                .collection("gifts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val gifts = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val name = doc.getString("name").orEmpty()
                            val price = doc.getDouble("price") ?: 0.0
                            val priorityStr = doc.getString("priority").orEmpty()
                            val priority = try {
                                GiftPriority.valueOf(priorityStr)
                            } catch (_: Exception) {
                                GiftPriority.MEDIUM
                            }
                            val description = doc.getString("description").orEmpty()
                            val productUrl = doc.getString("productUrl").orEmpty()
                            val imageUrl = doc.getString("imageUrl").orEmpty()
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            Gift(
                                id = id,
                                listId = listId,
                                name = name,
                                price = price,
                                priority = priority,
                                description = description,
                                productUrl = productUrl,
                                imageUrl = imageUrl,
                                createdAt = createdAt
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resume(Result.success(gifts))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }

    override suspend fun createGift(listId: String, gift: Gift): Result<Gift> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            val listDocRef = firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)

            val giftDocRef = listDocRef.collection("gifts").document()

            val now = System.currentTimeMillis()
            val giftData = hashMapOf(
                "id" to giftDocRef.id,
                "listId" to listId,
                "name" to gift.name,
                "price" to gift.price,
                "priority" to gift.priority.name,
                "description" to gift.description,
                "productUrl" to gift.productUrl,
                "imageUrl" to gift.imageUrl,
                "createdAt" to now
            )

            giftDocRef.set(giftData)
                .addOnSuccessListener {
                    listDocRef.update("itemCount", FieldValue.increment(1))
                    val createdGift = gift.copy(
                        id = giftDocRef.id,
                        listId = listId,
                        createdAt = now
                    )
                    if (continuation.isActive) {
                        continuation.resume(Result.success(createdGift))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }

    override suspend fun updateGift(listId: String, gift: Gift): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            val giftData = hashMapOf<String, Any>(
                "name" to gift.name,
                "price" to gift.price,
                "priority" to gift.priority.name,
                "description" to gift.description,
                "productUrl" to gift.productUrl,
                "imageUrl" to gift.imageUrl
            )

            firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)
                .collection("gifts")
                .document(gift.id)
                .update(giftData)
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

    override suspend fun deleteGift(listId: String, giftId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Usuario no autenticado"))

        return suspendCancellableCoroutine { continuation ->
            val listDocRef = firestore.collection("users")
                .document(userId)
                .collection("wishlists")
                .document(listId)

            listDocRef.collection("gifts")
                .document(giftId)
                .delete()
                .addOnSuccessListener {
                    listDocRef.update("itemCount", FieldValue.increment(-1))
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
