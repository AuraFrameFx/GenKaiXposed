package dev.aurakai.genesis.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cryptographic operations using Android Keystore.
 * Provides AES-GCM encryption/decryption for sensitive Genesis data.
 */
@Singleton
class CryptographyManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "genesis_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12 // GCM standard IV size
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Encrypts data using AES-GCM with Android Keystore.
     * @param plaintext Raw data to encrypt
     * @return Encrypted data (IV + ciphertext + tag)
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv

        // Prepend IV to ciphertext (needed for decryption)
        return iv + ciphertext
    }

    /**
     * Decrypts data encrypted by this manager.
     * @param encryptedData IV + ciphertext + tag
     * @return Original plaintext
     */
    fun decrypt(encryptedData: ByteArray): ByteArray {
        require(encryptedData.size > IV_SIZE) {
            "Encrypted data too short (must include IV)"
        }

        val iv = encryptedData.sliceArray(0 until IV_SIZE)
        val ciphertext = encryptedData.sliceArray(IV_SIZE until encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Encrypts a string (convenience method).
     */
    fun encryptString(plaintext: String): ByteArray =
        encrypt(plaintext.toByteArray(Charsets.UTF_8))

    /**
     * Decrypts to a string (convenience method).
     */
    fun decryptString(encryptedData: ByteArray): String =
        decrypt(encryptedData).toString(Charsets.UTF_8)

    /**
     * Gets or generates the master encryption key from Android Keystore.
     * Key is hardware-backed on devices with TEE/StrongBox.
     */
    private fun getOrCreateKey(): SecretKey {
        // Check if key already exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // Set true for biometric-gated encryption
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Deletes the master key (use for factory reset scenarios).
     */
    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}