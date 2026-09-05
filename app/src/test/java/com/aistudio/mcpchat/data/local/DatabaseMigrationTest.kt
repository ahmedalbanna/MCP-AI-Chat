package com.aistudio.mcpchat.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a real version-1 database file (exact v1 DDL as exported by Room),
 * then opens it through Room with [AppDatabase.migration1To2] — Room itself
 * validates the migrated schema against the v2 entities, so any future entity
 * drift fails this test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dbFile: File

    // Exact v1 DDL (matches schemas/…/1.json).
    private val v1Ddl = listOf(
        "CREATE TABLE IF NOT EXISTS `mcp_servers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
            "`description` TEXT NOT NULL, `endpointUrl` TEXT NOT NULL, `authHeader` TEXT NOT NULL, " +
            "`isEnabled` INTEGER NOT NULL, `isPreset` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
            "`latencyMs` INTEGER NOT NULL, `protocolVersion` TEXT NOT NULL, `toolsCount` INTEGER NOT NULL, " +
            "`lastSyncTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `mcp_tools` (`id` TEXT NOT NULL, `serverId` TEXT NOT NULL, " +
            "`serverName` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, " +
            "`inputSchemaJson` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `chat_sessions` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
            "`model` TEXT NOT NULL, `systemPrompt` TEXT NOT NULL, `enabledServerIdsJson` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, " +
            "`role` TEXT NOT NULL, `content` TEXT NOT NULL, `toolCallsJson` TEXT, " +
            "`latencyMs` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `isError` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `mcp_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`serverId` TEXT NOT NULL, `serverName` TEXT NOT NULL, `toolName` TEXT NOT NULL, " +
            "`requestPayload` TEXT NOT NULL, `responsePayload` TEXT NOT NULL, `isSuccess` INTEGER NOT NULL, " +
            "`latencyMs` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `errorMessage` TEXT)",
    )

    @Before
    fun setUp() {
        dbFile = context.getDatabasePath(DB_NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        // Create the raw v1 database: user_version=1 + v1 tables + seed data.
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.version = 1
            v1Ddl.forEach(db::execSQL)
            db.execSQL(
                """
                INSERT INTO chat_sessions
                    (id, title, model, systemPrompt, enabledServerIdsJson, createdAt, updatedAt)
                VALUES ('s1', 'Legacy Session', 'gemini-3.5-flash', '', '[]', 1000, 2000)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO chat_messages
                    (id, sessionId, role, content, toolCallsJson, latencyMs, timestamp, isError)
                VALUES ('m1', 's1', 'user', 'hello from v1', NULL, 0, 1000, 0)
                """.trimIndent(),
            )
        }
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun migrate1To2_addsIsPinnedAndMemoryNotes_preservingData() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.migration1To2, AppDatabase.migration2To3, AppDatabase.migration3To4)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = db.chatDao()

            val migrated = dao.getSessionById("s1")
            assertNotNull(migrated)
            assertEquals("Legacy Session", migrated?.title)
            assertFalse(migrated?.isPinned == true)

            val legacyMessage = dao.getMessagesList("s1").singleOrNull()
            assertNotNull(legacyMessage)
            assertEquals("hello from v1", legacyMessage?.content)
            // v4 added the column with NULL for pre-existing rows.
            assertEquals(null, legacyMessage?.attachmentsJson)

            val noteId = db.memoryNoteDao().insertNote(
                MemoryNoteEntity(content = "post-migration note"),
            )
            assertTrue(noteId > 0)

            assertEquals(4, db.openHelper.writableDatabase.version)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test-db"
    }
}
