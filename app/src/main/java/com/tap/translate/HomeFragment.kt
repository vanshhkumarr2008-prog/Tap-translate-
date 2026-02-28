package com.tap.translate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE = 1000
    private var selectedLanguage = "Hindi"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val spinner = view.findViewById<Spinner>(R.id.languageSpinner)
        val btnActivate = view.findViewById<Button>(R.id.btnActivate)

        projectionManager =
            requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        val languages = arrayOf("Hindi", "Spanish", "French", "Arabic", "German")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )

        spinner.adapter = adapter

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedLanguage = languages[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        btnActivate.setOnClickListener {
            checkPermissionsAndStart()
        }

        return view
    }

    private fun checkPermissionsAndStart() {

        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                return
            }
        }

        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_CODE
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {

            val serviceIntent =
                Intent(requireContext(), ScreenCaptureService::class.java).apply {
                    putExtra("RESULT_CODE", resultCode)
                    putExtra("DATA", data)
                    putExtra("TARGET_LANG", selectedLanguage)
                }

            ContextCompat.startForegroundService(
                requireContext(),
                serviceIntent
            )

            Toast.makeText(
                requireContext(),
                "Magic Star 🌟 Ready!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
