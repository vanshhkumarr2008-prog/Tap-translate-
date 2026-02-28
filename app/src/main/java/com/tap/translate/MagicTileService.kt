package com.tap.translate

import android.content.Intent
import android.service.quicksettings.TileService

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // Close notification panel
        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        // Open MainActivity safely
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("FROM_TILE", true)

        startActivityAndCollapse(intent)
    }
}
