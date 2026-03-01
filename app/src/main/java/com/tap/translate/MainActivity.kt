package com.tap.translate

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_CAPTURE = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- TILE SE SIGNAL AANE PAR ---
        if (intent.getBooleanExtra("FROM_TILE", false)) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            
            // Android 14 Fix: Chota delay taaki window background se foreground mein aa sake
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    finish()
                }
            }, 200) 
            return
        }

        // --- NORMAL APP START ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
                    true
                }
                R.id.nav_history -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HistoryFragment()).commit()
                    true
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SettingsFragment()).commit()
                    true
                }
                else -> false
            }
        }
    }

    // --- SCREEN CAPTURE PERMISSION KA RESULT ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Service ko naya Token (Data) bhejo
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    action = "MAGIC_TAP"
                    putExtra("RESULT_CODE", resultCode)
                    putExtra("DATA", data)
                }
                
                ContextCompat.startForegroundService(this, serviceIntent)

                // Service shuru hone ke baad app ko minimize kar do
                Handler(Looper.getMainLooper()).postDelayed({
                    moveTaskToBack(true)
                    finish()
                }, 1000)
            } else {
                // Agar user ne "Cancel" kiya
                finish()
            }
        }
    }
}
