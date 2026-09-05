package com.aistudio.mcpchat.di

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.aistudio.mcpchat.data.local.AppDatabase
import com.aistudio.mcpchat.data.local.ThemeSettings
import com.aistudio.mcpchat.data.model.ImageAttachment
import com.aistudio.mcpchat.data.network.AiChatService
import com.aistudio.mcpchat.data.network.McpHttpClient
import com.aistudio.mcpchat.data.network.ProviderChatRouter
import com.aistudio.mcpchat.data.repository.AttachmentContentSource
import com.aistudio.mcpchat.data.repository.ChatRepository
import com.aistudio.mcpchat.data.repository.LoadedAttachment
import com.aistudio.mcpchat.data.repository.McpRepository
import com.aistudio.mcpchat.data.security.AiProviderSettings
import com.aistudio.mcpchat.data.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // AI providers (Gemini, OpenAI-compatible, Anthropic) — 60s global default.
        // Per-server tuning is deferred (ROADMAP Phase 10.3, WONTFIX); see README.md.
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("mcp")
    fun provideMcpOkHttpClient(): OkHttpClient {
        // MCP uses shorter timeouts: fast fail on unreachable dev servers.
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMcpHttpClient(@Named("mcp") client: OkHttpClient): McpHttpClient {
        return McpHttpClient(client = client)
    }

    @Provides
    @Singleton
    fun provideAiProviderSettings(@ApplicationContext context: Context): AiProviderSettings {
        return AiProviderSettings(context)
    }

    @Provides
    @Singleton
    fun provideThemeSettings(@ApplicationContext context: Context): ThemeSettings {
        return ThemeSettings(context)
    }

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager()
    }

    /** Routes chat requests to whichever provider the user configured. */
    @Provides
    @Singleton
    fun provideAiChatService(okHttpClient: OkHttpClient, settings: AiProviderSettings): AiChatService {
        // AiProviderSettings.getActive() satisfies the read-only source.
        return ProviderChatRouter(okHttpClient, settings)
    }

    @Provides
    @Singleton
    fun provideMcpRepository(database: AppDatabase, httpClient: McpHttpClient, crypto: CryptoManager): McpRepository {
        return McpRepository(database, httpClient, crypto)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        database: AppDatabase,
        mcpRepository: McpRepository,
        aiService: AiChatService,
        settings: AiProviderSettings,
        @ApplicationContext context: Context,
    ): ChatRepository {
        val attachmentSource = AttachmentContentSource { attachment: ImageAttachment ->
            runCatching {
                val uri = Uri.parse(attachment.uri)
                val mimeType = context.contentResolver.getType(uri) ?: attachment.mimeType
                val bytes = context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
                    ?: return@runCatching null
                LoadedAttachment(mimeType, Base64.encodeToString(bytes, Base64.NO_WRAP))
            }.getOrNull()
        }
        return ChatRepository(database, mcpRepository, aiService, settings, attachmentSource)
    }
}
