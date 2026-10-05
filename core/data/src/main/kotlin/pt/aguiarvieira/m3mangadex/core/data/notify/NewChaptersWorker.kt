package pt.aguiarvieira.m3mangadex.core.data.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Runs [NewChapterChecker] every few hours, while online. */
class NewChaptersWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Dependencies {
        fun checker(): NewChapterChecker
    }

    override suspend fun doWork(): Result {
        val checker = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).checker()
        val since = inputData.getLong(KEY_SINCE, -1L).takeIf { it >= 0 }
        return try {
            checker.check(since)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC = "new-chapters"
        private const val NOW = "new-chapters-now"
        private const val KEY_SINCE = "since"
        private const val PERIOD_HOURS = 2L

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Schedules (or cancels) the periodic check. */
        fun schedule(
            context: Context,
            enabled: Boolean,
        ) {
            val work = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
            if (!enabled) {
                work.cancelUniqueWork(PERIODIC)
                return
            }
            val request =
                PeriodicWorkRequestBuilder<NewChaptersWorker>(
                    PERIOD_HOURS,
                    TimeUnit.HOURS
                ).setConstraints(online).build()
            work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** One check right away, looking back [sinceMillis] (epoch) instead of to the last check. */
        fun checkNow(
            context: Context,
            sinceMillis: Long,
        ) {
            val request =
                OneTimeWorkRequestBuilder<NewChaptersWorker>()
                    .setConstraints(online)
                    .setInputData(workDataOf(KEY_SINCE to sinceMillis))
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
