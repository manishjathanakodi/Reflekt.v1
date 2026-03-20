package com.reflekt.journal.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val KEY_ALIAS        = "reflekt_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREF_FILE        = "reflekt_ks_prefs"
        private const val PREF_ENC_PASS    = "enc_passphrase"
        private const val PREF_IV          = "enc_iv"
        private const val GCM_TAG_LENGTH   = 128
    }

    private val keystore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Cached in memory after first unlock so Room only pays the crypto cost once. */
    @Volatile private var cached: ByteArray? = null

    /**
     * Returns the 32-byte SQLCipher passphrase, creating and persisting it on first call.
     * The passphrase itself is stored encrypted under a 256-bit AES-GCM key that lives
     * exclusively in the Android Keystore — never in app storage.
     */
    fun getOrCreatePassphrase(): ByteArray {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            val encPass = prefs.getString(PREF_ENC_PASS, null)
            val ivB64   = prefs.getString(PREF_IV, null)

            val passphrase = if (encPass != null && ivB64 != null) {
                decrypt(
                    Base64.decode(encPass, Base64.DEFAULT),
                    Base64.decode(ivB64, Base64.DEFAULT),
                )
            } else {
                val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val cipher = encryptCipher()
                val encrypted = cipher.doFinal(fresh)
                prefs.edit()
                    .putString(PREF_ENC_PASS, Base64.encodeToString(encrypted, Base64.DEFAULT))
                    .putString(PREF_IV, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
                    .apply()
                fresh
            }
            cached = passphrase
            return passphrase
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun encryptCipher(): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").also {
            it.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            doFinal(ciphertext)
        }

    private fun getOrCreateKey(): SecretKey {
        if (!keystore.containsAlias(KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }
        }
        return (keystore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }
}
