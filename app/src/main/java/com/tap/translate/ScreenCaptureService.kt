package com.tap.translate

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class ScreenCaptureService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Screen Translation Started!", Toast.LENGTH_SHORT).show()
        // Yahan future mein hum translation logic add karenge
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
