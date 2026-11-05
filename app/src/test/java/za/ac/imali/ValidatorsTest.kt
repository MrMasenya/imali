package za.ac.imali

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import za.ac.imali.util.Validators

class ValidatorsTest {
    @Test
    fun validEmail() {
        assertTrue(Validators.isValidEmail("student@example.com"))
        assertFalse(Validators.isValidEmail("not-an-email"))
    }

    @Test
    fun validPassword() {
        assertTrue(Validators.isValidPassword("secret1"))
        assertFalse(Validators.isValidPassword("123"))
    }

    @Test
    fun validAmount() {
        assertTrue(Validators.isValidAmount("150.50"))
        assertFalse(Validators.isValidAmount("0"))
        assertFalse(Validators.isValidAmount("abc"))
    }
}
