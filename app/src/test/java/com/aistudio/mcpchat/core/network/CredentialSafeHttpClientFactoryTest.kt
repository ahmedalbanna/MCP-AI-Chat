package com.aistudio.mcpchat.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CredentialSafeHttpClientFactoryTest {

    @Test
    fun `credential clients never follow redirects`() {
        val client = CredentialSafeHttpClientFactory.create(
            connectTimeoutSeconds = 7,
            readTimeoutSeconds = 11,
            writeTimeoutSeconds = 13,
        )

        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertEquals(7_000, client.connectTimeoutMillis)
        assertEquals(11_000, client.readTimeoutMillis)
        assertEquals(13_000, client.writeTimeoutMillis)
    }
}
