package net.taikula.autohelper.tools

import android.os.SystemClock
import android.view.View

object ViewUtils {
    fun View.setSafeClickListener(gapTime: Long = 500, action: () -> Unit) {
        this.setOnClickListener(object : View.OnClickListener {
            private val MIN_CLICK_DELAY_TIME = gapTime
            private var lastClickTime: Long = 0
            override fun onClick(v: View) {
                val currentTime = SystemClock.uptimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime
                    action()
                }
            }
        })
    }

    /**
     * 使能控件
     */
    fun View.enable(able: Boolean = true) {
        this.isEnabled = able
        this.alpha = if (able) 1.0f else 0.5f
    }

    /**
     * 禁用控件
     */
    fun View.disable() {
        this.enable(false)
    }
}