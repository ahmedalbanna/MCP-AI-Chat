package com.aistudio.mcpchat.data.model

sealed class ChatException(message: String) : Exception(message) {
    class ApiKeyMissing :
        ChatException("Gemini API key is missing. Configure it in the Secrets panel.")

    class RateLimit(detail: String) :
        ChatException("Rate limit exceeded: $detail")

    class Network(detail: String) :
        ChatException("Network error: $detail")

    class GeminiApi(detail: String) :
        ChatException(detail)
}
