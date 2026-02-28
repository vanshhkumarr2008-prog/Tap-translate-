package com.tap.translate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        val btnClear = view.findViewById<Button>(R.id.btnClearHistory)
        val dbHelper = DatabaseHelper(requireContext())

        btnClear.setOnClickListener {
            val db = dbHelper.writableDatabase
            db.execSQL("DELETE FROM history")
            db.close()
            Toast.makeText(requireContext(), "History Cleared! 🗑️", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
