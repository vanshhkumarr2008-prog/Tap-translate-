package com.vansh.taptranslatepro.magic

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Future: screen text read logic
        // Ye tile click ke sath connect hoga
    }

    override fun onInterrupt() {}
}
