package com.tap.translate

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Naya layout load ho raha hai jisme fragment_container hai
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Pehla Page (Home) load karo jab app khule
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        // Niche wale buttons ke click handle karna
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "History Coming Soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings Coming Soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}
