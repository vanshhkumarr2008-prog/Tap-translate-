package com.tap.translate

import android.service.quicksettings.TileService
import android.content.Intent

class TranslationTileService : TileService() {
    // Jab user panel mein star icon par click karega
    override fun onClick() {
        super.onClick()
        // Ye MainActivity ko kholega taaki hum Screen Capture ki permission le saken
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }
}
