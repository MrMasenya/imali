package za.ac.imali.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.ac.imali.data.ApiClient
import za.ac.imali.data.AuthRequest
import za.ac.imali.data.Budget
import za.ac.imali.data.BudgetRequest
import za.ac.imali.data.FcmTokenRequest
import za.ac.imali.data.FinanceRepository
import za.ac.imali.data.SessionManager
import za.ac.imali.data.SettingsRequest
import za.ac.imali.data.SsoRequest
import za.ac.imali.data.Stokvel
import za.ac.imali.data.StokvelRequest
import za.ac.imali.data.ContributeRequest
import za.ac.imali.data.Transaction
import za.ac.imali.sync.SyncScheduler
import za.ac.imali.util.Finance
import za.ac.imali.util.NetworkMonitor
import za.ac.imali.util.Validators
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class UiState(
    val isLoggedIn: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val currency: String = "ZAR",
    val language: String = "en",
    val darkTheme: Boolean = false,
    val notifications: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val stokvels: List<Stokvel> = emptyList(),
    val balance: Double = 0.0,
    val streakDays: Int = 0,
    val badges: List<String> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isOnline: Boolean = true,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionManager(app)
    private val api = ApiClient.api
    private val repo = FinanceRepository(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                isLoggedIn = session.token != null,
                userName = session.userName,
                currency = session.currency,
                language = session.language,
                darkTheme = session.darkTheme,
                notifications = session.notifications,
            )
        }
        viewModelScope.launch {
            NetworkMonitor.observeOnline(app).collect { online ->
                _state.update { it.copy(isOnline = online) }
                if (online && session.token != null) SyncScheduler.enqueue(app)
            }
        }
        viewModelScope.launch {
            repo.observeTransactions().collect { txs ->
                _state.update {
                    it.copy(
                        transactions = txs,
                        balance = Finance.balance(txs),
                    )
                }
            }
        }
        if (session.token != null) refreshAll()
    }

    private fun bearer(): String = "Bearer ${session.token}"

    fun clearError() = _state.update { it.copy(error = null) }

    fun register(name: String, email: String, password: String) {
        if (!Validators.isValidName(name)) return setError("Name must be at least 2 characters")
        if (!Validators.isValidEmail(email)) return setError("Enter a valid email")
        if (!Validators.isValidPassword(password)) return setError("Password must be at least 6 characters")
        runAuth {
            val response = api.register(AuthRequest(name = name.trim(), email = email.trim(), password = password))
            saveSession(response.token, response.user)
        }
    }

    fun login(email: String, password: String) {
        if (!Validators.isValidEmail(email)) return setError("Enter a valid email")
        if (!Validators.isValidPassword(password)) return setError("Password must be at least 6 characters")
        runAuth {
            val response = api.login(AuthRequest(email = email.trim(), password = password))
            saveSession(response.token, response.user)
        }
    }

    fun googleSignIn(idToken: String) {
        runAuth {
            val response = api.googleSso(SsoRequest(idToken))
            saveSession(response.token, response.user)
            Log.i(TAG, "Google SSO success")
        }
    }

    fun logout() {
        session.clear()
        _state.update {
            UiState(
                isLoggedIn = false,
                darkTheme = it.darkTheme,
                language = it.language,
                notifications = it.notifications,
            )
        }
    }

    fun refreshAll() = viewModelScope.launch {
        val token = session.token ?: return@launch
        _state.update { it.copy(loading = true, error = null) }
        try {
            repo.syncPending(token)
            repo.refreshFromServer(token)
            SyncScheduler.enqueue(getApplication())
            val budgets = api.getBudgets(bearer())
            val stokvels = api.getStokvels(bearer())
            val user = api.me(bearer())
            _state.update {
                it.copy(
                    loading = false,
                    budgets = budgets,
                    stokvels = stokvels,
                    userName = user.name,
                    currency = user.currency,
                    streakDays = user.streakDays,
                    badges = user.badges,
                    language = user.language,
                )
            }
            session.userName = user.name
            session.currency = user.currency
            session.language = user.language
            registerFcmToken()
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            _state.update { it.copy(loading = false, error = e.message ?: "Could not load data") }
        }
    }

    fun addTransaction(amountText: String, type: String, category: String, note: String) {
        if (!Validators.isValidAmount(amountText)) return setError("Enter a valid amount")
        if (!Validators.isValidCategory(category)) return setError("Choose a category")
        val amount = amountText.toDouble()
        val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                repo.addTransactionLocal(amount, type, category.trim(), note.trim(), date)
                SyncScheduler.enqueue(getApplication())
                if (session.token != null) {
                    try {
                        repo.syncPending(session.token!!)
                        refreshAll()
                    } catch (_: Exception) {
                        _state.update { it.copy(loading = false, pendingSyncCount = it.pendingSyncCount + 1) }
                    }
                } else {
                    _state.update { it.copy(loading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not save transaction") }
            }
        }
    }

    fun deleteTransaction(id: String?) {
        if (id.isNullOrBlank() || id.startsWith("local-")) return
        viewModelScope.launch {
            try {
                session.token?.let { repo.deleteTransaction(it, id) }
                refreshAll()
            } catch (e: Exception) {
                setError(e.message ?: "Could not delete transaction")
            }
        }
    }

    fun addBudget(category: String, month: String, limitText: String) {
        if (!Validators.isValidCategory(category)) return setError("Choose a category")
        if (!Validators.isValidAmount(limitText)) return setError("Enter a valid budget limit")
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                api.addBudget(bearer(), BudgetRequest(category.trim(), month, limitText.toDouble()))
                refreshAll()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not save budget") }
            }
        }
    }

    fun createStokvel(name: String, membersText: String) {
        if (name.trim().length < 2) return setError("Stokvel name too short")
        val members = membersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            try {
                api.createStokvel(bearer(), StokvelRequest(name.trim(), members))
                refreshAll()
            } catch (e: Exception) {
                setError(e.message ?: "Could not create stokvel")
            }
        }
    }

    fun contributeStokvel(stokvelId: String, member: String, amountText: String) {
        if (!Validators.isValidAmount(amountText)) return setError("Enter a valid amount")
        viewModelScope.launch {
            try {
                api.contributeStokvel(
                    bearer(),
                    stokvelId,
                    ContributeRequest(member.trim(), amountText.toDouble()),
                )
                refreshAll()
            } catch (e: Exception) {
                setError(e.message ?: "Could not record contribution")
            }
        }
    }

    fun saveSettings(name: String, currency: String, language: String, darkTheme: Boolean, notifications: Boolean) {
        if (!Validators.isValidName(name)) return setError("Name must be at least 2 characters")
        session.darkTheme = darkTheme
        session.notifications = notifications
        session.language = language
        _state.update { it.copy(darkTheme = darkTheme, notifications = notifications, language = language) }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val user = api.updateSettings(
                    bearer(),
                    SettingsRequest(
                        name = name.trim(),
                        currency = currency,
                        theme = if (darkTheme) "dark" else "light",
                        language = language,
                        notify = notifications,
                    ),
                )
                session.userName = user.name
                session.currency = user.currency
                session.language = user.language
                _state.update {
                    it.copy(
                        loading = false,
                        userName = user.name,
                        currency = user.currency,
                        language = user.language,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not save settings") }
            }
        }
    }

    private fun saveSession(token: String, user: za.ac.imali.data.User) {
        session.token = token
        session.userName = user.name
        session.currency = user.currency
        session.language = user.language
        _state.update {
            it.copy(
                isLoggedIn = true,
                userName = user.name,
                currency = user.currency,
                language = user.language,
                streakDays = user.streakDays,
                badges = user.badges,
                error = null,
            )
        }
        refreshAll()
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            viewModelScope.launch {
                try {
                    api.saveFcmToken(bearer(), FcmTokenRequest(token))
                } catch (e: Exception) {
                    Log.w(TAG, "FCM token upload failed: ${e.message}")
                }
            }
        }
    }

    private fun runAuth(block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            block()
            _state.update { it.copy(loading = false) }
        } catch (e: Exception) {
            Log.e(TAG, "Auth failed", e)
            _state.update { it.copy(loading = false, error = e.message ?: "Authentication failed") }
        }
    }

    private fun setError(message: String) = _state.update { it.copy(error = message) }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
