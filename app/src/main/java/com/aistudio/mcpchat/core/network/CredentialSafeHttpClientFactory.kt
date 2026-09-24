package com.aistudio.mcpchat.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object CredentialSafeHttpClientFactory {
    fun create(connectTimeoutSeconds: Long, readTimeoutSeconds: Long, writeTimeoutSeconds: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
