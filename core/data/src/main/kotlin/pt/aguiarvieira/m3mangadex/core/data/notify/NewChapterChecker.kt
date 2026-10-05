package pt.aguiarvieira.m3mangadex.core.data.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.R
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.network.MangaDexUserApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks for chapters of followed manga released since the last check and posts a notification per
 * manga (tapping it opens the chapter, or the manga when there are several). Needs an account:
 * the follows feed is MangaDex's.
 */
@Singleton
class NewChapterChecker
    @Inject
    internal constructor(
        @ApplicationContext private val context: Context,
        private val auth: AuthRepository,
        private val userApi: MangaDexUserApi,
        private val preferences: PreferencesDataSource,
    ) {
        /** Returns how many manga were notified about. [since] overrides the last check (testing). */
        suspend fun check(since: Long? = null): Int {
            val prefs = preferences.preferences.first()
            if (!prefs.newChapterNotifications || auth.session.value !is Session.LoggedIn) return 0
            val startedAt = System.currentTimeMillis()
            val last = since ?: preferences.lastChapterCheck.first()
            val feed =
                userApi
                    .followsFeed(
                        prefs.chapterLanguages,
                        prefs.contentRatings,
                        offset = 0,
                        limit = FEED_LIMIT
                    ).items
            val groups = newChapters(feed, last).take(MAX_NOTIFICATIONS)
            if (since == null) preferences.setLastChapterCheck(startedAt)
            if (groups.isEmpty() || !canNotify()) return 0
            AppNotifications.ensureChannels(context)
            groups.forEach(::post)
            return groups.size
        }

        private fun canNotify(): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        @Suppress("MissingPermission") // Checked in canNotify().
        private fun post(group: NewChapters) {
            val newest = group.entries.first()
            val title = newest.manga?.displayTitle(listOf("en")).orEmpty()
            val labels = group.entries.map { label(it.chapter) }
            val single = group.entries.size == 1 && !newest.chapter.isExternal
            val text =
                if (group.entries.size == 1) {
                    listOfNotNull(labels.first(), newest.chapter.title).joinToString(" · ")
                } else {
                    context.resources.getQuantityString(
                        R.plurals.notify_new_chapters,
                        group.entries.size,
                        group.entries.size
                    ) +
                        ": " + labels.joinToString()
                }
            val id = group.mangaId.hashCode()
            val notification =
                NotificationCompat
                    .Builder(context, AppNotifications.NEW_CHAPTERS)
                    .setSmallIcon(AppNotifications.smallIcon)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setWhen(newest.chapter.readableAt?.toEpochMilli() ?: System.currentTimeMillis())
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setGroup(GROUP)
                    .setContentIntent(
                        AppNotifications.openApp(
                            context,
                            requestCode = id,
                            mangaId = group.mangaId,
                            chapterId = newest.chapter.id.takeIf { single },
                        ),
                    ).build()
            NotificationManagerCompat.from(context).notify(TAG, id, notification)
        }

        private fun label(chapter: Chapter) =
            chapter.number?.let { context.getString(R.string.notify_chapter, it) }
                ?: context.getString(R.string.notify_oneshot)

        private companion object {
            const val FEED_LIMIT = 100
            const val MAX_NOTIFICATIONS = 10
            const val TAG = "new-chapters"
            const val GROUP = "pt.aguiarvieira.m3mangadex.NEW_CHAPTERS"
        }
    }
