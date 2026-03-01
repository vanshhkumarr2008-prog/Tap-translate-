package com.tap.translate

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
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
    private val dbHelper by lazy { DatabaseHelper(applicationContext) }
    private var floatingStar: ImageView? = null
    private var overlayContainer: FrameLayout? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var targetLangCode = TranslateLanguage.HINDI

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification() 

        if (intent?.action == "MAGIC_TAP") {
            if (mediaProjection == null) {
                // Agar session expire ho gaya toh activity kholo
                val i = Intent(this, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                return START_STICKY
            }
            Handler(Looper.getMainLooper()).postDelayed({ startCaptureAndTranslate() }, 500)
        } else {
            val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
            val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("DATA")
            }

            val langStr = intent?.getStringExtra("TARGET_LANG") ?: "Hindi"
            targetLangCode = getLangCode(langStr)

            if (data != null && resultCode == Activity.RESULT_OK) {
                val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)
                showFloatingStar()
            }
        }
        return START_STICKY
    }

    private fun showNotification() {
        val channelId = "TAP_TRANSLATE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Active Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tap Translate is Ready 🌟")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun getLangCode(lang: String) = when (lang) {
        "Spanish" -> TranslateLanguage.SPANISH
        "French" -> TranslateLanguage.FRENCH
        "Arabic" -> TranslateLanguage.ARABIC
        "German" -> TranslateLanguage.GERMAN
        else -> TranslateLanguage.HINDI
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingStar() {
        if (floatingStar != null) return
        floatingStar = ImageView(this).apply { setImageResource(android.R.drawable.btn_star_big_on) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 500 }
        
        floatingStar?.setOnClickListener { startCaptureAndTranslate() }
        windowManager.addView(floatingStar, params)
    }

    private fun startCaptureAndTranslate() {
        if (mediaProjection == null) return
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * image.width
                
                val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                image.close()
                processAndShow(finalBitmap)
            }
            virtualDisplay?.release()
            imageReader?.close()
        }, 400)
    }

    private fun processAndShow(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage).addOnSuccessListener { visionText ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(targetLangCode).build()
            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded().addOnSuccessListener {
                removeOverlayIfExists()
                overlayContainer = FrameLayout(this)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                for (block in visionText.textBlocks) {
                    translator.translate(block.text).addOnSuccessListener { translated ->
                        try { dbHelper.insertHistory(block.text, translated) } catch (e: Exception) {}
                        val tv = TextView(this).apply {
                            text = translated
                            setTextColor(Color.YELLOW)
                            setBackgroundColor(Color.parseColor("#99000000"))
                            x = block.boundingBox?.left?.toFloat() ?: 0f
                            y = block.boundingBox?.top?.toFloat() ?: 0f
                        }
                        overlayContainer?.addView(tv)
                    }
                }
                overlayContainer?.setOnClickListener { removeOverlayIfExists() }
                windowManager.addView(overlayContainer, params)
            }
        }
    }

    private fun removeOverlayIfExists() {
        overlayContainer?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        overlayContainer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingStar?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        removeOverlayIfExists()
        mediaProjection?.stop()
    }
}
