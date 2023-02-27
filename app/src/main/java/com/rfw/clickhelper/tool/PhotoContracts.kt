package com.rfw.clickhelper.tool

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.core.content.contentValuesOf
import com.rfw.clickhelper.tool.Extensions.TAG
import java.io.File

/**
 * 参考：https://blog.csdn.net/java_android_man/article/details/120809631
 */
class PhotoContracts {

    /**
     * 选择照片的协定
     * Input type  : Unit? 不需要传值
     * Output type : Uri?  选择完成后的 image uri
     */
    class SelectPhotoContract : ActivityResultContract<Unit?, Uri?>() {

        @CallSuper
        override fun createIntent(context: Context, input: Unit?): Intent {
            // todo: 新的图片选择器
            // https://developer.android.com/training/data-storage/shared/photopicker?hl=zh-cn#kotlin
            return Intent(Intent.ACTION_PICK).setType("image/*")
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            Log.d(TAG, "pick photo result: ${intent?.data}")
            return intent?.data
        }
    }

    /**
     * 剪裁照片的协定
     * Input type  : CropParams 剪裁照片的相关参数
     * Output type : Uri?       照片剪裁完成后的uri
     */
    class CropPhotoContract : ActivityResultContract<CropParams, Uri?>() {

        private var outputUri: Uri? = null

        @CallSuper
        override fun createIntent(context: Context, input: CropParams): Intent {
            // 获取输入图片uri的媒体类型
            val mimeType = context.contentResolver.getType(input.uri)
            // 创建新的图片名称
            val imageName = "${System.currentTimeMillis()}.${
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            }"
            outputUri = if (input.extraOutputUri != null) {
                // 使用指定的uri地址
                input.extraOutputUri
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10 及以上获取图片uri
                    val values = contentValuesOf(
                        Pair(MediaStore.MediaColumns.DISPLAY_NAME, imageName),
                        Pair(MediaStore.MediaColumns.MIME_TYPE, mimeType),
                        Pair(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                    )
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                } else {
                    Uri.fromFile(File(context.externalCacheDir!!.absolutePath, imageName))
                }
            }

            return Intent("com.android.camera.action.CROP")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setDataAndType(input.uri, mimeType)
                .putExtra("outputX", input.outputX)
                .putExtra("outputY", input.outputY)
                .putExtra("aspectX", input.aspectX)
                .putExtra("aspectY", input.aspectY)
                .putExtra("scale", input.scale)
                .putExtra("crop", input.crop)
                .putExtra("return-data", input.returnData)
                .putExtra("noFaceDetection", input.noFaceDetection)
                .putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                .putExtra("outputFormat", input.outputFormat)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            Log.d(TAG, "crop photo outputUri : $outputUri")
            return outputUri
        }
    }

    /**
     * 剪裁照片的参数
     */
    class CropParams(
        val uri: Uri,
        val aspectX: Int = 1,
        val aspectY: Int = 1,
        @androidx.annotation.IntRange(from = 0, to = 1080)
        val outputX: Int = 250,
        @androidx.annotation.IntRange(from = 0, to = 1080)
        val outputY: Int = 250,
        val scale: Boolean = true,
        val crop: Boolean = true,
        val noFaceDetection: Boolean = true,
        val returnData: Boolean = false,
        val outputFormat: String = Bitmap.CompressFormat.JPEG.toString(),
        val extraOutputUri: Uri? = null
    )
}