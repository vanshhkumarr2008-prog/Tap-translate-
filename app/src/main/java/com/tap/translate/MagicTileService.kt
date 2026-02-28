package com.tap.translate

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        try {
            // Android 12+ ke liye proper tarika
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // System panel safely collapse hota hai
                collapsePanels()
            } else {
                // Purane Android ke liye fallback
                val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                sendBroadcast(closeIntent)
            }
        } catch (e: Exception) {
            Log.e("MagicTile", "Panel collapse error", e)
        }

        try {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = "MAGIC_TAP"
            }

            // Android O+ me foreground service zaroori
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

        } catch (e: Exception) {
            Log.e("MagicTile", "Service start error", e)
        }
    }
}
