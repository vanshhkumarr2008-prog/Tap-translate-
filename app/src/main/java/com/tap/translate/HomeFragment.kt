package com.tap.translate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE = 1000
    private var selectedLanguage = "Hindi"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val spinner = view.findViewById<Spinner>(R.id.languageSpinner)
        val btnActivate = view.findViewById<Button>(R.id.btnActivate)

        projectionManager = requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val languages = arrayOf("Hindi", "Spanish", "French", "Arabic", "German")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedLanguage = languages[pos]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnActivate.setOnClickListener {
            if (!Settings.canDrawOverlays(requireContext())) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
                startActivity(intent)
            } else {
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
            }
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            
            // Service start karne ka sahi tarika ✅
            val serviceIntent = Intent(requireContext(), ScreenCaptureService::class.java).apply {
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
                putExtra("TARGET_LANG", selectedLanguage)
            }
            requireContext().startForegroundService(serviceIntent)
            
            // Instruction Toast
            Toast.makeText(requireContext(), "Magic Star 🌟 Ready! Ab Notification Panel se use karein.", Toast.LENGTH_LONG).show()
            
            // App ko background mein bhej do
            activity?.moveTaskToBack(true)
        }
    }
}
