package se.bambulive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.bambulive.data.ImageStore
import se.bambulive.data.Prefs
import se.bambulive.data.PrinterRepository
import se.bambulive.data.PrinterState
import se.bambulive.data.formatMinutes
import se.bambulive.widget.WidgetUpdater

/**
 * Förgrundstjänst som håller MQTT-anslutningen öppen och uppdaterar widgeten
 * med några sekunders mellanrum – det är det som gör widgeten "live".
 */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var acquired = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(PrinterRepository.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        if (!acquired) {
            acquired = true
            _running.value = true
            PrinterRepository.acquire()
            startUpdating()
        }
        return START_STICKY
    }

    private fun startUpdating() {
        scope.launch {
            var lastJob: String? = null
            PrinterRepository.state.collect { state ->
                WidgetUpdater.push(this@MonitorService, state)
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIF_ID, buildNotification(state))
                if (state.jobName != null && state.jobName != lastJob) {
                    lastJob = state.jobName
                    ImageStore.refreshCover(this@MonitorService)
                    WidgetUpdater.push(this@MonitorService, state)
                }
                delay(UPDATE_INTERVAL_MS)
            }
        }
        // Be skrivaren om full status då och då (cloud skickar annars mest ändringar).
        scope.launch {
            while (true) {
                delay(5 * 60_000L)
                PrinterRepository.requestPushAll()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        if (acquired) PrinterRepository.release()
        _running.value = false
        super.onDestroy()
    }

    private fun buildNotification(s: PrinterState): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Live-widget", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MonitorService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val name = Prefs(this).deviceName ?: "Skrivare"
        val text = buildList {
            add(s.statusLabel)
            s.percent?.takeIf { s.isPrinting }?.let { add("$it %") }
            formatMinutes(s.remainingMin)?.takeIf { s.isPrinting }?.let { add("$it kvar") }
        }.joinToString(" · ")
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(name)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .apply { if (s.isPrinting && s.percent != null) setProgress(100, s.percent, false) }
            .addAction(0, "Stäng av live", stop)
            .build()
    }

    companion object {
        private const val CHANNEL = "live"
        private const val NOTIF_ID = 1
        private const val ACTION_STOP = "stop"
        private const val UPDATE_INTERVAL_MS = 5_000L

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(ctx: Context) =
            ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java))

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, MonitorService::class.java))
        }
    }
}
