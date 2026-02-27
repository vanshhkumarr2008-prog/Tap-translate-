package com.tap.translate

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var windowManager: WindowManager
    private var resultView: TextView? = null

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val translator = Translation.getClient(TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.HINDI)
        .build())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("DATA")

        // 1. Notification ko Silent banana
        createSilentNotification()

        if (data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // Model download hone ke baad scan shuru karein
            translator.downloadModelIfNeeded().addOnSuccessListener {
                startScreenCapture()
            }.addOnFailureListener {
                Toast.makeText(this, "Language model download failed!", Toast.LENGTH_SHORT).show()
            }
        }
        return START_STICKY
    }

    private fun startScreenCapture() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay("Scan", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, 
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)

        // 1 second baad screen scan karna
        Handler(Looper.getMainLooper()).postDelayed({ scanNow() }, 1000)
    }

    private fun scanNow() {
        val image = imageReader?.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer = planes[0].buffer
        val bitmap = Bitmap.createBitmap(image.width + (planes[0].rowStride - planes[0].pixelStride * image.width) / planes[0].pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { visionText ->
            if (visionText.text.isNotEmpty()) {
                translator.translate(visionText.text).addOnSuccessListener { hindiText ->
                    showResultOnScreen(hindiText)
                }
            } else {
                Toast.makeText(this, "No text found to translate", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResultOnScreen(text: String) {
        // Purana result hatao
        if (resultView != null) windowManager.removeView(resultView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        
        // Professional Result Box
        resultView = TextView(this).apply {
            this.text = text
            this.setPadding(40, 60, 40, 60)
            this.setBackgroundColor(Color.parseColor("#EE000000")) // Premium Dark
            this.setTextColor(Color.WHITE)
            this.textSize = 20f
            this.gravity = Gravity.CENTER
            this.setOnClickListener { 
                windowManager.removeView(this)
                resultView = null
            }
        }
        windowManager.addView(resultView, params)
    }

    private fun createSilentNotification() {
        val channelId = "TAP_CHANNEL"
        val channel = NotificationChannel(channelId, "Translator", NotificationManager.IMPORTANCE_MIN)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Tap Translate Active")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }
}
