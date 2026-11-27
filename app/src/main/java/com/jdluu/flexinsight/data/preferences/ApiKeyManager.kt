package com.jdluu.flexinsight.data.preferences

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jdluu.flexinsight.core.dispatchers.DefaultDispatcherProvider
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.core.logger.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureApiKeyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "secure_api_key_preferences"
)

private val Context.legacyApiKeyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "api_key_preferences"
)

/**
 * Symmetric cipher contract used by [ApiKeyManager] so Keystore-backed AES-GCM can be
 * swapped with an in-memory cipher in JVM/Robolectric unit tests.
 */
interface SecretKeyCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String?
}

/**
 * Encrypts and decrypts secrets using an AES-256 GCM key stored in the Android Keystore.
 * Ciphertext format: Base64([ivLengthByte][ivBytes...][cipherTextWithGcmTag]).
 */
class AndroidKeystoreAesGcmCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : SecretKeyCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(1 + iv.size + encrypted.size)
        payload[0] = iv.size.toByte()
        iv.copyInto(payload, destinationOffset = 1)
        encrypted.copyInto(payload, destinationOffset = 1 + iv.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String? {
        return try {
            val payload = Base64.decode(ciphertext, Base64.NO_WRAP)
            if (payload.size < 2) return null
            val ivSize = payload[0].toInt() and 0xFF
            if (ivSize <= 0 || payload.size <= 1 + ivSize) return null
            val iv = payload.copyOfRange(1, 1 + ivSize)
            val encrypted = payload.copyOfRange(1 + ivSize, payload.size)

            val secretKey = getExistingSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.w("Failed to decrypt stored API key; ignoring stale ciphertext", e, TAG)
            null
        }
    }

    private fun getExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(keyAlias, null) as? SecretKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        getExistingSecretKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val TAG = "AndroidKeystoreCipher"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val DEFAULT_KEY_ALIAS = "flexinsight_hevy_api_key_v1"
    }
}

/**
 * Read-only view of Hevy API key availability, for callers that only need to
 * check connectivity status (e.g. AI Trainer pre-chat sync).
 */
interface ApiKeyStatusSource {
    suspend fun hasApiKey(): Boolean
}

/**
 * Stores the Hevy API key encrypted with Android Keystore AES-GCM inside DataStore.
 * Migrates plaintext keys from the legacy DataStore on first access.
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val cipher: SecretKeyCipher = AndroidKeystoreAesGcmCipher()
) : ApiKeyStatusSource {

    private val encryptedKeyPref = stringPreferencesKey(ENCRYPTED_API_KEY)
    private val legacyKeyPref = stringPreferencesKey(LEGACY_API_KEY)
    private val migrationMutex = Mutex()
    @Volatile
    private var migrationChecked = false

    val apiKeyFlow: Flow<String?> = context.secureApiKeyDataStore.data
        .map { preferences ->
            preferences[encryptedKeyPref]
                ?.takeIf { it.isNotBlank() }
                ?.let { cipher.decrypt(it) }
                ?.takeIf { it.isNotBlank() }
        }
        .flowOn(dispatcherProvider.io)
        .distinctUntilChanged()

    suspend fun getApiKey(): String? {
        migrateFromLegacyIfNeeded()
        return withContext(dispatcherProvider.io) {
            apiKeyFlow.first()
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        withContext(dispatcherProvider.io) {
            val encrypted = cipher.encrypt(apiKey)
            context.secureApiKeyDataStore.edit { preferences ->
                preferences[encryptedKeyPref] = encrypted
            }
            context.legacyApiKeyDataStore.edit { preferences ->
                preferences.remove(legacyKeyPref)
            }
            migrationChecked = true
        }
    }

    suspend fun clearApiKey() {
        withContext(dispatcherProvider.io) {
            context.secureApiKeyDataStore.edit { preferences ->
                preferences.remove(encryptedKeyPref)
            }
            context.legacyApiKeyDataStore.edit { preferences ->
                preferences.remove(legacyKeyPref)
            }
            context.deleteSharedPreferences(OBSOLETE_ENCRYPTED_PREFS_NAME)
            migrationChecked = true
        }
    }

    override suspend fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun isValidApiKeyFormat(apiKey: String): Boolean {
        return apiKey.isNotBlank() && apiKey.length >= 10
    }

    private suspend fun migrateFromLegacyIfNeeded() {
        if (migrationChecked) return
        migrationMutex.withLock {
            if (migrationChecked) return
            withContext(dispatcherProvider.io) {
                val existingEncrypted = context.secureApiKeyDataStore.data.first()[encryptedKeyPref]
                if (existingEncrypted.isNullOrBlank()) {
                    val legacyValue = context.legacyApiKeyDataStore.data.first()[legacyKeyPref]
                        ?.takeIf { it.isNotBlank() }
                    if (legacyValue != null) {
                        val encrypted = cipher.encrypt(legacyValue)
                        context.secureApiKeyDataStore.edit { preferences ->
                            preferences[encryptedKeyPref] = encrypted
                        }
                        context.legacyApiKeyDataStore.edit { preferences ->
                            preferences.remove(legacyKeyPref)
                        }
                    }
                }
                migrationChecked = true
            }
        }
    }

    companion object {
        private const val ENCRYPTED_API_KEY = "encrypted_api_key"
        private const val LEGACY_API_KEY = "api_key"
        private const val OBSOLETE_ENCRYPTED_PREFS_NAME = "encrypted_api_key_prefs"
    }
}
