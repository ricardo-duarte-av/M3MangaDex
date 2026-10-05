package pt.aguiarvieira.m3mangadex.core.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import pt.aguiarvieira.m3mangadex.core.data.R

/** The app's notification channels, and how a notification opens the app at a manga or chapter. */
object AppNotifications {
    const val DOWNLOADS = "downloads"
    const val NEW_CHAPTERS = "new_chapters"

    /** Extras on the launch intent: open this manga (and, with [EXTRA_CHAPTER_ID], this chapter). */
    const val EXTRA_MANGA_ID = "pt.aguiarvieira.m3mangadex.MANGA_ID"
    const val EXTRA_CHAPTER_ID = "pt.aguiarvieira.m3mangadex.CHAPTER_ID"

    val smallIcon: Int get() = R.drawable.ic_stat_m3mangadex

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    DOWNLOADS,
                    context.getString(R.string.channel_downloads),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.channel_downloads_description)
                },
                NotificationChannel(
                    NEW_CHAPTERS,
                    context.getString(R.string.channel_new_chapters),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.channel_new_chapters_description) },
            ),
        )
    }

    /** Opens the app (its launcher activity), optionally at [mangaId]'s details or [chapterId] in the reader. */
    fun openApp(
        context: Context,
        requestCode: Int,
        mangaId: String? = null,
        chapterId: String? = null,
    ): PendingIntent {
        val intent =
            (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                mangaId?.let { putExtra(EXTRA_MANGA_ID, it) }
                chapterId?.let { putExtra(EXTRA_CHAPTER_ID, it) }
            }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
