package com.vansh.taptranslatepro.magic

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class MagicTileService : TileService() {

    override fun onClick() {
        super.onClick()

        // Tile ko active karo
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()

        // Toast for debug / initial testing
        Toast.makeText(this, "Magic Tile Activated 🚀", Toast.LENGTH_SHORT).show()

        // Future: Call AccessibilityService logic yaha se trigger hoga
    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }
}
