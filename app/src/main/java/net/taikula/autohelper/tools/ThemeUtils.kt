package net.taikula.autohelper.tools

import android.content.Context
import android.content.res.Configuration

object ThemeUtils {

    /**
     * 是否为深色主题
     */
    fun isDarkTheme(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }
}