package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.Page
import pt.aguiarvieira.m3mangadex.core.network.MangaDexApi

/** Offset paging over a `/manga` query, stopping at the API's 10 000-result window. */
internal class MangaPagingSource(
    private val fetch: suspend (offset: Int, limit: Int) -> Page<Manga>,
) : PagingSource<Int, Manga>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Manga> {
        val offset = params.key ?: 0
        val limit = minOf(params.loadSize, MangaDexApi.MAX_RESULTS - offset)
        if (limit <= 0) return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        return try {
            val page = fetch(offset, limit)
            LoadResult.Page(
                data = page.items,
                prevKey = null,
                nextKey = page.nextOffset?.takeIf { it < MangaDexApi.MAX_RESULTS },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            LoadResult.Error(e)
        }
    }

    // Search results shift as MangaDex updates; a refresh starts over rather than guessing.
    override fun getRefreshKey(state: PagingState<Int, Manga>): Int? = null
}
