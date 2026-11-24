package es.ulpgc.wishlistapp.domain.model

enum class GiftPriority(val label: String) {
    HIGH("Alta"),
    MEDIUM("Media"),
    LOW("Baja")
}

data class Gift(
    val id: String = "",
    val listId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val priority: GiftPriority = GiftPriority.MEDIUM,
    val description: String = "",
    val productUrl: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
