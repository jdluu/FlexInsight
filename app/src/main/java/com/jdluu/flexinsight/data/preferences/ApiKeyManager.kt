package com.jdluu.flexinsight.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.legacyApiKeyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "api_key_preferences"
)

/**
 * Stores the Hevy API key in encrypted shared preferences.
 * Migrates keys from the legacy DataStore on first access.
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _apiKeyFlow = MutableStateFlow<String?>(readStoredKey())
    val apiKeyFlow: Flow<String?> = _apiKeyFlow.asStateFlow()

    private val legacyKey = stringPreferencesKey("api_key")

    init {
        encryptedPrefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == API_KEY) {
                _apiKeyFlow.value = readStoredKey()
            }
        }
    }

    suspend fun getApiKey(): String? {
        migrateFromLegacyIfNeeded()
        return readStoredKey()
    }

    suspend fun saveApiKey(apiKey: String) {
        migrateFromLegacyIfNeeded()
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().putString(API_KEY, apiKey).apply()
        }
        _apiKeyFlow.value = apiKey
    }

    suspend fun clearApiKey() {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().remove(API_KEY).apply()
        }
        _apiKeyFlow.value = null
    }

    suspend fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun isValidApiKeyFormat(apiKey: String): Boolean {
        return apiKey.isNotBlank() && apiKey.length >= 10
    }

    private fun readStoredKey(): String? =
        encryptedPrefs.getString(API_KEY, null)?.takeIf { it.isNotBlank() }

    private suspend fun migrateFromLegacyIfNeeded() {
        if (readStoredKey() != null) return

        val legacyValue = withContext(Dispatchers.IO) {
            context.legacyApiKeyDataStore.data.first()[legacyKey]
        } ?: return

        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().putString(API_KEY, legacyValue).apply()
            context.legacyApiKeyDataStore.edit { preferences ->
                preferences.remove(legacyKey)
            }
        }
        _apiKeyFlow.value = legacyValue
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "encrypted_api_key_prefs"
        private const val API_KEY = "api_key"
    }
}
