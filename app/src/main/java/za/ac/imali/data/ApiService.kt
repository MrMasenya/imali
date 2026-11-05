package za.ac.imali.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/sso")
    suspend fun googleSso(@Body body: SsoRequest): AuthResponse

    @GET("api/transactions")
    suspend fun getTransactions(@Header("Authorization") token: String): List<Transaction>

    @POST("api/transactions")
    suspend fun addTransaction(
        @Header("Authorization") token: String,
        @Body body: TransactionRequest,
    ): Transaction

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(
        @Header("Authorization") token: String,
        @Path("id") id: String,
    ): Message

    @GET("api/budgets")
    suspend fun getBudgets(@Header("Authorization") token: String): List<Budget>

    @POST("api/budgets")
    suspend fun addBudget(
        @Header("Authorization") token: String,
        @Body body: BudgetRequest,
    ): Budget

    @GET("api/users/me")
    suspend fun me(@Header("Authorization") token: String): User

    @PUT("api/users/me")
    suspend fun updateSettings(
        @Header("Authorization") token: String,
        @Body body: SettingsRequest,
    ): User

    @PUT("api/users/fcm-token")
    suspend fun saveFcmToken(
        @Header("Authorization") token: String,
        @Body body: FcmTokenRequest,
    ): Message

    @GET("api/stokvels")
    suspend fun getStokvels(@Header("Authorization") token: String): List<Stokvel>

    @POST("api/stokvels")
    suspend fun createStokvel(
        @Header("Authorization") token: String,
        @Body body: StokvelRequest,
    ): Stokvel

    @POST("api/stokvels/{id}/contribute")
    suspend fun contributeStokvel(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: ContributeRequest,
    ): Stokvel
}
