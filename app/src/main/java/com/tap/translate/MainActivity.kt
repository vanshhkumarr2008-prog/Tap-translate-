package com.tap.translate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Overlay Permission Check
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, 123)
            Toast.makeText(this, "Please allow 'Display over other apps' to use 🌟 icon", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Permission already granted! Add the 🌟 icon from notification panel", Toast.LENGTH_LONG).show()
            finish() 
        }
    }
}
