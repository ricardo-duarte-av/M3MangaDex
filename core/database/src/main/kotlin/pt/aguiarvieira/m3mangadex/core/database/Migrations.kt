package pt.aguiarvieira.m3mangadex.core.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v2: per-manga extra chapter languages. */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE manga_settings ADD COLUMN extraLanguages TEXT DEFAULT NULL")
        }
    }

/** v3: offline downloads. */
internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `downloads` (`chapterId` TEXT NOT NULL, `mangaId` TEXT NOT NULL, " +
                    "`mangaTitle` TEXT NOT NULL, `coverUrl` TEXT, `chapterNumber` TEXT, `chapterTitle` TEXT, " +
                    "`language` TEXT NOT NULL, `state` TEXT NOT NULL, `pagesDone` INTEGER NOT NULL, " +
                    "`pageCount` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`chapterId`))",
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_mangaId` ON `downloads` (`mangaId`)")
        }
    }

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
