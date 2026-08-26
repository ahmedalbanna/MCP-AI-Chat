package com.aistudio.mcpchat.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts short secrets (MCP server auth headers) at rest using AES/GCM
 * backed by the Android Keystore.
 *
 * Ciphertext format: "enc:v1:<base64 iv>:<base64 ciphertext>". Values without
 * that prefix are treated as legacy plaintext so existing rows keep working;
 * they are re-encrypted transparently on the next write.
 */
class CryptoManager(
    private val keyProvider: () -> SecretKey = { getOrCreateKeystoreKey() },
) {

    fun encrypt(plain: String): String {
        if (plain.isBlank()) return plain
        if (plain.startsWith(PREFIX)) return plain // already encrypted

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        val encodedIv = iv.toByteString().base64()
        val encodedData = encrypted.toByteString().base64()
        return "$PREFIX$VERSION:$encodedIv:$encodedData"
    }

    @Suppress("ThrowsCount")
    fun decrypt(value: String): String {
        if (value.isBlank()) return value
        if (!value.startsWith(PREFIX)) return value // legacy plaintext

        val parts = value.removePrefix(PREFIX).split(":", limit = 3)
        require(parts.size == 3 && parts[0] == VERSION) { "Unsupported ciphertext format" }

        val iv = parts[1].decodeBase64()?.toByteArray()
            ?: throw IllegalArgumentException("Corrupted ciphertext IV")
        val encrypted = parts[2].decodeBase64()?.toByteArray()
            ?: throw IllegalArgumentException("Corrupted ciphertext payload")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    companion object {
        private const val PREFIX = "enc:"
        private const val VERSION = "v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "mcp_chat_auth_header_key"

        /**
         * Creates (once) and returns the Keystore-backed key. Requires a real
         * Android runtime; callers on JVM test environments inject their own
         * [SecretKey] via the constructor instead.
         */
        private fun getOrCreateKeystoreKey(): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .apply {
                    // StrongBox is optional hardware; fall back silently when absent.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setIsStrongBoxBacked(false)
                    }
                }
                .build()
            generator.init(spec)
            return generator.generateKey()
        }
    }
}
