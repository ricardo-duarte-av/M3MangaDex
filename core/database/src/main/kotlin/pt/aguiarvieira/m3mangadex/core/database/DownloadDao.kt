package pt.aguiarvieira.m3mangadex.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt")
    fun all(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE mangaId = :mangaId")
    fun forManga(mangaId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE chapterId = :chapterId")
    suspend fun get(chapterId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE mangaId = :mangaId")
    suspend fun forMangaNow(mangaId: String): List<DownloadEntity>

    /** The next chapter to work on: one interrupted mid-download first, then the oldest queued. */
    @Query(
        "SELECT * FROM downloads WHERE state IN ('Downloading', 'Queued') ORDER BY state = 'Queued', createdAt LIMIT 1"
    )
    suspend fun next(): DownloadEntity?

    /** Adds new downloads; chapters already known (queued, done or failed) are left alone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(downloads: List<DownloadEntity>)

    @Upsert
    suspend fun upsert(download: DownloadEntity)

    @Query(
        "UPDATE downloads SET state = :state, pagesDone = :pagesDone, pageCount = :pageCount, sizeBytes = :sizeBytes " +
            "WHERE chapterId = :chapterId",
    )
    suspend fun update(
        chapterId: String,
        state: String,
        pagesDone: Int,
        pageCount: Int,
        sizeBytes: Long,
    )

    @Query("DELETE FROM downloads WHERE chapterId = :chapterId")
    suspend fun delete(chapterId: String)
}
