package za.ac.imali.util

object Validators {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun isValidName(name: String): Boolean = name.trim().length >= 2

    fun isValidAmount(text: String): Boolean {
        val value = text.toDoubleOrNull() ?: return false
        return value > 0
    }

    fun isValidCategory(category: String): Boolean = category.trim().isNotEmpty()
}
