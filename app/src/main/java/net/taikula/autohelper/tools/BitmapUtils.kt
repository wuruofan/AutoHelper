package net.taikula.autohelper.tools

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import net.taikula.autohelper.tools.Extensions.TAG

object BitmapUtils {
    /**
     * 判断当前 bitmap 是否包含指定矩形区域
     */
    fun Bitmap.contains(rect: Rect): Boolean {
        if (rect.left < 0 || rect.top < 0)
            return false

        if (rect.right <= rect.left || rect.bottom <= rect.top)
            return false

        if (rect.right - rect.left > this.width || rect.bottom - rect.top > this.height)
            return false

        return true
    }

    /**
     * 裁剪矩形区域图片
     */
    fun Bitmap.cropRectBitmap(rect: Rect): Bitmap? {
        if (!this.contains(rect)) {
            return null
        }

        val width = this.width
        val height = this.height
        val doodleWidth = rect.width()
        val doodleHeight = rect.height()

//        if (isLandscape && width < height) {
////            width = height.also { height = width } // swap: https://stackoverflow.com/a/45377921/1097709
//            val tmp = doodleWidth
//            doodleWidth = doodleHeight
//            doodleHeight = tmp
//        }

        Log.w(
            TAG,
            "cropDoodleRectBitmap: origin size = $width x $height, doodle size = $doodleWidth x $doodleHeight, rect: $rect"
        )

        return Bitmap.createBitmap(this, rect.left, rect.top, doodleWidth, doodleHeight)
    }


}