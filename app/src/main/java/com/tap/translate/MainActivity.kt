package com.tap.translate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- TILE SIGNAL HANDLING ---
        if (intent.getBooleanExtra("FROM_TILE", false)) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
            serviceIntent.action = "MAGIC_TAP"
            
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Android 14 Fix: Activity ko turant band mat karo
            Handler(Looper.getMainLooper()).postDelayed({
                moveTaskToBack(true)
                finish() 
            }, 800) 
            return 
        }

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
}
