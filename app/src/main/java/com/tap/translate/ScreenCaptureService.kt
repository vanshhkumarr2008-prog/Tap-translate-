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
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
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
    private var overlayView: TextView? = null

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

        // 1. Silent Notification Setup
        val channel = NotificationChannel("TAP_CHANNEL", "Translator", NotificationManager.IMPORTANCE_MIN)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        startForeground(1, Notification.Builder(this, "TAP_CHANNEL")
            .setSmallIcon(android.R.drawable.btn_star_big_on) // Star Logo
            .setContentTitle("Tap Translate Pro is Ready")
            .build())

        if (data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // 2. Download Hindi Dictionary silently
            translator.downloadModelIfNeeded().addOnSuccessListener {
                startScanning()
            }
        }
        return START_STICKY
    }

    private fun startScanning() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay("Scan", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, 
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)

        // Magic: 1 second wait karke screen read karna
        Handler(Looper.getMainLooper()).postDelayed({ scanScreen() }, 1000)
    }

    private fun scanScreen() {
        val image = imageReader?.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer = planes[0].buffer
        val bitmap = Bitmap.createBitmap(image.width + (planes[0].rowStride - planes[0].pixelStride * image.width) / planes[0].pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { visionText ->
            if (visionText.text.isNotEmpty()) {
                translator.translate(visionText.text).addOnSuccessListener { translated ->
                    showSmartOverlay(translated) // Professional Overlay
                }
            }
        }
    }

    private fun showSmartOverlay(text: String) {
        removeOldOverlay()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        overlayView = TextView(this).apply {
            this.text = text
            this.setTextColor(Color.YELLOW) // High Visibility
            this.textSize = 24f
            this.setShadowLayer(5f, 0f, 0f, Color.BLACK)
            this.setPadding(40, 40, 40, 40)
            this.setBackgroundColor(Color.parseColor("#99000000")) // Smart Dark Glass
            this.gravity = Gravity.CENTER
            this.setOnClickListener { removeOldOverlay() } // Tap to Close
        }

        windowManager.addView(overlayView, params)
    }

    private fun removeOldOverlay() {
        overlayView?.let { windowManager.removeView(it); overlayView = null }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOldOverlay()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }
}
