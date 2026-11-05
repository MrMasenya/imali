package za.ac.imali.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val key = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "imali_session",
        key,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var userName: String
        get() = prefs.getString("name", "") ?: ""
        set(value) = prefs.edit().putString("name", value).apply()

    var currency: String
        get() = prefs.getString("currency", "ZAR") ?: "ZAR"
        set(value) = prefs.edit().putString("currency", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark", false)
        set(value) = prefs.edit().putBoolean("dark", value).apply()

    var notifications: Boolean
        get() = prefs.getBoolean("notify", true)
        set(value) = prefs.edit().putBoolean("notify", value).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    fun clear() = prefs.edit().clear().apply()
}
