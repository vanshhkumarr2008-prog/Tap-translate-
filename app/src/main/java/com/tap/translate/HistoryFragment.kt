package com.tap.translate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.historyList)
        
        val dbHelper = DatabaseHelper(requireContext())
        val historyData = mutableListOf<Pair<String, String>>()
        
        // Database se data nikalna
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT original, translated FROM history ORDER BY id DESC", null)
        
        if (cursor.moveToFirst()) {
            do {
                historyData.add(Pair(cursor.getString(0), cursor.getString(1)))
            } while (cursor.moveToNext())
        }
        cursor.close()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = HistoryAdapter(historyData)
        
        return view
    }
}
