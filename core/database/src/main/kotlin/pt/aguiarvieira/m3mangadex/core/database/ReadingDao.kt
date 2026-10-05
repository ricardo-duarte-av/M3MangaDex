package pt.aguiarvieira.m3mangadex.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM chapter_progress WHERE mangaId = :mangaId")
    fun progress(mangaId: String): Flow<List<ChapterProgressEntity>>

    @Query("SELECT * FROM chapter_progress WHERE mangaId = :mangaId ORDER BY readAt DESC LIMIT 1")
    fun lastRead(mangaId: String): Flow<ChapterProgressEntity?>

    @Query("SELECT * FROM chapter_progress WHERE chapterId = :chapterId")
    suspend fun progressOf(chapterId: String): ChapterProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: ChapterProgressEntity)

    @Query("SELECT readerMode FROM manga_settings WHERE mangaId = :mangaId")
    fun readerMode(mangaId: String): Flow<String?>

    @Query("SELECT * FROM manga_settings WHERE mangaId = :mangaId")
    fun settings(mangaId: String): Flow<MangaSettingsEntity?>

    @Query("SELECT * FROM manga_settings WHERE mangaId = :mangaId")
    suspend fun settingsNow(mangaId: String): MangaSettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: MangaSettingsEntity)
}
