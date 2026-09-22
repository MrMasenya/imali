package za.ac.imali.data

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val email: String = "",
    val currency: String = "ZAR",
    val theme: String = "system",
    val notify: Boolean = true,
)

data class AuthRequest(
    val name: String? = null,
    val email: String,
    val password: String,
)

data class AuthResponse(
    val token: String,
    val user: User,
)

data class Transaction(
    @SerializedName("_id") val id: String? = null,
    val amount: Double,
    val type: String,
    val category: String,
    val note: String = "",
    val date: String,
)

data class TransactionRequest(
    val amount: Double,
    val type: String,
    val category: String,
    val note: String,
    val date: String,
)

data class Budget(
    @SerializedName("_id") val id: String? = null,
    val category: String,
    val month: String,
    val limit: Double,
)

data class BudgetRequest(
    val category: String,
    val month: String,
    val limit: Double,
)

data class SettingsRequest(
    val name: String? = null,
    val currency: String? = null,
    val theme: String? = null,
)

data class Message(val message: String)
