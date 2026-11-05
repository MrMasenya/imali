package za.ac.imali.data

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    val email: String = "",
    val currency: String = "ZAR",
    val theme: String = "system",
    val language: String = "en",
    val notify: Boolean = true,
    val streakDays: Int = 0,
    val badges: List<String> = emptyList(),
)

data class AuthRequest(
    val name: String? = null,
    val email: String,
    val password: String,
)

data class SsoRequest(val idToken: String)

data class AuthResponse(val token: String, val user: User)

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

data class BudgetRequest(val category: String, val month: String, val limit: Double)

data class SettingsRequest(
    val name: String? = null,
    val currency: String? = null,
    val theme: String? = null,
    val language: String? = null,
    val notify: Boolean? = null,
)

data class FcmTokenRequest(val token: String)

data class Stokvel(
    @SerializedName("_id") val id: String? = null,
    val name: String,
    val members: List<String> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
)

data class Contribution(val member: String, val amount: Double, val date: String)

data class StokvelRequest(val name: String, val members: List<String>? = null)

data class ContributeRequest(val member: String, val amount: Double, val date: String? = null)

data class BlobResponse(val id: String, val url: String, val contentType: String)

data class Message(val message: String)
