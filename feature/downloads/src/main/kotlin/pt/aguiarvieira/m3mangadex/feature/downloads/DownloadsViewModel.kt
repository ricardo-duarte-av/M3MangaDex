package pt.aguiarvieira.m3mangadex.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.data.download.DownloadRepository
import pt.aguiarvieira.m3mangadex.core.model.ChapterOrder
import pt.aguiarvieira.m3mangadex.core.model.Download
import javax.inject.Inject

/** One manga's downloads, in reading order. */
data class DownloadGroup(
    val mangaId: String,
    val title: String,
    val coverUrl: String?,
    val downloads: List<Download>,
) {
    val sizeBytes: Long get() = downloads.sumOf { it.sizeBytes }
}

@HiltViewModel
class DownloadsViewModel
    @Inject
    constructor(
        private val repository: DownloadRepository,
    ) : ViewModel() {
        /** null until loaded. */
        val groups: StateFlow<List<DownloadGroup>?> =
            repository.downloads
                .map { downloads ->
                    downloads
                        .groupBy { it.mangaId }
                        .map { (mangaId, items) ->
                            val ordered = items.sortedWith(compareBy(ChapterOrder) { it.toChapter() })
                            DownloadGroup(mangaId, items.first().mangaTitle, items.first().coverUrl, ordered)
                        }.sortedBy { it.title.lowercase() }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        fun delete(chapterId: String) {
            viewModelScope.launch { repository.delete(chapterId) }
        }

        fun retry(chapterId: String) {
            viewModelScope.launch { repository.retry(chapterId) }
        }

        fun deleteManga(mangaId: String) {
            viewModelScope.launch { repository.deleteManga(mangaId) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
