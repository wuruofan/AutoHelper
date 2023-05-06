package net.taikula.autohelper.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.annotation.Px
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import net.taikula.autohelper.R
import net.taikula.autohelper.helper.MediaProjectionHelper
import net.taikula.autohelper.helper.RotationWatchHelper
import net.taikula.autohelper.tools.DisplayUtils
import net.taikula.autohelper.tools.Extensions.TAG
import kotlin.math.absoluteValue

class FloatWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var floatingViewLayoutParams: WindowManager.LayoutParams
    private lateinit var screenSize: Point

    var isLandscape = false

    private val rotationWatcher = RotationWatchHelper {
        isLandscape = it == Surface.ROTATION_90 || it == Surface.ROTATION_270
        Log.w(TAG, "RotationWatcher orientation: $it, isLandscape: $isLandscape")
//        resetVirtualDisplay()
        MediaProjectionHelper.instance?.isLandscape = isLandscape

        Handler(Looper.getMainLooper()).post {
            onOrientationChanged(isLandscape)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("InflateParams")
    override fun onCreate() {
        super.onCreate()

        screenSize = DisplayUtils.getRealScreenSize(this)

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null, false)
        val imageView = floatingView.findViewById<ImageView>(R.id.iv_main)
        Glide.with(this).asGif().load(R.drawable.mario_running).into(imageView)
        (imageView.drawable as? GifDrawable)?.stop()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val dp50 = DisplayUtils.dip2px(this, 50f)
        floatingViewLayoutParams = configWindowManagerParam(dp50, dp50).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenSize.x / 2
            y = screenSize.y / 2
        }
        windowManager.addView(floatingView, floatingViewLayoutParams)

        Log.w(
            TAG,
            "windowManager: ${floatingView.layoutParams.width} x ${floatingView.layoutParams.height}, @(${floatingView.x}, ${floatingView.y})"
        )

        floatingView.findViewById<ImageView>(R.id.iv_screenshot).setOnClickListener {
            Log.w(TAG, "screenshot clicked!!")
            if (MediaProjectionHelper.instance?.isTimerTicking == true) {
                MediaProjectionHelper.instance?.stopTimer()
            } else {
                MediaProjectionHelper.instance?.startTimer(1678)
            }
        }

        floatingView.isClickable = true
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var x: Int = 0
            private var y: Int = 0
            private var isMoving = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = event.rawX.toInt()
                        y = event.rawY.toInt()
                        isMoving = false

                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isMoving) {
                            isMoving = false
                        } else {
                            exit()
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newX = event.rawX.toInt()
                        val newY = event.rawY.toInt()
                        val deltaX = newX - x
                        val deltaY = newY - y

                        if (deltaX.absoluteValue > 0 || deltaY.absoluteValue > 0) {
                            isMoving = true
                            x = newX
                            y = newY

                            updateFloatingWindowPosition(deltaX, deltaY)
                        }
                    }
                }
                return false
            }
        })

        rotationWatcher.watchRotation()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        windowManager.removeView(floatingView)

        rotationWatcher.removeRotationWatcher()

        if (MediaProjectionHelper.instance?.isTimerTicking == true) {
            MediaProjectionHelper.instance?.stopTimer()
        }
        super.onDestroy()
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        Log.w(TAG, "onConfigurationChanged orientation: ${newConfig.orientation}")
//        super.onConfigurationChanged(newConfig)
//    }

    private fun startForeground() {
        val notification =
            Notification.Builder(this, net.taikula.autohelper.MainApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentText("识别屏幕中...")
                .build()
        this.startForeground(0x5252, notification)
    }

    private fun configWindowManagerParam(
        @Px width: Int,
        @Px height: Int
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            this.width = WindowManager.LayoutParams.WRAP_CONTENT
            this.height = WindowManager.LayoutParams.WRAP_CONTENT
            this.format = PixelFormat.RGBA_8888

            this.flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//                (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                    // 不拦截触摸事件
//                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                    // 处理窗口外的点击事件
//                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
//                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
////                    or WindowManager.LayoutParams.FLAG_SCALED
//                    or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
//                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            this.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
    }

    private fun updateFloatingWindowPosition(deltaX: Int, deltaY: Int) {
        floatingViewLayoutParams.x += deltaX
        floatingViewLayoutParams.y += deltaY
        windowManager.updateViewLayout(floatingView, floatingViewLayoutParams)
    }

    private fun backToActivity() {
//        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//        activityManager.moveTaskToFront()
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(intent)
    }

    private fun exit() {
        stopSelf()
        backToActivity()
    }

    private fun onOrientationChanged(isLandscapeNow: Boolean) {
        val aspectRadio = screenSize.x / screenSize.y.toFloat()
        val newX: Float
        val newY: Float

        if (isLandscapeNow) {
            // portrait -> landscape

            newX = floatingViewLayoutParams.x / aspectRadio // x / w * h = x / (w / h)
            newY = floatingViewLayoutParams.y * aspectRadio // y / h * w = y * (w / h)
        } else {
            // landscape -> portrait
            newX = floatingViewLayoutParams.x * aspectRadio // x / h * w = x * (w / h)
            newY = floatingViewLayoutParams.y / aspectRadio // y / w * h = y / (w / h)
        }

        Log.w(
            TAG,
            "onOrientationChanged: isLandscapeNow=$isLandscapeNow screenSize= ${screenSize}, aspectRadio=$aspectRadio, (${floatingViewLayoutParams.x},${floatingViewLayoutParams.y})->($newX,$newY)"
        )

        floatingViewLayoutParams.x = newX.toInt()
        floatingViewLayoutParams.y = newY.toInt()

        windowManager.updateViewLayout(floatingView, floatingViewLayoutParams)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.w(TAG, "onConfigurationChanged: ${newConfig.orientation}")
//        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//            onOrientationChanged(false)
//        } else {
//            onOrientationChanged(true)
//        }
    }
}