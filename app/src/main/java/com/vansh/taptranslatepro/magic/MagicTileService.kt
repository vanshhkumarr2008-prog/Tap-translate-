package com.vansh.taptranslatepro.magic

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        if (qsTile.state == Tile.STATE_INACTIVE) {

            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()

            // Start Overlay
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)

        } else {

            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()

            // Stop Overlay
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
