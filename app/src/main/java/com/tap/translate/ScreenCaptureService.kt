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
import android.os.*
import android.view.*
import android.widget.*
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
    
    // Database ko "Lazy" load karenge taaki app start hote hi hang na ho ✅
    private val dbHelper: DatabaseHelper by lazy { DatabaseHelper(applicationContext) }
    
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
        // --- Sabse Pehle Notification Dikhao (Android 14 Rule) ---
        showNotification()

        val action = intent?.action
        if (action == "MAGIC_TAP") {
            if (mediaProjection != null) {
                // Thoda sa delay panel band hone ke liye
                Handler(Looper.getMainLooper()).postDelayed({ startCaptureAndTranslate() }, 700)
            } else {
                Toast.makeText(this, "Pehle App se Activate karein!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Activity se data aa raha hai
            val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
            val data = intent?.getParcelableExtra<Intent>("DATA")
            val langStr = intent?.getStringExtra("TARGET_LANG") ?: "Hindi"
            
            targetLangCode = getLangCode(langStr)

            if (data != null) {
                val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)
                showFloatingStar()
            }
        }
        return START_STICKY
    }

    private fun showNotification() {
        val channelId = "TAP_PRO_CHANNEL"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Tap Translate", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Tap Translate Active 🌟")
            .setContentText("Tile ya Star se translate karein")
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
        floatingStar = ImageView(this).apply {
            setImageResource(android.R.drawable.btn_star_big_on)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100; y = 500
        }
        
        floatingStar?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) startCaptureAndTranslate()
            true
        }
        windowManager.addView(floatingStar, params)
    }

    private fun startCaptureAndTranslate() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "Capture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val buffer = image.planes[0].buffer
                val pixelStride = image.planes[0].pixelStride
                val rowStride = image.planes[0].rowStride
                val rowPadding = rowStride - pixelStride * image.width
                val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                processAndShow(bitmap)
            }
            virtualDisplay?.release()
        }, 600)
    }

    private fun processAndShow(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage).addOnSuccessListener { visionText ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLangCode)
                .build()
            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded().addOnSuccessListener {
                overlayContainer?.let { windowManager.removeView(it) }
                overlayContainer = FrameLayout(this)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                for (block in visionText.textBlocks) {
                    translator.translate(block.text).addOnSuccessListener { translatedText ->
                        // Database update yahan hoga bina crash ke
                        dbHelper.insertHistory(block.text, translatedText)
                        val tv = TextView(this).apply {
                            text = translatedText
                            setTextColor(Color.YELLOW)
                            setBackgroundColor(Color.parseColor("#99000000"))
                            x = block.boundingBox?.left?.toFloat() ?: 0f
                            y = block.boundingBox?.top?.toFloat() ?: 0f
                        }
                        overlayContainer?.addView(tv)
                    }
                }
                overlayContainer?.setOnClickListener { 
                    windowManager.removeView(overlayContainer)
                    overlayContainer = null
                }
                windowManager.addView(overlayContainer, params)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingStar?.let { windowManager.removeView(it) }
        mediaProjection?.stop()
    }
}
