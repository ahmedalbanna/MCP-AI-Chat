package com.aistudio.mcpchat.data.security

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private lateinit var crypto: CryptoManager

    @Before
    fun setUp() {
        val key: SecretKey = KeyGenerator.getInstance("AES")
            .apply { init(256) }
            .generateKey()
        crypto = CryptoManager(keyProvider = { key })
    }

    @Test
    fun `encrypt decrypt round trips`() {
        val secret = "Bearer super-secret-token-123"
        assertEquals(secret, crypto.decrypt(crypto.encrypt(secret)))
    }

    @Test
    fun `encrypt produces prefixed ciphertext distinct from plaintext`() {
        val secret = "Bearer another-secret"
        val encrypted = crypto.encrypt(secret)

        assertTrue(encrypted.startsWith("enc:v1:"))
        assertNotEquals(secret, encrypted)
        assertTrue(!encrypted.contains(secret))
    }

    @Test
    fun `blank values pass through unchanged`() {
        assertEquals("", crypto.encrypt(""))
        assertEquals("", crypto.decrypt(""))
    }

    @Test
    fun `legacy plaintext is returned as-is on decrypt`() {
        // Rows written before encryption existed must keep working.
        assertEquals("Bearer legacy", crypto.decrypt("Bearer legacy"))
    }

    @Test
    fun `already encrypted value is not double encrypted`() {
        val once = crypto.encrypt("token")
        assertEquals(once, crypto.encrypt(once))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed ciphertext throws`() {
        crypto.decrypt("enc:v1:not-enough-parts")
    }
}
