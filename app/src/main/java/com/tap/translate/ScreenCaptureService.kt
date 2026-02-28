package com.tap.translate

import android.annotation.SuppressLint
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
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
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
    
    // 1. DATABASE HELPER KA VARIABLE ADD KIYA ✅
    private lateinit var dbHelper: DatabaseHelper
    
    private var floatingStar: ImageView? = null
    private var overlayContainer: FrameLayout? = null

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var targetLangCode = TranslateLanguage.HINDI

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // 2. DATABASE KO INITIALIZE KIYA ✅
        dbHelper = DatabaseHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("DATA")
        val langStr = intent?.getStringExtra("TARGET_LANG") ?: "Hindi"
        
        targetLangCode = when (langStr) {
            "Spanish" -> TranslateLanguage.SPANISH
            "French" -> TranslateLanguage.FRENCH
            "Arabic" -> TranslateLanguage.ARABIC
            "German" -> TranslateLanguage.GERMAN
            else -> TranslateLanguage.HINDI
        }

        val channelId = "TAP_PRO_CHANNEL"
        val channel = NotificationChannel(channelId, "Tap Translate", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Magic Star 🌟 Active")
            .setContentText("Tap the star on any screen to translate")
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .build()

        startForeground(1, notification)

        if (data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            showFloatingStar()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingStar() {
        if (floatingStar != null) return

        floatingStar = ImageView(this).apply {
            setImageResource(android.R.drawable.btn_star_big_on)
            setBackgroundResource(android.R.drawable.editbox_dropdown_dark_frame)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 500
        }

        floatingStar?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoving = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager.updateViewLayout(floatingStar, params)
                            isMoving = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            Toast.makeText(this@ScreenCaptureService, "Magic Scanning... ✨", Toast.LENGTH_SHORT).show()
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
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * image.width
                
                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                processAndShow(bitmap)
            }
            virtualDisplay?.release()
        }, 500)
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
                if (overlayContainer != null) {
                    windowManager.removeView(overlayContainer)
                    overlayContainer = null
                }

                overlayContainer = FrameLayout(this)
                val fullParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )

                for (block in visionText.textBlocks) {
                    translator.translate(block.text).addOnSuccessListener { translatedText ->
                        
                        // 3. HISTORY MEIN SAVE KARNE KA LOGIC ADD KIYA ✅
                        dbHelper.insertHistory(block.text, translatedText)

                        val tv = TextView(this).apply {
                            text = translatedText
                            setTextColor(Color.YELLOW)
                            setBackgroundColor(Color.parseColor("#99000000"))
                            setPadding(10, 5, 10, 5)
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
            }.addOnFailureListener {
                Toast.makeText(this, "Language model download failed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingStar?.let { windowManager.removeView(it) }
        mediaProjection?.stop()
    }
}
