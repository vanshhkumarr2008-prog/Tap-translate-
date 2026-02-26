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

        createNotificationChannel()
        val notification = Notification.Builder(this, "TAP_CHANNEL")
            .setContentTitle("Tap Translate Pro Running")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()
        startForeground(1, notification)

        if (data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // Step 1: Dictionary download check
            translator.downloadModelIfNeeded().addOnSuccessListener {
                startScanningProcess()
            }
        }
        return START_STICKY
    }

    private fun startScanningProcess() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay("Scan", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, 
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)

        // 1 second baad screen capture karke translate karna
        Handler(Looper.getMainLooper()).postDelayed({ scanScreenNow() }, 1000)
    }

    private fun scanScreenNow() {
        val image = imageReader?.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer = planes[0].buffer
        val bitmap = Bitmap.createBitmap(image.width + (planes[0].rowStride - planes[0].pixelStride * image.width) / planes[0].pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { visionText ->
            if (visionText.text.isNotEmpty()) {
                translator.translate(visionText.text).addOnSuccessListener { hindiText ->
                    showProfessionalResult(hindiText)
                }
            } else {
                Toast.makeText(this, "No English text found on screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProfessionalResult(text: String) {
        removeOldOverlay()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM // Result niche dikhega
        params.y = 100

        overlayView = TextView(this).apply {
            this.text = text
            this.setPadding(50, 50, 50, 50)
            this.setBackgroundColor(Color.parseColor("#E6000000")) // Dark Glass Look
            this.setTextColor(Color.WHITE)
            this.textSize = 20f
            this.gravity = Gravity.CENTER
            this.setOnClickListener { removeOldOverlay() } // Tap to close
        }

        windowManager.addView(overlayView, params)
    }

    private fun removeOldOverlay() {
        overlayView?.let { windowManager.removeView(it); overlayView = null }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("TAP_CHANNEL", "Translate", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOldOverlay()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }
}
