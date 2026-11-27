package com.jdluu.flexinsight.data.preferences

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.jdluu.flexinsight.TestApplication
import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class ApiKeyManagerTest {

    private lateinit var context: Context
    private lateinit var cipher: InMemoryAesGcmCipher
    private lateinit var manager: ApiKeyManager

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = Dispatchers.Unconfined
        override val io: CoroutineDispatcher get() = Dispatchers.Unconfined
        override val default: CoroutineDispatcher get() = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cipher = InMemoryAesGcmCipher()
        manager = ApiKeyManager(
            context = context,
            dispatcherProvider = testDispatchers,
            cipher = cipher
        )
        runBlocking { manager.clearApiKey() }
    }

    @Test
    fun saveAndReadApiKey_roundTripsEncryptedValue() = runTest {
        assertFalse(manager.hasApiKey())
        assertNull(manager.getApiKey())

        manager.saveApiKey("hevy_secret_key_123456")

        assertTrue(manager.hasApiKey())
        assertEquals("hevy_secret_key_123456", manager.getApiKey())
        assertEquals("hevy_secret_key_123456", manager.apiKeyFlow.first())
    }

    @Test
    fun clearApiKey_removesStoredKeyAndResetsFlow() = runTest {
        manager.saveApiKey("hevy_secret_key_123456")
        assertTrue(manager.hasApiKey())

        manager.clearApiKey()

        assertFalse(manager.hasApiKey())
        assertNull(manager.getApiKey())
        assertNull(manager.apiKeyFlow.first())
    }

    @Test
    fun corruptedCiphertext_returnsNullSafely() = runTest {
        manager.saveApiKey("hevy_secret_key_123456")
        cipher.failDecrypt = true

        assertNull(manager.getApiKey())
        assertFalse(manager.hasApiKey())
    }

    @Test
    fun isValidApiKeyFormat_enforcesBlankAndLength() {
        assertFalse(manager.isValidApiKeyFormat(""))
        assertFalse(manager.isValidApiKeyFormat("   "))
        assertFalse(manager.isValidApiKeyFormat("short"))
        assertTrue(manager.isValidApiKeyFormat("1234567890"))
    }

    private class InMemoryAesGcmCipher : SecretKeyCipher {
        var failDecrypt = false
        private val secretKey: SecretKey = KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()

        override fun encrypt(plaintext: String): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val payload = ByteArray(1 + iv.size + encrypted.size)
            payload[0] = iv.size.toByte()
            iv.copyInto(payload, destinationOffset = 1)
            encrypted.copyInto(payload, destinationOffset = 1 + iv.size)
            return Base64.encodeToString(payload, Base64.NO_WRAP)
        }

        override fun decrypt(ciphertext: String): String? {
            if (failDecrypt) return null
            val payload = Base64.decode(ciphertext, Base64.NO_WRAP)
            val ivSize = payload[0].toInt() and 0xFF
            val iv = payload.copyOfRange(1, 1 + ivSize)
            val encrypted = payload.copyOfRange(1 + ivSize, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }
    }
}
