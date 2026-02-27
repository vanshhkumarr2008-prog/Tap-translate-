package com.tap.translate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE = 1000 // Aapka code 1000 use kar raha tha
    private var selectedLanguage = "Hindi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val spinner = findViewById<Spinner>(R.id.languageSpinner)
        val btnActivate = findViewById<Button>(R.id.btnSettings)

        // 1. Spinner Setup (Languages)
        val languages = arrayOf("Hindi", "Spanish", "French", "Arabic", "German")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguage = languages[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnActivate.setOnClickListener {
            // 2. Critical Check: Overlay Permission
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "Pehle 'Display over other apps' on karein", Toast.LENGTH_LONG).show()
            } else {
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
                putExtra("TARGET_LANG", selectedLanguage) // Language pass kar rahe hain
            }
            startForegroundService(serviceIntent)
            
            Toast.makeText(this, "Star 🌟 is now Active!", Toast.LENGTH_SHORT).show()
        }
    }
}
