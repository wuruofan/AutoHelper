package net.taikula.autohelper.service

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import net.taikula.autohelper.MainApp
import net.taikula.autohelper.R
import net.taikula.autohelper.databinding.FloatingWindowBinding
import net.taikula.autohelper.helper.MediaProjectionHelper
import net.taikula.autohelper.helper.RotationWatchHelper
import net.taikula.autohelper.model.ClickTask
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

    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * 悬浮窗展开时自动隐藏动画
     */
    private val autoHideRunnable = Runnable() {
        hiddenOtherViews()
        animatedMagnetizeToEdge()
    }

    /**
     * 悬浮窗吸附边缘隐藏时不可见的宽度
     */
    private var hiddenSize = INVALID_VALUE

    /**
     * 悬浮窗未展开时宽度
     */
    private var aloneWidth = INVALID_VALUE

    /**
     * 悬浮窗展开时宽度
     */
    private var expandedWidth = INVALID_VALUE

    /**
     * 是否吸附边缘隐藏中
     */
    private var hiddenState = false

    /**
     * 悬浮窗当前左右位置
     */
    private var posState = State.NONE

    private val rotationWatcher = RotationWatchHelper {
        isLandscape = it == Surface.ROTATION_90 || it == Surface.ROTATION_270
        Log.w(TAG, "RotationWatcher orientation: $it, isLandscape: $isLandscape")
//        resetVirtualDisplay()
        MediaProjectionHelper.instance?.isLandscape = isLandscape

        mainHandler.post {
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

                        if (!hiddenState) {
                            cancelDelayedHideAnimation()
                        }
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
                            val isLeft = x < screenSize.x / 2
                            if (hiddenState) {
                                // 未展开时动画展开
                                animatedShowFromEdge(isLeft)
                            } else {
                                // 已展开时隐藏到边缘
                                hiddenOtherViews()
                                animatedHideToEdge()
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

                            updateFloatingWindowPosition(deltaX, deltaY)

                            Log.i(TAG, "moving=$x,$y")

                            if (!hiddenState) {
                                // 屏幕左侧的时候需要更新悬浮窗 x 坐标，避免脱离手指位置
                                // fixme: 还有些僵硬、掉帧的感觉
                                mainHandler.post {
                                    if (x < screenSize.x / 2) {
                                        updateFloatingWindowAbsPosition(
                                            floatingViewLayoutParams.x + expandedWidth - aloneWidth,
                                            floatingViewLayoutParams.y
                                        )
                                    }
                                    hiddenOtherViews()
                                }
                            }
                        }
                    }
                }
                return false
            }
        })

        // 界面渲染完后吸附到屏幕边缘
        mainHandler.post {
            if (aloneWidth == INVALID_VALUE) {
                aloneWidth = floatingView.width
                hiddenSize = aloneWidth / 4
            }

            animatedMagnetizeToEdge()
            hiddenState = true
        }
    }

    /**
     * 初始化点击事件
     */
    private fun initViewClickListeners() {
        floatingView.isClickable = true

        val animatable = (_binding.ivMain.drawable as? Animatable)

        // 运行/停止按钮事件
        val runStopListener: () -> Unit = {
            Log.w(TAG, "screenshot clicked!!")
            if (MediaProjectionHelper.isRunning) {
                MediaProjectionHelper.instance?.stopTimer()
                _binding.ivLeftRunStop.isSelected = false
                _binding.ivRightRunStop.isSelected = false
//                animatable?.stop()
            } else {
                MediaProjectionHelper.instance?.startTimer()
                _binding.ivLeftRunStop.isSelected = true
                _binding.ivRightRunStop.isSelected = true
//                animatable?.start()
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
        val targetX = if (floatingViewLayoutParams.x < screenSize.x / 2) 0 - hiddenSize
        else (screenSize.x - aloneWidth + hiddenSize)

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
        mainHandler.post {
            if (expandedWidth == INVALID_VALUE) {
                expandedWidth = floatingView.width
            }

            val targetX = if (isLeft)
                0
            else
                screenSize.x - expandedWidth

            val animator = ValueAnimator.ofInt(floatingViewLayoutParams.x, targetX)
            animator.addUpdateListener { animation ->
                updateFloatingWindowAbsPosition(
                    animation.animatedValue as Int, floatingViewLayoutParams.y
                )
            }
            animator.addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    animatedHideToEdge(3000)
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            })
            animator.interpolator = LinearInterpolator()
            animator.duration = 200
            animator.start()

        }
    }

    /**
     * 展开恢复到边缘吸附
     */
    private fun animatedHideToEdge(delayMs: Long = 0) {
        mainHandler.postDelayed(autoHideRunnable, delayMs)
    }

    /**
     * 移出延迟的隐藏到边缘动画
     */
    private fun cancelDelayedHideAnimation() {
        mainHandler.removeCallbacks(autoHideRunnable)
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

        setStateAndBackground(State.NONE)
    }

    /**
     * 设置悬浮窗背景
     */
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

    /**
     * 设置悬浮窗位置状态和背景
     */
    private fun setStateAndBackground(state: State) {
        this.posState = state
        setBackground(state)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)

        cancelDelayedHideAnimation()

        windowManager.removeView(floatingView)

        rotationWatcher.removeRotationWatcher()

        if (MediaProjectionHelper.isRunning) {
            MediaProjectionHelper.instance?.stopTimer()
        }
        super.onDestroy()
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        Log.w(TAG, "onConfigurationChanged orientation: ${newConfig.orientation}")
//        super.onConfigurationChanged(newConfig)
//    }

    /**
     * 启动前台通知栏
     */
    private fun startForeground() {
        val notification =
            Notification.Builder(this, MainApp.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("屏幕识别")
                .build()
        this.startForeground(0x5252, notification)
    }

    /**
     * 配置悬浮窗参数
     */
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

    /**
     * 回到 activity
     */
    private fun backToActivity() {
//        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//        activityManager.moveTaskToFront()
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(intent)
    }

    /**
     * 退出服务
     */
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

    companion object {
        private const val INVALID_VALUE = -1

        /**
         * 当前点击任务
         */
        var currentTask: ClickTask? = null

        /**
         * 检查上一次点击是否成功的标志位
         */
        var lastClickCheck = false

    }
}