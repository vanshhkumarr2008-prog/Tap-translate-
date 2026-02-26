package com.tap.translate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Abhi humne layout set nahi kiya hai, isliye crash ho sakta hai
        // Ek simple empty view set karte hain
        setContentView(android.view.View(this))
    }
}
