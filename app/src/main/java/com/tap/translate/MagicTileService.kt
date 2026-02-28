package com.tap.translate

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent

class MagicTileService : TileService() {

    // Jab user Notification bar mein button dekhega
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    // Jab user icon par click karega
    override fun onClick() {
        super.onClick()
        // App khul jayegi taaki user translate start kar sake
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }
}
