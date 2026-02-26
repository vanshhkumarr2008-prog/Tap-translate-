package com.tap.translate

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    // 1. Translation Settings (English to Hindi)
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.HINDI)
        .build()
    
    private val translator = Translation.getClient(options)

    override fun onCreate() {
        super.onCreate()
        // Hindi Model Download karna (sirf ek baar internet chahiye hoga)
        val conditions = DownloadConditions.Builder().requireWifi().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { Log.d("TAP", "Hindi Model Ready") }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Yahan Screenshot se text nikalne ka process shuru hoga
        // Abhi ke liye hum sirf dimaag (ML Kit) set kar rahe hain
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
    }
}
