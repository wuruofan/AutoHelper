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
import net.taikula.autohelper.service.FloatWindowService
import net.taikula.autohelper.tools.DisplayUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.Extensions.launchPeriodicAsync
import java.lang.ref.WeakReference
import java.util.*

typealias ImageReadyCallback = (Bitmap?) -> Unit

/**
 * https://github.com/android/media-samples/blob/6b205c3db94a7f4a0f73fe27fc6139125c24e9b8/ScreenCapture/Application/src/main/java/com/example/android/screencapture/ScreenCaptureFragment.java
 */
@SuppressLint("WrongConstant")
class MediaProjectionHelper private constructor(
    activity: ComponentActivity,
//    activityResultCallback: ActivityResultCallback<ActivityResult>,
    @WorkerThread callback: ImageReadyCallback? = null
) {
    private var screenCaptureResultCode = 0
    private var screenCaptureResultData: Intent? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        activity.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val screenSize = DisplayUtils.getRealScreenSize(activity)
    private var screenDpi = DisplayUtils.getScreenDensity(activity)

    private var weakActivity = WeakReference(activity)

    var isLandscape = false

    @Volatile
    var isCapturing = false

    @Volatile
    var isTimerTicking = false

    var timer = Timer("media_projection")

    var timerJob: Job? = null

    private var imageReader: ImageReader? = null
    private var imageReadyCallback: ImageReadyCallback? = callback

    private val requestMediaProjectionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "request media projection failed!")
                return@registerForActivityResult
            }

            screenCaptureResultCode = it.resultCode
            screenCaptureResultData = it.data

            activity.run {
                startService(Intent(activity, FloatWindowService::class.java))
                moveTaskToBack(true)
            }

//            startScreenCapture()
//            activityResultCallback.onActivityResult(it)
        }


    private fun setUpMediaProjection() {
        if (screenCaptureResultData == null)
            return

        mediaProjection = mediaProjectionManager.getMediaProjection(
            screenCaptureResultCode,
            screenCaptureResultData!!
        )

    }

    private fun tearDownMediaProjection() {
        mediaProjection?.stop()
        mediaProjection = null

    }

    fun requestPermission() {
        Log.d(TAG, "Requesting confirmation")
        // This initiates a prompt dialog for the user to confirm screen projection.
        requestMediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    fun isPermissionGranted(): Boolean {
        return screenCaptureResultCode == Activity.RESULT_OK
    }

    fun startScreenCapture() {
        if (mediaProjection != null) {
            setUpVirtualDisplay()
        } else if (screenCaptureResultCode != 0 && screenCaptureResultData != null) {
            setUpMediaProjection()
            setUpVirtualDisplay()
        } else {
            requestPermission()
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
        if (!isCapturing)
            return

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
    }

    fun onSaveInstanceState(outState: Bundle) {
        if (screenCaptureResultData != null) {
            outState.putInt(SCREEN_CAPTURE_STATE_RESULT_CODE, screenCaptureResultCode)
            outState.putParcelable(SCREEN_CAPTURE_STATE_RESULT_DATA, screenCaptureResultData)
        }
    }

    fun onRestoreInstanceState(inState: Bundle?) {
        inState?.let {
            screenCaptureResultCode = it.getInt(SCREEN_CAPTURE_STATE_RESULT_CODE)
            screenCaptureResultData =
                it.getParcelable(SCREEN_CAPTURE_STATE_RESULT_DATA)
        }
    }

    fun setImageReadyCallback(callback: ImageReadyCallback?) {
        imageReadyCallback = callback
    }

    fun startTimer(period: Long) {
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
        if (isCapturing)
            stopScreenCapture()

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

        // 单例参考：https://juejin.cn/post/6844903590545326088
        fun initInstance(
            activity: ComponentActivity,
            @WorkerThread callback: ImageReadyCallback? = null
        ): MediaProjectionHelper {
            return instance ?: synchronized(this) {
                instance ?: MediaProjectionHelper(activity, callback).also { instance = it }
            }
        }

        fun image2Bitmap(image: Image?): Bitmap? {
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
            val tmpBitmap =
                Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
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