package com.banner.logs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LogcatService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID, "Live Logcat", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Simple Logcat background capture" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageFilter = intent?.getStringExtra(EXTRA_PACKAGE) ?: ""
        val filePath = intent?.getStringExtra(EXTRA_FILE_PATH) ?: ""

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (packageFilter.isNotEmpty())
            "Filter: $packageFilter"
        else
            "Writing all logs to live file"

        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Simple Logcat — Live")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n$filePath"))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "simple_logcat_live"
        const val NOTIF_ID = 1001
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_PACKAGE = "package_filter"
    }
}
