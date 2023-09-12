package net.taikula.autohelper.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.taikula.autohelper.tools.DialogXUtils
import net.taikula.autohelper.tools.DisplayUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.Extensions.launchPeriodicAsync
import java.util.*

typealias ImageReadyCallback = (Bitmap?) -> Unit

/**
 * 手机截屏/录屏功能
 * [参考链接](https://github.com/android/media-samples/blob/6b205c3db94a7f4a0f73fe27fc6139125c24e9b8/ScreenCapture/Application/src/main/java/com/example/android/screencapture/ScreenCaptureFragment.java)
 * @param activity 需要触发录屏的 activity，需要申请权限使用，必须提前注册 launcher 否则报错
 */
@SuppressLint("WrongConstant")
class MediaProjectionHelper private constructor(val activity: ComponentActivity) {
    private var screenCaptureResultCode = 0
    private var screenCaptureResultData: Intent? = null

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        activity.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val screenSize = DisplayUtils.getRealScreenSize(activity)
    private var screenDpi = DisplayUtils.getScreenDensity(activity)

    var isLandscape = false

    /**
     * 是否在运行
     */
    var isRunning: Boolean = false
        get() = isTimerTicking
        private set


    /**
     * 是否在录制屏幕
     */
    @Volatile
    private var isCapturing = false

    /**
     * 定时器是否在运行
     * 定时器触发时才录制屏幕，设置 [isCapturing] 为 true
     */
    @Volatile
    private var isTimerTicking = false

//    private var timer = Timer("media_projection")

    /**
     * 定时器任务
     */
    private var timerJob: Job? = null

    /**
     * 屏幕读取器
     */
    private var imageReader: ImageReader? = null

    /**
     * 屏幕图像准备就绪回调
     */
    private var imageReadyCallback: ImageReadyCallback? = null

    /**
     * 权限申请成功后的回调
     */
    private var permissionCallback: ((Boolean) -> Unit)? = null

    /**
     * 注册请求权限回调，必须在 activity START 之前设置，否则会报错
     * LifecycleOwners must call register before they are STARTED.
     */
    private val requestMediaProjectionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "request media projection failed!")
                permissionCallback?.invoke(false)
                return@registerForActivityResult
            }

            screenCaptureResultCode = it.resultCode
            screenCaptureResultData = it.data

            permissionCallback?.invoke(true)
        }

    /**
     * 设置权限申请回调
     */
    fun setPermissionCallback(callback: (Boolean) -> Unit) {
        this.permissionCallback = callback
    }

    /**
     * 请求录屏权限
     */
    fun requestPermission() {
        Log.d(TAG, "Requesting confirmation")
        // This initiates a prompt dialog for the user to confirm screen projection.
        requestMediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    /**
     * 是否有录屏权限
     */
    fun isPermissionGranted(): Boolean {
        return screenCaptureResultCode == Activity.RESULT_OK
    }

    /**
     * 保存数据
     */
    fun onSaveInstanceState(outState: Bundle) {
        if (screenCaptureResultData != null) {
            outState.putInt(SCREEN_CAPTURE_STATE_RESULT_CODE, screenCaptureResultCode)
            outState.putParcelable(SCREEN_CAPTURE_STATE_RESULT_DATA, screenCaptureResultData)
        }
    }

    /**
     * 恢复数据
     */
    fun onRestoreInstanceState(inState: Bundle?) {
        inState?.let {
            screenCaptureResultCode = it.getInt(SCREEN_CAPTURE_STATE_RESULT_CODE)
            screenCaptureResultData = it.getParcelable(SCREEN_CAPTURE_STATE_RESULT_DATA)
        }
    }

    private fun setUpMediaProjection() {
        if (screenCaptureResultData == null) return

        mediaProjection = mediaProjectionManager.getMediaProjection(
            screenCaptureResultCode, screenCaptureResultData!!
        )
    }

    private fun tearDownMediaProjection() {
        mediaProjection?.stop()
        mediaProjection = null

    }

    fun startScreenCapture(activity: ComponentActivity? = null) {
        if (mediaProjection != null) {
            setUpVirtualDisplay()
        } else if (screenCaptureResultCode != 0 && screenCaptureResultData != null) {
            setUpMediaProjection()
            setUpVirtualDisplay()
        } else {
            DialogXUtils.showPopTip("尚未申请屏幕录制权限！")
        }
    }

    private fun initImagerReader(width: Int, height: Int) {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
            this.setOnImageAvailableListener({ reader ->
//                if (!reader.equals(imageReader)) {
//                    Log.w(TAG, "imageReader reconstructed!")
//                    reader.close()
//                    return@setOnImageAvailableListener
//                }
//
//                val image = reader.acquireLatestImage()
//                if (image != null) {
//                    val bitmap = image2Bitmap(image)
//                    Log.w(TAG, "bitmap: ${bitmap?.width} x ${bitmap?.height}")
//                    image.close()
//                    imageAvailableCallback(bitmap)
//                } else {
//                    Log.d(TAG, "image == null")
//                }
//
//                stopScreenCapture()
            }, Handler(Looper.getMainLooper()))
            // todo: imageReader.surface.setFrameRate?
//        this.surface.setFrameRate()
        }
    }

    private fun setUpVirtualDisplay() {
        Log.w(TAG, "screen size: $screenSize, isLandscape: $isLandscape")

        val width = if (isLandscape) screenSize.y else screenSize.x
        val height = if (isLandscape) screenSize.x else screenSize.y

        Log.w(TAG, "setUpVirtualDisplay: $width x $height")

        isCapturing = true

        initImagerReader(width, height)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            screenDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )

    }

    fun stopScreenCapture() {
        if (!isCapturing) return

        virtualDisplay?.release()
        virtualDisplay = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            imageReader?.discardFreeBuffers()
        }
        imageReader?.close()
        imageReader = null

        isCapturing = false
    }

    private fun resetVirtualDisplay() {
        stopScreenCapture()
        setUpVirtualDisplay()
//        val width = if (isLandscape) screenSize.y else screenSize.x
//        val height = if (isLandscape) screenSize.x else screenSize.y
//
//        Log.w(TAG, "setUpVirtualDisplay: $width x $height")
//
//        initImagerReader(width, height)
//
//        virtualDisplay?.resize(width, height, screenDpi)
    }

    fun destroy() {
        stopTimer()
        stopScreenCapture()
        tearDownMediaProjection()

        instance = null
    }

    /**
     * 设置屏幕图像准备就绪回调
     */
    fun setImageReadyCallback(@WorkerThread callback: ImageReadyCallback? = null) {
        imageReadyCallback = callback
    }

    fun startTimer(period: Long = 1678) {
        if (isTimerTicking) return

        isTimerTicking = true

        timerJob = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(period) {
            Log.w(TAG, "timer tick")
            startScreenCapture()

            var image: Image? = null
            while (true) {
                image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val bitmap = image2Bitmap(image)
                    imageReadyCallback?.invoke(bitmap)
                    image.close()
                    break
                } else {
                    Log.d(TAG, "image == null, sleep 100ms")
                    delay(100)
                    if (!isTimerTicking) {
                        break
                    }
                }
            }

            stopScreenCapture()
        }

