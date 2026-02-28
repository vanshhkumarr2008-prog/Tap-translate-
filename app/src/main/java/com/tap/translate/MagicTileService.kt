package com.tap.translate

import android.content.Intent
import android.service.quicksettings.TileService

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // 1. Notification bar ko turant band karo ✅
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        sendBroadcast(closeIntent)

        // 2. ScreenCaptureService ko "MAGIC_TAP" ka signal bhejo ✅
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "MAGIC_TAP"
        }
        startForegroundService(serviceIntent)
    }
}
