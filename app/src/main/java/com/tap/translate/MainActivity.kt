package com.tap.translate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // 1. Language Selection Setup (Professional Look)
        val languageSpinner: Spinner = Spinner(this) // Agle step mein ise XML mein dalenge
        val languages = arrayOf("English to Hindi", "Hindi to English")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        
        // 2. Activate Button Click
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // Screen Capture ki permission mangna (Professional Pop-up)
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // 3. Permission milte hi Service start karna
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
            serviceIntent.putExtra("RESULT_CODE", resultCode)
            serviceIntent.putExtra("DATA", data)
            
            startForegroundService(serviceIntent)
            
            Toast.makeText(this, "Star 🌟 is now Active in Quick Settings!", Toast.LENGTH_LONG).show()
            
            // App ko background mein bhej dena (Professional feel)
            moveTaskToBack(true) 
        } else {
            Toast.makeText(this, "Permission Denied! App won't work.", Toast.LENGTH_SHORT).show()
        }
    }
}