//        timer.schedule(object : TimerTask() {
//            override fun run() {
//                Log.w(TAG, "timer tick")
//                startScreenCapture()
//
//                var image: Image? = null
//                while (true) {
//                    image = imageReader?.acquireLatestImage()
//                    if (image != null) {
//                        val bitmap = image2Bitmap(image)
//                        imageReadyCallback?.invoke(bitmap)
//                        image.close()
//                        break
//                    } else {
//                        Log.d(TAG, "image == null, sleep 100ms")
//                        Thread.sleep(100)
//                    }
//                }
//
//                stopScreenCapture()
//            }
//        }, 0, period)
    }

    fun stopTimer() {
        Log.w(TAG, "stopTimer: isTimerTicking=$isTimerTicking")
        if (!isTimerTicking) return
        if (isCapturing) stopScreenCapture()

        timerJob?.cancel()
        timerJob = null

//        timer.cancel()
//        timer.purge()
        isTimerTicking = false
    }

    companion object {
        private const val SCREEN_CAPTURE_STATE_RESULT_CODE = "screen_capture_result_code"
        private const val SCREEN_CAPTURE_STATE_RESULT_DATA = "screen_capture_result_data"

        var instance: MediaProjectionHelper? = null
            private set

        /**
         * 初始化单例
         * [单例参考](https://juejin.cn/post/6844903590545326088)
         */
        fun initInstance(
            activity: ComponentActivity
        ): MediaProjectionHelper {
            return instance ?: synchronized(this) {
                instance ?: MediaProjectionHelper(activity).also { instance = it }
            }
        }

        /**
         * Image 对象转换成 Bitmap
         */
        private fun image2Bitmap(image: Image?): Bitmap? {
            if (image == null) {
                println("image 为空")
                return null
            }

            /* 将image图层缓冲区字节数据按像素格式写入bitmap */
            val width = image.width
            val height = image.height
            val planes = image.planes // 图层？
            val buffer = planes[0].buffer // 图层的buffer

            val pixelStride = planes[0].pixelStride // 每个像素的占用字节数(像素间距)
            val rowStride = planes[0].rowStride // 每行像素的字节数
            //因为内存对齐的缘故，所以buffer的行宽度与上面获取到的width*pixelStride会有差异
            //内存对齐的padding字节数 = Buffer行宽 - Image中图片宽度×像素间距
            val rowPadding = rowStride - pixelStride * width
            //接收ByteBuffer数据的Bitmap需要的像素宽度 = Image中图片宽度 + 内存对齐宽度/像素间距
            //每行的对应位置会填充一些无效数据
            //(其实直接写成rowStride/pixelStride也行，按步骤写只是为了让逻辑清晰)
            val tmpBitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
            )
            tmpBitmap.copyPixelsFromBuffer(buffer)

            val lastBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, width, height) //过滤掉每行像素中的无效数据

            Log.w(
                TAG,
                "image2Bitmap: ${image.width} x ${image.height} -> ${tmpBitmap.width} x ${tmpBitmap.height} -> ${lastBitmap.width} x ${lastBitmap.height}"
            )

            tmpBitmap.recycle()
            return lastBitmap
        }
    }

}