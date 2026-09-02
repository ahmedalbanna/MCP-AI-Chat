package com.aistudio.mcpchat.data.model

import org.json.JSONObject

/**
 * Encodes/decodes the HTTP headers an MCP server requires.
 *
 * Storage format: a JSON object of header name → value, e.g.
 * `{"Authorization":"Bearer x","X-API-Key":"y"}`. Values written before this
 * codec existed keep working as a single header: "Name: value" splits on the
 * first colon, anything else becomes the Authorization header value.
 */
object McpHeaderCodec {

    fun encode(headers: Map<String, String>): String {
        val valid = headers.filterKeys { it.isNotBlank() }
        if (valid.isEmpty()) return ""
        return JSONObject()
            .apply { valid.forEach { (name, value) -> put(name, value) } }
            .toString()
    }

    fun decode(raw: String): Map<String, String> {
        val value = raw.trim()
        if (value.isEmpty()) return emptyMap()

        if (value.startsWith("{")) {
            try {
                val obj = JSONObject(value)
                return buildMap {
                    obj.keys().forEach { key ->
                        val name = key.trim()
                        if (name.isNotEmpty()) put(name, obj.getString(key).trim())
                    }
                }
            } catch (_: Exception) {
                // Corrupt JSON — fall through to single-header handling.
            }
        }
        return singleHeader(value)
    }

    private fun singleHeader(value: String): Map<String, String> {
        val parts = value.split(":", limit = 2)
        val name = parts[0].trim()
        return if (parts.size == 2 && isValidHeaderName(name)) {
            mapOf(name to parts[1].trim())
        } else {
            // Bare value: "Bearer x" or ERPNext-style "token key:secret" whose
            // colon belongs to the VALUE, not a name separator — the whole
            // string is the Authorization value.
            mapOf("Authorization" to value)
        }
    }

    private fun isValidHeaderName(name: String): Boolean = name.isNotEmpty() && HEADER_NAME_REGEX.matches(name)

    // RFC 7230 token chars — no spaces/separators, so "token 4ed7…" (ERPNext)
    // fails the check and its colon is kept as part of the value.
    private val HEADER_NAME_REGEX = Regex("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+")
}
