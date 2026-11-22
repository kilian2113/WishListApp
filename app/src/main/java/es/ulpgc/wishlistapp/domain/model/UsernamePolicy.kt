package es.ulpgc.wishlistapp.domain.model

object UsernamePolicy {
    private val pattern = Regex("^[A-Za-z0-9_]{3,20}$")

    fun normalize(username: String): String = username.trim()

    fun isValid(username: String): Boolean = pattern.matches(username)
}
