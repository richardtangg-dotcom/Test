package se.bambulive.widget

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import se.bambulive.MonitorService
import se.bambulive.data.ImageStore
import se.bambulive.data.Prefs
import se.bambulive.data.PrinterRepository
import java.util.concurrent.TimeUnit

/** Uppdaterar widgeten var 15:e minut när live-tjänsten inte är igång. */
class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!Prefs(ctx).hasDevice) return Result.success()
        if (!MonitorService.running.value) {
            val start = System.currentTimeMillis()
            PrinterRepository.acquire()
            try {
                PrinterRepository.requestPushAll()
                withTimeoutOrNull(25_000) {
                    PrinterRepository.state.first { it.updatedAt > start && it.gcodeState != null }
                }
            } finally {
                PrinterRepository.release()
            }
        }
        ImageStore.refreshCover(ctx)
        WidgetUpdater.push(ctx, PrinterRepository.state.value)
        return Result.success()
    }

    companion object {
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "widget-refresh", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build(),
            )
        }

        fun runNow(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "widget-refresh-now", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(constraints).build(),
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork("widget-refresh")
        }
    }
}
