package com.vansh.taptranslatepro.magic

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceProperly()
    }

    private fun startForegroundServiceProperly() {

        val channelId = "TapTranslateChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tap Translate Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tap Translate Running")
            .setContentText("Magic capture service active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}
