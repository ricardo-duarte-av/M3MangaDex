package pt.aguiarvieira.m3mangadex.core.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import pt.aguiarvieira.m3mangadex.core.data.R
import pt.aguiarvieira.m3mangadex.core.data.notify.AppNotifications
import pt.aguiarvieira.m3mangadex.core.model.Download

/** Runs the [DownloadEngine] in a foreground service, with a progress notification. */
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Dependencies {
        fun engine(): DownloadEngine
    }

    override suspend fun doWork(): Result {
        val engine = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).engine()
        AppNotifications.ensureChannels(applicationContext)
        // May be refused when the app is in the background; the work still runs, just quietly.
        runCatching { setForeground(foregroundInfo(null)) }
        var lastUpdate = 0L
        engine.run { download ->
            val now = System.currentTimeMillis()
            if (now - lastUpdate > UPDATE_INTERVAL_MILLIS || download.pagesDone == download.pageCount) {
                lastUpdate = now
                runCatching { setForeground(foregroundInfo(download)) }
            }
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(null)

    private fun foregroundInfo(download: Download?): ForegroundInfo {
        val context = applicationContext
        val title =
            download?.let { context.getString(R.string.download_running, it.mangaTitle) }
                ?: context.getString(R.string.download_starting)
        val chapter =
            download?.chapterNumber?.let { context.getString(R.string.notify_chapter, it) }
                ?: context.getString(R.string.notify_oneshot)
        val notification =
            NotificationCompat
                .Builder(context, AppNotifications.DOWNLOADS)
                .setSmallIcon(AppNotifications.smallIcon)
                .setContentTitle(title)
                .apply {
                    if (download != null) {
                        setContentText(
                            context.getString(
                                R.string.download_progress,
                                chapter,
                                download.pagesDone,
                                download.pageCount
                            )
                        )
                        setProgress(download.pageCount, download.pagesDone, false)
                    } else {
                        setProgress(0, 0, true)
                    }
                }.setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(AppNotifications.openApp(context, REQUEST_CODE))
                .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        private const val NAME = "downloads"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE = 1001
        private const val UPDATE_INTERVAL_MILLIS = 500L

        /**
         * Makes sure the queue is being worked on. Appends: if a run is just finishing, the next one
         * picks up whatever was queued meanwhile.
         */
        fun start(context: Context) {
            val request =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
