package es.ulpgc.wishlistapp.domain.model

data class WishList(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
