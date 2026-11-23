package es.ulpgc.wishlistapp.domain.model

import java.util.Locale

object FriendCodePolicy {
    private val pattern = Regex("^[A-HJ-NP-Z2-9]{8}$")

    fun normalize(code: String): String = code.trim().uppercase(Locale.ROOT)

    fun isValid(code: String): Boolean = pattern.matches(code)
}
