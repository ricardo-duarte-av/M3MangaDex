package pt.aguiarvieira.m3mangadex.core.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * The app's own data: reading progress and per-manga settings. Unlike a cache it holds things
 * only this device knows (until sync in M4), so schema changes need real migrations, never a
 * destructive fallback.
 */
@Database(
    entities = [ChapterProgressEntity::class, MangaSettingsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class M3MangaDexDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao

    companion object {
        /** [name] null builds an in-memory database (tests). */
        fun build(
            context: Context,
            name: String? = "m3mangadex.db",
            driver: SQLiteDriver = BundledSQLiteDriver(),
        ): M3MangaDexDatabase {
            val builder =
                if (name == null) {
                    Room.inMemoryDatabaseBuilder(context, M3MangaDexDatabase::class.java)
                } else {
                    Room.databaseBuilder(context, M3MangaDexDatabase::class.java, name)
                }
            return builder
                .setDriver(driver)
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}
