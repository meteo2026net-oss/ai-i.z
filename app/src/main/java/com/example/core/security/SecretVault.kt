package com.example.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecretStorage {
    fun putSecret(key: String, value: String)
    fun getSecret(key: String): String
    fun hasSecret(key: String): Boolean
    fun clearAll()
}

/**
 * Hardware-backed Android Keystore AES-GCM secret vault for storing
 * GitHub PAT, optional OpenAI API key, and Backend Runner token locally.
 * Secrets never leave the device into Firestore.
 */
class SecretVault(context: Context) : SecretStorage {
    private val prefs = context.applicationContext.getSharedPreferences(
        "metra_encrypted_vault",
        Context.MODE_PRIVATE
    )

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun putSecret(key: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(key).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        prefs.edit().putString(key, encoded).apply()
    }

    override fun getSecret(key: String): String {
        val stored = prefs.getString(key, null) ?: return ""
        return try {
            val parts = stored.split(":")
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    override fun hasSecret(key: String): Boolean = getSecret(key).isNotBlank()

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "metra_agent_vault_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val KEY_GITHUB_PAT = "github_pat"
        const val KEY_OPENAI_API = "openai_api_key"
        const val KEY_BACKEND_TOKEN = "backend_runner_token"
        const val KEY_LINKED_ACCOUNTS_JSON = "linked_accounts_json"
        const val KEY_PLATFORM_CONFIG_JSON = "platform_config_json"
    }
}
