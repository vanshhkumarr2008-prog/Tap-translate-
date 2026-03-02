package com.vansh.taptranslatepro.magic

import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import com.vansh.taptranslatepro.R

class OverlayService : Service() {

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var resultText: TextView

    private val textReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newText = intent?.getStringExtra("translatedText")
            if (!newText.isNullOrEmpty()) {
                resultText.text = newText
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        registerReceiver(textReceiver, IntentFilter("UPDATE_OVERLAY_TEXT"))

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_layout, null)

        resultText = overlayView!!.findViewById(R.id.txtResult)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 300

        windowManager.addView(overlayView, params)

        addDragFeature()
        addOpenAnimation()

        overlayView!!.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            stopSelf()
        }
    }

    private fun addDragFeature() {
        overlayView?.setOnTouchListener(object : View.OnTouchListener {

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
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun addOpenAnimation() {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 300
        overlayView?.startAnimation(fadeIn)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(textReceiver)
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }
}
