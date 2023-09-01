package net.taikula.autohelper.service

import android.animation.ValueAnimator
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import net.taikula.autohelper.R
import net.taikula.autohelper.databinding.FloatingWindowBinding
import net.taikula.autohelper.helper.MediaProjectionHelper
import net.taikula.autohelper.helper.RotationWatchHelper
import net.taikula.autohelper.tools.DisplayUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.ViewUtils.setSafeClickListener
import kotlin.math.absoluteValue

/**
 * 悬浮窗服务
 */
class FloatWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var floatingViewLayoutParams: WindowManager.LayoutParams
    private lateinit var screenSize: Point
    private lateinit var _binding: FloatingWindowBinding

    private var isLandscape = false

    /**
     * 是否吸附边缘隐藏中
     */
    private var hiddenState = false

    private var posState = State.NONE

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

        initFloatingView()

        // 监听屏幕旋转事件
        rotationWatcher.watchRotation()
    }

    /**
     * 初始化悬浮窗相关
     */
    private fun initFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null, false)
        _binding = FloatingWindowBinding.bind(floatingView)

        floatingViewLayoutParams = configWindowManagerParam().apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenSize.x / 2
            y = screenSize.y / 2
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, floatingViewLayoutParams)

        Log.w(
            TAG,
            "windowManager: ${floatingView.layoutParams.width} x ${floatingView.layoutParams.height}, @(${floatingView.x}, ${floatingView.y})"
        )

        initViewClickListeners()

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var x: Int = 0
            private var y: Int = 0
            private var isMoving = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = event.rawX.toInt()
                        y = event.rawY.toInt()
                        isMoving = false
                    }

                    MotionEvent.ACTION_UP -> {
                        x = event.rawX.toInt()
                        y = event.rawY.toInt()
                        Log.i(TAG, "up=$x,$y")

                        if (isMoving) {
                            isMoving = false

                            animatedMagnetizeToEdge()
                        } else {
                            // 点击事件
                            if (hiddenState) {
                                animatedShowFromEdge(x < screenSize.x / 2)
                            } else {
                                hiddenOtherViews()
                            }

                            v?.performClick()
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

                            if (!hiddenState) {
                                hiddenOtherViews()
                            }
                            setStateAndBackground(State.NONE)

                            updateFloatingWindowPosition(deltaX, deltaY)

                            Log.i(TAG, "moving=$x,$y")
                        }
                    }
                }
                return false
            }
        })

        // 界面渲染完后吸附到屏幕边缘
        Handler(Looper.getMainLooper()).post {
            animatedMagnetizeToEdge()
        }
    }

    /**
     * 初始化点击事件
     */
    private fun initViewClickListeners() {
        floatingView.isClickable = true

        // 运行/停止按钮事件
        val runStopListener: () -> Unit = {
            Log.w(TAG, "screenshot clicked!!")
            if (MediaProjectionHelper.instance?.isTimerTicking == true) {
                MediaProjectionHelper.instance?.stopTimer()
            } else {
                MediaProjectionHelper.instance?.startTimer(1678)
            }
        }

        _binding.ivLeftRunStop.setSafeClickListener(action = runStopListener)
        _binding.ivRightRunStop.setSafeClickListener(action = runStopListener)

        _binding.ivLeftExit.setSafeClickListener { exit() }
        _binding.ivRightExit.setSafeClickListener { exit() }

    }

    /**
     * 吸附到屏幕左右边缘
     */
    private fun animatedMagnetizeToEdge() {
        val targetX = if (floatingViewLayoutParams.x < screenSize.x / 2) 0 - floatingView.width / 3
        else (screenSize.x - floatingView.width * 2 / 3)

        val animator = ValueAnimator.ofInt(floatingViewLayoutParams.x, targetX)
        animator.addUpdateListener { animation ->
//            Log.i(TAG, "animatedValue: ${animation.animatedValue}")
            updateFloatingWindowAbsPosition(
                animation.animatedValue as Int, floatingViewLayoutParams.y
            )
        }
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 200
        animator.start()
    }

    /**
     * 边缘吸附时点击展开
     */
    private fun animatedShowFromEdge(isLeft: Boolean = true) {
        showCompleteViews(isLeft)

        // width 生效后再触发动画避免 width 值未更新导致位置计算错误
        Handler(Looper.getMainLooper()).post {
            val targetX = if (isLeft)
                0
            else
                screenSize.x - floatingView.width

            val animator = ValueAnimator.ofInt(floatingViewLayoutParams.x, targetX)
            animator.addUpdateListener { animation ->
                updateFloatingWindowAbsPosition(
                    animation.animatedValue as Int, floatingViewLayoutParams.y
                )
            }
            animator.interpolator = LinearInterpolator()
            animator.duration = 200
            animator.start()

        }
    }

    /**
     * 展开恢复到边缘吸附
     */
    private fun animatedHideToEdge() {

    }

    /**
     * 显示全部的悬浮窗
     *
     * @param isLeft 是否在屏幕左侧
     */
    private fun showCompleteViews(isLeft: Boolean = true) {
        hiddenState = false

        // 在屏幕左侧展示右边按钮，在右侧展示左边按钮
        if (isLeft) {
            _binding.layoutLeftMore.visibility = View.VISIBLE
            _binding.layoutRightMore.visibility = View.GONE
            setStateAndBackground(State.LEFT)
        } else {
            _binding.layoutLeftMore.visibility = View.GONE
            _binding.layoutRightMore.visibility = View.VISIBLE
            setStateAndBackground(State.RIGHT)
        }
    }

    /**
     * 隐藏其他按钮，仅显示主图标
     */
    private fun hiddenOtherViews() {
        hiddenState = true

        _binding.layoutRightMore.visibility = View.GONE
        _binding.layoutLeftMore.visibility = View.GONE
    }

    private fun setBackground(state: State) {
        floatingView.background = when (state) {
            State.LEFT -> AppCompatResources.getDrawable(
                this, R.drawable.shape_floating_window_left_bg
            )

            State.RIGHT -> AppCompatResources.getDrawable(
                this, R.drawable.shape_floating_window_right_bg
            )

            else -> AppCompatResources.getDrawable(this, R.drawable.shape_floating_window_bg)
        }
    }

    private fun setStateAndBackground(state: State) {
        this.posState = state
        setBackground(state)
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

    private fun configWindowManagerParam(): WindowManager.LayoutParams {
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

    /**
     * 更新悬浮窗位置，相对坐标
     */
    private fun updateFloatingWindowPosition(deltaX: Int, deltaY: Int) {
        floatingViewLayoutParams.x += deltaX
        floatingViewLayoutParams.y += deltaY
        windowManager.updateViewLayout(floatingView, floatingViewLayoutParams)
    }

    /**
     * 更新悬浮窗位置，绝对值坐标
     */
    private fun updateFloatingWindowAbsPosition(x: Int, y: Int) {
        floatingViewLayoutParams.x = x
        floatingViewLayoutParams.y = y
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

    internal enum class State {
        NONE, LEFT, RIGHT
    }
}