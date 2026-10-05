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

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
