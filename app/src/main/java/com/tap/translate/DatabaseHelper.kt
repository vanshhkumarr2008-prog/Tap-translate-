package com.tap.translate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "TranslationDB", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, original TEXT, translated TEXT, time DATETIME DEFAULT CURRENT_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }

    fun insertHistory(original: String, translated: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("original", original)
            put("translated", translated)
        }
        db.insert("history", null, values)
        db.close()
    }
}
