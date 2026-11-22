package es.ulpgc.wishlistapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernamePolicyTest {

    @Test
    fun acceptsThreeToTwentyAsciiLettersDigitsAndUnderscores() {
        assertTrue(UsernamePolicy.isValid("Ana_123"))
        assertTrue(UsernamePolicy.isValid("a".repeat(3)))
        assertTrue(UsernamePolicy.isValid("a".repeat(20)))
    }

    @Test
    fun rejectsNamesOutsideTheAllowedFormat() {
        assertFalse(UsernamePolicy.isValid("ab"))
        assertFalse(UsernamePolicy.isValid("a".repeat(21)))
        assertFalse(UsernamePolicy.isValid("ana maria"))
        assertFalse(UsernamePolicy.isValid("ana-maria"))
        assertFalse(UsernamePolicy.isValid("niño"))
    }

    @Test
    fun trimsWhitespaceWithoutChangingLetterCase() {
        assertTrue(UsernamePolicy.isValid(UsernamePolicy.normalize("  Ana  ")))
        assertTrue(UsernamePolicy.isValid("ana"))
    }
}
