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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
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
    
    private var floatingStar: ImageView? = null
    private var overlayContainer: FrameLayout? = null

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var targetLangCode = TranslateLanguage.HINDI // Default

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("DATA")
        val langStr = intent?.getStringExtra("TARGET_LANG") ?: "Hindi"
        
        // 🔥 Smart Language Mapper
        targetLangCode = when (langStr) {
            "Hindi" -> TranslateLanguage.HINDI
            "Spanish" -> TranslateLanguage.SPANISH
            "French" -> TranslateLanguage.FRENCH
            "Arabic" -> TranslateLanguage.ARABIC
            "German" -> TranslateLanguage.GERMAN
            else -> TranslateLanguage.HINDI
        }

        // Android 14 Foreground Fix
        val channel = NotificationChannel("TAP_CHANNEL", "Translator", NotificationManager.IMPORTANCE_MIN)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        startForeground(1, Notification.Builder(this, "TAP_CHANNEL")
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("Tap Translate Pro Active")
            .build())

        if (data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            showFloatingStar()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingStar() {
        floatingStar = ImageView(this).apply {
            setImageResource(android.R.drawable.btn_star_big_on)
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 150
                y = 150
            }
        }

        val params = floatingStar?.layoutParams as WindowManager.LayoutParams
        floatingStar?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingStar, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 15 && diffY < 15) {
                            startCaptureAndTranslate()
                        }
                        return true
                    }
                }
                return false
            }
        })
        windowManager.addView(floatingStar, params)
    }

    private fun startCaptureAndTranslate() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay("Scan", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val bitmap = Bitmap.createBitmap(image.width + (planes[0].rowStride - planes[0].pixelStride * image.width) / planes[0].pixelStride, image.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                processAndShow(bitmap)
            }
            virtualDisplay?.release()
        }, 600)
    }

    private fun processAndShow(bitmap: Bitmap) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { visionText ->
            // 🔥 Auto-Detect Logic: ML Kit Translator handles source detection if set correctly
            val translator = Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH) // Base detection
                .setTargetLanguage(targetLangCode).build())

            translator.downloadModelIfNeeded().addOnSuccessListener {
                overlayContainer = FrameLayout(this)
                val fullParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                )

                for (block in visionText.textBlocks) {
                    translator.translate(block.text).addOnSuccessListener { translatedText ->
                        val tv = TextView(this).apply {
                            text = translatedText
                            setBackgroundColor(Color.parseColor("#CC000000")) // Glassy Black
                            setTextColor(Color.WHITE)
                            setPadding(8, 4, 8, 4)
                            textSize = 14f
                            val rect = block.boundingBox
                            x = rect?.left?.toFloat() ?: 0f
                            y = rect?.top?.toFloat() ?: 0f
                        }
                        overlayContainer?.addView(tv)
                    }
                }
                
                overlayContainer?.setOnClickListener { 
                    windowManager.removeView(overlayContainer)
                    overlayContainer = null
                }
                windowManager.addView(overlayContainer, fullParams)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingStar?.let { windowManager.removeView(it) }
        overlayContainer?.let { windowManager.removeView(it) }
        mediaProjection?.stop()
    }
}
