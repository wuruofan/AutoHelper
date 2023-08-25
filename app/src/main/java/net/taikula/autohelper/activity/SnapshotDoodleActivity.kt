package net.taikula.autohelper.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import net.taikula.autohelper.R
import net.taikula.autohelper.databinding.ActivitySnapshotConfigBinding
import net.taikula.autohelper.tools.DisplayUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.ViewUtils.setSafeClickListener
import net.taikula.autohelper.view.DoodleImageView

class SnapshotDoodleActivity : Activity() {
    companion object {
        const val INTENT_KEY_IMAGE_URI = "image_uri"
    }

    private lateinit var binding: ActivitySnapshotConfigBinding

    private lateinit var bitmap: Bitmap
    private lateinit var grafftiImageView: DoodleImageView
    private var floatingView: View? = null

    private var lastBackTime = 0L

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySnapshotConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DisplayUtils.fullscreen(this)

        grafftiImageView = binding.grafftiImageView
        intent.getStringExtra(INTENT_KEY_IMAGE_URI)?.let {
            bitmap = BitmapFactory.decodeStream(this.contentResolver.openInputStream(Uri.parse(it)))

            if (bitmap.width > bitmap.height) {
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            grafftiImageView.setImageBitmap(bitmap)
        }

        initFloatingInfoView()
    }

    override fun onResume() {
        super.onResume()

        Log.w(
            TAG,
            "width: ${bitmap.width}, height: ${bitmap.height}, screen size: ${
                DisplayUtils.getScreenWidth(this)
            } x ${DisplayUtils.getScreenHeight(this)}, real screen size: ${
                DisplayUtils.getRealScreenSize(
                    this
                )
            }"
        )
    }

    override fun onPause() {
        Log.w(TAG, "onPause")

        // 放到onStop/onDestroy中都会出现上一个Activity的onResume先执行，但是onStop/onDestroy尚未执行的情况
//        cropDoodleRectBitmap()
//        ClickAreaModel.bitmap = grafftiImageView.doodledBitmap()

//        if (grafftiImageView.clickArea?.isEmpty() == true) {
//            setResult(RESULT_CANCELED, Intent())
//        } else {
//            val resultIntent = Intent()
//            resultIntent.putExtra("data", grafftiImageView.clickArea)
//            setResult(RESULT_OK, resultIntent)
//        }

        super.onPause()
    }

    override fun onStop() {
        Log.w(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy")
        removeFloatView()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val now = System.currentTimeMillis()
            if (now - lastBackTime <= 1500) {
                finishWithResult()
            } else {
                lastBackTime = now
                Toast.makeText(this, "再次点击返回退出", Toast.LENGTH_SHORT).show()
            }

            return false
        }

        return super.onKeyDown(keyCode, event)
    }

    /**
     * 保存结果并退出
     */
    private fun finishWithResult() {
        if (grafftiImageView.clickArea?.isEmpty() == true) {
            setResult(RESULT_CANCELED, Intent())
        } else {
            val resultIntent = Intent()
            resultIntent.putExtra("data", grafftiImageView.clickArea)
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    /**
     * 清除缓存并退出
     */
    private fun finishWithoutSave() {
        grafftiImageView.removeDoodledImageCache()
        setResult(RESULT_CANCELED, Intent())
        finish()
    }

    /**
     * 初始化悬浮窗
     */
    private fun initFloatingInfoView() {
        val layoutParams = WindowManager.LayoutParams().apply {
            // ⚠️设置大小自适应，不生效
            width = DisplayUtils.dip2px(
                this@SnapshotDoodleActivity,
                240f
            ) //WindowManager.LayoutParams.WRAP_CONTENT
            height = DisplayUtils.dip2px(
                this@SnapshotDoodleActivity,
                80f
            ) //WindowManager.LayoutParams.WRAP_CONTENT
            flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            // ⚠️必须设置，否则不支持透明色！四个角是黑的！！！！
            format = PixelFormat.RGBA_8888

            // 顶部居中展示
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_info_view, null)
        floatingView?.run {
            val okIv = findViewById<ImageView>(R.id.iv_ok)
            val closeIv = findViewById<ImageView>(R.id.iv_close)
            Log.i(TAG, "closeIv tint mode: ${closeIv.imageTintMode?.name}")

            setOnTouchListener(FloatingViewTouchListener(layoutParams, windowManager))
            windowManager.addView(this, layoutParams)

            okIv.setSafeClickListener {
                finishWithResult()
            }

            closeIv.setSafeClickListener {
                finishWithoutSave()
            }
        }
    }

    /**
     * 移除悬浮窗，避免窗口泄露
     */
    private fun removeFloatView() {
        windowManager.removeViewImmediate(floatingView)
        floatingView = null
    }

    class FloatingViewTouchListener(
        private val layoutParams: WindowManager.LayoutParams,
        private val windowManager: WindowManager
    ) :
        View.OnTouchListener {
        private var x = 0
        private var y = 0

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = motionEvent.rawX.toInt()
                    y = motionEvent.rawY.toInt()

                }

                MotionEvent.ACTION_MOVE -> {
                    val nowX = motionEvent.rawX.toInt()
                    val nowY = motionEvent.rawY.toInt()
                    val movedX = nowX - x
                    val movedY = nowY - y
                    x = nowX
                    y = nowY
                    layoutParams.apply {
                        x += movedX
                        y += movedY
                    }
                    //更新悬浮球控件位置
                    windowManager.updateViewLayout(view, layoutParams)
                }

                else -> {

                }
            }
            return false
        }
    }
}