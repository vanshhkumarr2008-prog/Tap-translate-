package com.tap.translate

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("DATA")

        createNotificationChannel()
        val notification = Notification.Builder(this, "TAP_CHANNEL")
            .setContentTitle("Tap Translate Active")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()
        startForeground(1, notification)

        if (data != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            startScreenCapture()
        }

        return START_STICKY
    }

    private fun startScreenCapture() {
        val metrics = resources.displayMetrics
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val realMetrics = DisplayMetrics()
        display.getRealMetrics(realMetrics)

        imageReader = ImageReader.newInstance(realMetrics.widthPixels, realMetrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            realMetrics.widthPixels, realMetrics.heightPixels, realMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        // 2 second baad screen read karna
        Handler(Looper.getMainLooper()).postDelayed({
            captureAndRecognize()
        }, 2000)
    }

    private fun captureAndRecognize() {
        val image = imageReader?.acquireLatestImage()
        if (image != null) {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // ML Kit se text pehchanna
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text
                    if (detectedText.isNotEmpty()) {
                        // Abhi ke liye text ko Toast mein dikhayenge
                        Toast.makeText(this, "Detected: $detectedText", Toast.LENGTH_LONG).show()
                        // NEXT STEP: Yahan Hindi translation logic aayega
                    } else {
                        Toast.makeText(this, "No text found on screen", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Scanning failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("TAP_CHANNEL", "Scan", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }
}
