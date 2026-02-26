package com.tap.translate

import android.content.Intent
import android.service.quicksettings.TileService

class TranslationTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, ScreenCaptureService::class.java)
        startService(intent)
    }
}
