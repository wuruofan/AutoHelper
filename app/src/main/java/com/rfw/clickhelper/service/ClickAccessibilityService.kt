package com.rfw.clickhelper.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.rfw.clickhelper.tool.Extensions.TAG

class ClickAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityService = this
    }

    override fun onDestroy() {
        super.onDestroy()
        accessibilityService = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event!!.eventType
        Log.i(TAG, "onAccessibilityEvent: $eventType")
        when (eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {}
//                handler.sendEmptyMessageDelayed(0, 1500)
            else -> {}
        }
    }

    override fun onInterrupt() {
//        TODO("Not yet implemented")
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var accessibilityService: AccessibilityService? = null
            private set
    }
}