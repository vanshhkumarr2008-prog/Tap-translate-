package com.tap.translate

import android.content.Intent
import android.service.quicksettings.TileService

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // 1. Notification panel band karne ki koshish (Android 12+ fix) ✅
        try {
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) {
            // Agar crash ho toh skip karein
        }

        // 2. Service ko "MAGIC_TAP" ka signal bhejo
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "MAGIC_TAP"
        }
        startForegroundService(serviceIntent)
    }
}
