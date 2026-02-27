package com.tap.translate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // Permission mangne ka professional tarika
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
            serviceIntent.putExtra("RESULT_CODE", resultCode)
            serviceIntent.putExtra("DATA", data)
            startForegroundService(serviceIntent)
            
            Toast.makeText(this, "Magic Active! Use Star 🌟 in Quick Settings", Toast.LENGTH_LONG).show()
        }
    }
}
