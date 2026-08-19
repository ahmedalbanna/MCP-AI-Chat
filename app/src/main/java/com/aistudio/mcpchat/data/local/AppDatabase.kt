package com.aistudio.mcpchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        McpServerEntity::class,
        McpToolEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        McpLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mcpServerDao(): McpServerDao
    abstract fun mcpToolDao(): McpToolDao
    abstract fun chatDao(): ChatDao
    abstract fun mcpLogDao(): McpLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mcp_chat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
