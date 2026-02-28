package com.tap.translate

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // 1️⃣ Notification panel close (Safe for all versions)
        try {
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) {
            Log.e("MagicTile", "Panel close error", e)
        }

        // 2️⃣ Start ScreenCaptureService
        try {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
            serviceIntent.action = "MAGIC_TAP"

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
