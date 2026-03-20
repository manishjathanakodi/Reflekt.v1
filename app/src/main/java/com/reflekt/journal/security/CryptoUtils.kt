package com.reflekt.journal.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val PBKDF2_ALGORITHM  = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS   = 256
    private const val GCM_IV_LENGTH     = 12
    private const val GCM_TAG_LENGTH    = 128

    /**
     * Derives a 256-bit AES key from [password] using PBKDF2 with 100,000 iterations.
     * [salt] should be a stable per-user value (e.g. userProfile.uid.toByteArray()).
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec    = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val tmp     = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts [plaintext] with AES-256-GCM.
     * Returns IV (12 bytes) prepended to ciphertext.
     */
    fun encrypt(key: SecretKey, plaintext: ByteArray): ByteArray {
        val iv     = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypts [ciphertextWithIv] (IV prepended) with AES-256-GCM.
     * Throws [javax.crypto.AEADBadTagException] if the key or data is wrong.
     */
    fun decrypt(key: SecretKey, ciphertextWithIv: ByteArray): ByteArray {
        val iv         = ciphertextWithIv.copyOf(GCM_IV_LENGTH)
        val ciphertext = ciphertextWithIv.copyOfRange(GCM_IV_LENGTH, ciphertextWithIv.size)
        val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }
}
