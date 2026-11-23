package es.ulpgc.wishlistapp.domain.model

const val DEFAULT_PROFILE_AVATAR_ID = "person"

data class UserProfile(
    val uid: String,
    val username: String,
    val name: String,
    val friendCode: String,
    val description: String,
    val hobbies: List<String>,
    val avatarId: String,
    val shirtSize: String,
    val pantsSize: String,
    val shoeSize: String
)

data class EditableProfile(
    val username: String,
    val name: String,
    val description: String,
    val hobbies: List<String>,
    val avatarId: String,
    val shirtSize: String,
    val pantsSize: String,
    val shoeSize: String
)
