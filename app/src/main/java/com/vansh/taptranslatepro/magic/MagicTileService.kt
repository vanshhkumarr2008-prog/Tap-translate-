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
            startService(Intent(this, OverlayService::class.java))

            // Start Screen Capture Service
            startService(Intent(this, ScreenCaptureService::class.java))

        } else {

            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()

            // Stop Overlay
            stopService(Intent(this, OverlayService::class.java))

            // Stop Screen Capture
            stopService(Intent(this, ScreenCaptureService::class.java))
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
