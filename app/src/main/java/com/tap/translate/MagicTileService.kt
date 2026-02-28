package com.tap.translate

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // 1. Notification panel band karna (Zaroori hai taaki screen capture saaf ho)
        try {
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) { }

        // 2. Service ko "MAGIC_TAP" ka signal bhejna
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "MAGIC_TAP"
        }

        // Android 12, 13, aur 14 ke liye sabse safe tarika ✅
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Agar system allow nahi kar raha, toh yahan crash nahi hoga
            e.printStackTrace()
        }
    }
}
