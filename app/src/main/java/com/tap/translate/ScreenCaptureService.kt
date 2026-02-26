package com.tap.translate

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = Notification.Builder(this, "TAP_CHANNEL")
            .setContentTitle("Tap Translate is Running")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()

        startForeground(1, notification)

        // Asli Kaam: Screen Read karna
        processScreenText()

        return START_STICKY
    }

    private fun processScreenText() {
        // Abhi hum test kar rahe hain ki kya ML Kit text pehchan pa raha hai
        // Agle step mein hum MediaProjection se LIVE screen ka bitmap yahan bhejenge
        Toast.makeText(this, "Reading Screen Text...", Toast.LENGTH_SHORT).show()
        
        // Sample message for Testing
        println("ML Kit System is analyzing the screen...")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("TAP_CHANNEL", "Scan", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
