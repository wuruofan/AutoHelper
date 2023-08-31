package net.taikula.autohelper.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
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

        // 避免竖屏的时候尝试截取横屏点击区域时，范围超出导致崩溃
        return try {
            Bitmap.createBitmap(this, rect.left, rect.top, doodleWidth, doodleHeight)
        } catch (e: Exception) {
            null
        }
    }


    /**
     * 获取图片宽高
     */
    fun getImageSize(context: Context, uri: Uri): Point? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            return if (options.outWidth > 0 && options.outHeight > 0) {
                Point().apply { set(options.outWidth, options.outHeight) }
            } else {
                null
            }
        } catch (ignore: Exception) {
            ignore.printStackTrace()
            return null
        }
    }
}