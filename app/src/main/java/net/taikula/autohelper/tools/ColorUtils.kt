package net.taikula.autohelper.tools

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

object ColorUtils {

    @ColorInt
    fun getColor(context: Context, @AttrRes colorAttrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(colorAttrRes, typedValue, true)
        return typedValue.data
    }
}