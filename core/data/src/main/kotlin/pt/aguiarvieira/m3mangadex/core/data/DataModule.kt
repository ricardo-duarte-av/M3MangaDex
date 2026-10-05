package pt.aguiarvieira.m3mangadex.core.data

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pt.aguiarvieira.m3mangadex.core.database.M3MangaDexDatabase
import pt.aguiarvieira.m3mangadex.core.database.ReadingDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun mangaRepository(impl: DefaultMangaRepository): MangaRepository

    @Binds
    fun readingRepository(impl: DefaultReadingRepository): ReadingRepository

    companion object {
        @Provides
        @Singleton
        fun database(
            @ApplicationContext context: Context,
        ): M3MangaDexDatabase = M3MangaDexDatabase.build(context)

        @Provides
        fun readingDao(database: M3MangaDexDatabase): ReadingDao = database.readingDao()
    }
}
