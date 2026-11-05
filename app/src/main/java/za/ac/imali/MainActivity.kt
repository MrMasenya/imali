package za.ac.imali

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import za.ac.imali.data.SessionManager
import za.ac.imali.ui.IMaliTheme
import za.ac.imali.ui.ImaliApp
import za.ac.imali.util.LocaleHelper
import za.ac.imali.vm.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> Log.i(TAG, "Notification permission: $granted") }

    override fun attachBaseContext(newBase: Context) {
        val lang = SessionManager(newBase).language
        super.attachBaseContext(LocaleHelper.apply(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.state.collectAsState()
            IMaliTheme(darkTheme = state.darkTheme) {
                ImaliApp(
                    state = state,
                    onLogin = vm::login,
                    onRegister = vm::register,
                    onGoogleSignIn = { launchGoogleSignIn(vm) },
                    onLogout = vm::logout,
                    onClearError = vm::clearError,
                    onRefresh = vm::refreshAll,
                    onAddTransaction = vm::addTransaction,
                    onDeleteTransaction = vm::deleteTransaction,
                    onAddBudget = vm::addBudget,
                    onCreateStokvel = vm::createStokvel,
                    onContributeStokvel = vm::contributeStokvel,
                    onSaveSettings = { name, currency, language, dark, notify ->
                        vm.saveSettings(name, currency, language, dark, notify)
                        recreateForLocale(language)
                    },
                )
            }
        }
    }

    private fun recreateForLocale(language: String) {
        SessionManager(this).language = language
        recreate()
    }

    private fun launchGoogleSignIn(vm: MainViewModel) {
        val serverClientId = getString(R.string.google_web_client_id)
        if (serverClientId.startsWith("REPLACE")) {
            Log.w(TAG, "Configure google_web_client_id in strings.xml and Firebase")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                vm.googleSignIn(credential.idToken)
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
