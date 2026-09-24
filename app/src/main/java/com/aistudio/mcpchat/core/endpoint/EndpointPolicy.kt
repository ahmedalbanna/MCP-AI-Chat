package com.aistudio.mcpchat.core.endpoint

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress
import java.net.URI

enum class EndpointScope {
    SECURE,
    DEVICE_LOCAL,
    PRIVATE_NETWORK,
}

enum class EndpointRejection {
    INVALID_URL,
    UNSUPPORTED_SCHEME,
    URL_CREDENTIALS,
    REMOTE_CLEARTEXT,
    PRIVATE_HTTP_CONFIRMATION_REQUIRED,
}

sealed interface EndpointDecision {
    data class Allowed(
        val normalizedUrl: String,
        val scope: EndpointScope,
        val usesCleartext: Boolean,
    ) : EndpointDecision

    data class RequiresConfirmation(
        val normalizedUrl: String,
        val scope: EndpointScope,
    ) : EndpointDecision

    data class Rejected(
        val reason: EndpointRejection,
        val message: String,
    ) : EndpointDecision
}

val EndpointDecision.requiresCleartextWarning: Boolean
    get() = this is EndpointDecision.RequiresConfirmation ||
        (this is EndpointDecision.Rejected && reason == EndpointRejection.REMOTE_CLEARTEXT)

object EndpointPolicy {
    private val localDnsSuffixes = setOf(".local", ".lan", ".home.arpa", ".internal")
    private val deviceLocalHosts = setOf("localhost", "localhost.localdomain", "10.0.2.2")
    private val invalidUrl = ParsedEndpoint.Rejected(
        EndpointDecision.Rejected(EndpointRejection.INVALID_URL, "Invalid URL"),
    )

    fun authorize(rawUrl: String, allowPrivateHttp: Boolean): EndpointDecision {
        return when (val decision = evaluate(rawUrl)) {
            is EndpointDecision.RequiresConfirmation -> {
                if (allowPrivateHttp) {
                    EndpointDecision.Allowed(
                        normalizedUrl = decision.normalizedUrl,
                        scope = decision.scope,
                        usesCleartext = true,
                    )
                } else {
                    EndpointDecision.Rejected(
                        reason = EndpointRejection.PRIVATE_HTTP_CONFIRMATION_REQUIRED,
                        message = "Private-network HTTP requires explicit confirmation",
                    )
                }
            }

            else -> decision
        }
    }

    fun authorizationFailure(rawUrl: String, allowPrivateHttp: Boolean): String? {
        val decision = authorize(rawUrl, allowPrivateHttp)
        return when (decision) {
            is EndpointDecision.Allowed -> null
            is EndpointDecision.Rejected -> decision.message
            is EndpointDecision.RequiresConfirmation -> error("Authorization must resolve private HTTP")
        }
    }

    fun requireAuthorizedUrl(rawUrl: String, allowPrivateHttp: Boolean): String {
        return when (val decision = authorize(rawUrl, allowPrivateHttp)) {
            is EndpointDecision.Allowed -> decision.normalizedUrl
            is EndpointDecision.Rejected -> throw IllegalArgumentException(decision.message)
            is EndpointDecision.RequiresConfirmation -> error("Authorization must resolve private HTTP")
        }
    }

    fun evaluate(rawUrl: String): EndpointDecision {
        return when (val parsed = parse(rawUrl)) {
            is ParsedEndpoint.Rejected -> parsed.decision
            is ParsedEndpoint.Valid -> evaluateUrl(parsed.url)
        }
    }

    private fun parse(rawUrl: String): ParsedEndpoint {
        val trimmedUrl = rawUrl.trim()
        val uri = runCatching { URI(trimmedUrl) }.getOrNull()
        val scheme = uri?.scheme
        return when {
            uri == null || scheme == null -> invalidUrl
            !scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true) -> {
                ParsedEndpoint.Rejected(
                    EndpointDecision.Rejected(
                        reason = EndpointRejection.UNSUPPORTED_SCHEME,
                        message = "Unsupported URL scheme; use http or https",
                    ),
                )
            }

            else -> trimmedUrl.toHttpUrlOrNull()?.let { ParsedEndpoint.Valid(it) } ?: invalidUrl
        }
    }

    private fun evaluateUrl(url: HttpUrl): EndpointDecision {
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            return EndpointDecision.Rejected(
                reason = EndpointRejection.URL_CREDENTIALS,
                message = "Credentials must not be embedded in endpoint URLs",
            )
        }
        if (url.isHttps) {
            return EndpointDecision.Allowed(
                normalizedUrl = url.toString(),
                scope = EndpointScope.SECURE,
                usesCleartext = false,
            )
        }

        val host = url.host.trimEnd('.').lowercase()
        return when {
            isDeviceLocal(host) -> EndpointDecision.Allowed(
                normalizedUrl = url.toString(),
                scope = EndpointScope.DEVICE_LOCAL,
                usesCleartext = true,
            )

            isPrivateNetwork(host) -> EndpointDecision.RequiresConfirmation(
                normalizedUrl = url.toString(),
                scope = EndpointScope.PRIVATE_NETWORK,
            )

            else -> EndpointDecision.Rejected(
                reason = EndpointRejection.REMOTE_CLEARTEXT,
                message = "Remote HTTP endpoints are not allowed; use HTTPS",
            )
        }
    }

    private fun isDeviceLocal(host: String): Boolean {
        if (host in deviceLocalHosts || host.endsWith(".localhost")) return true
        val address = numericAddress(host) ?: return false
        return address.isLoopbackAddress
    }

    private fun isPrivateNetwork(host: String): Boolean {
        if (localDnsSuffixes.any(host::endsWith)) return true
        val address = numericAddress(host)
        if (address != null) {
            return address.isSiteLocalAddress || address.isLinkLocalAddress || isUniqueLocalIpv6(host)
        }
        return '.' !in host
    }

    private fun numericAddress(host: String): InetAddress? {
        val looksNumeric = host.all { it.isDigit() || it == '.' } || ':' in host
        if (!looksNumeric) return null
        return runCatching { InetAddress.getByName(host) }.getOrNull()
    }

    private fun isUniqueLocalIpv6(host: String): Boolean {
        val normalized = host.substringBefore('%')
        return normalized.startsWith("fc") || normalized.startsWith("fd")
    }

    private sealed interface ParsedEndpoint {
        data class Valid(val url: HttpUrl) : ParsedEndpoint
        data class Rejected(val decision: EndpointDecision.Rejected) : ParsedEndpoint
    }
}
