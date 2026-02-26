package com.tap.translate

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Yahan asli scanning ka code aayega jo hum agle step mein detail karenge
        return START_STICKY
    }
}
