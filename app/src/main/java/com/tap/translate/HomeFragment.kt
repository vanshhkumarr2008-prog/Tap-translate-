override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {

        val serviceIntent = Intent(requireContext(), ScreenCaptureService::class.java).apply {
            putExtra("RESULT_CODE", resultCode)
            putExtra("DATA", data)
            putExtra("TARGET_LANG", selectedLanguage)
        }

        ContextCompat.startForegroundService(requireContext(), serviceIntent)

        Toast.makeText(requireContext(), "Magic Star 🌟 Ready!", Toast.LENGTH_SHORT).show()

        // ❌ moveTaskToBack REMOVE
    }
}
