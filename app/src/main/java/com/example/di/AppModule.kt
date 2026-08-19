package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.network.GeminiApiService
import com.example.data.network.McpHttpClient
import com.example.data.repository.ChatRepository
import com.example.data.repository.McpRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideMcpHttpClient(): McpHttpClient {
        return McpHttpClient()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        return GeminiApiService()
    }

    @Provides
    @Singleton
    fun provideMcpRepository(
        database: AppDatabase,
        httpClient: McpHttpClient
    ): McpRepository {
        return McpRepository(database, httpClient)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        database: AppDatabase,
        mcpRepository: McpRepository,
        geminiService: GeminiApiService
    ): ChatRepository {
        return ChatRepository(database, mcpRepository, geminiService)
    }
}
