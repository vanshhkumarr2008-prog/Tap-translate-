package com.tap.translate

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // Screen capture ki permission mangna
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // Permission milte hi Service ko data bhejna
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
            serviceIntent.putExtra("RESULT_CODE", resultCode)
            serviceIntent.putExtra("DATA", data)
            startForegroundService(serviceIntent)
            finish() // App band ho jayegi par service chalti rahegi
        }
    }
}
