package com.tap.translate

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingIcon: ImageView

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingIcon = ImageView(this)
        
        // Icon ke liye koi bhi system icon use kar lete hain
        floatingIcon.setImageResource(android.R.drawable.ic_menu_search)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        // Bubble ko touch karke move karne ka logic
        floatingIcon.setOnTouchListener(object : View.OnTouchListener {
            private var lastX: Int = 0
            private var lastY: Int = 0
            private var initialX: Int = 0
            private var initialY: Int = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX.toInt() - lastX)
                        params.y = initialY + (event.rawY.toInt() - lastY)
                        windowManager.updateViewLayout(floatingIcon, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Yahan screen reading ka feature trigger hoga
                        Toast.makeText(this@FloatingService, "Reading Screen...", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingIcon, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingIcon.isInitialized) windowManager.removeView(floatingIcon)
    }
}
