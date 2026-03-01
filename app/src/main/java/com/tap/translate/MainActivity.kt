package com.tap.translate

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_CAPTURE = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tile se signal aaya
        if (intent.getBooleanExtra("FROM_TILE", false)) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE)
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = "MAGIC_TAP"
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            
            Handler(Looper.getMainLooper()).postDelayed({
                moveTaskToBack(true)
                finish()
            }, 500)
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            finish() // Agar user ne cancel kiya
        }
    }
}
