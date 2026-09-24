package com.aistudio.mcpchat.core.endpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointPolicyTest {

    @Test
    fun `https endpoints are allowed`() {
        val decision = EndpointPolicy.evaluate("  https://api.example.com/v1  ")

        assertEquals(
            EndpointDecision.Allowed(
                normalizedUrl = "https://api.example.com/v1",
                scope = EndpointScope.SECURE,
                usesCleartext = false,
            ),
            decision,
        )
    }

    @Test
    fun `device-local cleartext endpoints are allowed without confirmation`() {
        val expectedHosts = listOf(
            "http://localhost:11434/v1",
            "http://127.0.0.1:3000/mcp",
            "http://[::1]:3000/mcp",
            "http://10.0.2.2:3000/mcp",
        )

        expectedHosts.forEach { endpoint ->
            val decision = EndpointPolicy.evaluate(endpoint)
            assertTrue("$endpoint should be allowed", decision is EndpointDecision.Allowed)
            assertEquals(EndpointScope.DEVICE_LOCAL, (decision as EndpointDecision.Allowed).scope)
            assertTrue(decision.usesCleartext)
        }
    }

    @Test
    fun `private-network cleartext endpoints require explicit confirmation`() {
        val expectedHosts = listOf(
            "http://192.168.1.20:8000/v1",
            "http://10.1.2.3/mcp",
            "http://172.20.0.4/mcp",
            "http://printer.local/mcp",
            "http://mcp.home.arpa/mcp",
            "http://mcp.internal/mcp",
            "http://mcp-server/mcp",
        )

        expectedHosts.forEach { endpoint ->
            val decision = EndpointPolicy.evaluate(endpoint)
            assertTrue("$endpoint should require confirmation", decision is EndpointDecision.RequiresConfirmation)
            assertEquals(EndpointScope.PRIVATE_NETWORK, (decision as EndpointDecision.RequiresConfirmation).scope)
        }
    }

    @Test
    fun `public cleartext endpoints are rejected`() {
        listOf(
            "http://api.example.com/v1",
            "http://8.8.8.8/v1",
            "http://[2001:4860:4860::8888]/v1",
        ).forEach { endpoint ->
            assertEquals(
                EndpointDecision.Rejected(
                    reason = EndpointRejection.REMOTE_CLEARTEXT,
                    message = "Remote HTTP endpoints are not allowed; use HTTPS",
                ),
                EndpointPolicy.evaluate(endpoint),
            )
        }
    }

    @Test
    fun `malformed and unsupported endpoints are rejected`() {
        listOf(
            "not-a-url" to EndpointRejection.INVALID_URL,
            "http://" to EndpointRejection.INVALID_URL,
            "ftp://example.com" to EndpointRejection.UNSUPPORTED_SCHEME,
        ).forEach { (endpoint, expectedReason) ->
            val decision = EndpointPolicy.evaluate(endpoint)
            assertTrue("$endpoint should be rejected", decision is EndpointDecision.Rejected)
            assertEquals(expectedReason, (decision as EndpointDecision.Rejected).reason)
        }
    }

    @Test
    fun `private cleartext authorization requires a persisted grant`() {
        val endpoint = "http://192.168.1.20:8000/v1"

        val denied = EndpointPolicy.authorize(endpoint, allowPrivateHttp = false)
        val allowed = EndpointPolicy.authorize(endpoint, allowPrivateHttp = true)

        assertEquals(
            EndpointDecision.Rejected(
                reason = EndpointRejection.PRIVATE_HTTP_CONFIRMATION_REQUIRED,
                message = "Private-network HTTP requires explicit confirmation",
            ),
            denied,
        )
        assertEquals(
            EndpointDecision.Allowed(
                normalizedUrl = endpoint,
                scope = EndpointScope.PRIVATE_NETWORK,
                usesCleartext = true,
            ),
            allowed,
        )
    }

    @Test
    fun `credentials embedded in endpoint URLs are rejected`() {
        val decision = EndpointPolicy.evaluate("https://user:secret@example.com/v1")

        assertEquals(EndpointRejection.URL_CREDENTIALS, (decision as EndpointDecision.Rejected).reason)
    }
}
