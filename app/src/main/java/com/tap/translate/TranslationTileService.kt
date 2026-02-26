package com.tap.translate

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class TranslationTileService : TileService() {

    // Jab tile notification panel mein dikhayi degi
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE // Isse icon hamesha chamkega (Active rahega)
        tile.updateTile()
    }

    // Sabse important: Jab aap Star Icon par TAP karenge
    override fun onClick() {
        super.onClick()

        // 1. User ko batana ki kaam shuru ho gaya hai
        Toast.makeText(this, "Starting Tap Translate...", Toast.LENGTH_SHORT).show()

        // 2. ScreenCaptureService ko chalu karna jo screen read karegi
        val intent = Intent(this, ScreenCaptureService::class.java)
        
        // Android 8.0+ ke liye foreground service zaroori hai
        startForegroundService(intent)

        // 3. Notification panel ko apne aap band kar dena (Collapse)
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        sendBroadcast(closeIntent)
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}
