package com.aistudio.mcpchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        McpServerEntity::class,
        McpToolEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        ChatMessageFtsEntity::class,
        McpLogEntity::class,
        MemoryNoteEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mcpServerDao(): McpServerDao
    abstract fun mcpToolDao(): McpToolDao
    abstract fun chatDao(): ChatDao
    abstract fun mcpLogDao(): McpLogDao
    abstract fun memoryNoteDao(): MemoryNoteDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        internal val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_sessions ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS memory_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        tags TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        internal val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mcp_servers ADD COLUMN sessionTtlMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentsJson TEXT")
            }
        }

        internal val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId_timestamp` " +
                        "ON `chat_messages` (`sessionId`, `timestamp`)",
                )
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `chat_messages_fts` USING FTS4(" +
                        "`content` TEXT NOT NULL, content=`chat_messages`)",
                )
                // Index rows that predate the FTS table; Room's sync triggers
                // (created at open) only cover writes made after migration.
                db.execSQL("INSERT INTO chat_messages_fts(chat_messages_fts) VALUES('rebuild')")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val current = instance
                    ?: Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "mcp_chat_database",
                    )
                        .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5)
                        .build()
                        .also { created -> instance = created }
                current
            }
        }
    }
}
