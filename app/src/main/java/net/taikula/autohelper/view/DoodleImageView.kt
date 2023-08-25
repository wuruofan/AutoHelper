package net.taikula.autohelper.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.taikula.autohelper.R
import net.taikula.autohelper.model.ClickArea
import net.taikula.autohelper.tools.ColorUtils
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.FileUtils
import java.util.*


/**
 * 支持涂抹的 ImageView
 * 参考：https://github.com/452896915/SnapShotMonitor
 */
class DoodleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    /**
     * 涂抹路径的画笔
     */
    private val painter = Paint()

    /**
     * 整个涂抹区域外框矩形的画笔
     */
    private val rectPainter = Paint()

    /**
     * 当前涂抹的路径
     */
    private var currentLine: ClickArea.LineInfo = ClickArea.LineInfo()

    /**
     * 涂抹区域
     */
    var clickArea: ClickArea? = null

    /**
     * 涂抹区域裁剪后保存的图片名称
     */
    private lateinit var imageName: String

    /**
     * 协程相关
     */
    private val job by lazy { Job() }
    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + job) }

    init {
        configPaint()
    }

    /**
     * 配置画笔
     */
    private fun configPaint() {
        painter.color = ColorUtils.getColor(this@DoodleImageView.context, R.attr.colorPrimary)
        painter.strokeWidth = 20F

        rectPainter.style = Paint.Style.STROKE
        rectPainter.strokeWidth = 5F
        rectPainter.color = ColorUtils.getColor(this@DoodleImageView.context, R.attr.colorSecondary)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        job.cancel()
        super.onDetachedFromWindow()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val point = ClickArea.PointInfo(event.x, event.y)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (clickArea == null) {
                        clickArea = ClickArea()
                        imageName = "${UUID.randomUUID()}.jpg"
                    } else {
//                        TODO("edit click area!")
                    }

                    currentLine.reset()
                    currentLine.append(point)
                    clickArea?.new(currentLine)
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    currentLine.append(point)
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    currentLine.append(point)
                    invalidate()

                    ioScope.launch {
                        saveDoodledImageCache()
                    }
                }
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null)
            return

        clickArea?.let {
            it.draw(canvas, painter)

            // 绘制边框
            val rect = it.outlineRect()
            if (ClickArea.isValid(rect)) {
                canvas.drawRect(rect, rectPainter)
//            Log.w(TAG, "onDraw outlineRect=$outlineRect")
            }
        }
    }

    /**
     * 保存涂抹的图片
     */
    fun saveDoodledImageCache() {
        val file = FileUtils.writeInnerFile(
            this@DoodleImageView.context,
            "image",
            imageName
        ) {
            val bitmap = doodledBitmap()
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                clickArea?.bitmap = bitmap
            }
        }

        if (file.exists() && file.length() > 0) {
            clickArea?.imagePath = file.absolutePath
        }
    }

    /**
     * 删除缓存的涂抹区域图片
     */
    fun removeDoodledImageCache() {
        clickArea?.run {
            if (!imagePath.isNullOrEmpty()) {
                val ret = FileUtils.delete(this.imagePath!!)
                Log.d(TAG, "delete success: ${this.imagePath}")
            }
        }
    }

    /**
     * 获取涂抹区域的图片
     */
    private fun doodledBitmap(): Bitmap? {
        if (drawable == null)
            return null;
        val bitmap = (drawable as BitmapDrawable).bitmap
        Log.w(TAG, "doodledBitmap: origin size ${bitmap.width} x ${bitmap.height}")
        return cropDoodleRectBitmap(bitmap)
    }

    /**
     * 截取涂抹区域的图片
     */
    private fun cropDoodleRectBitmap(bitmap: Bitmap): Bitmap? {
        if (clickArea == null) return null

        val rect = clickArea!!.outlineRect()
        if (!ClickArea.isValid(rect)) {
            return null
        }

        Log.w(TAG, "cropDoodleRectBitmap outlineRect=$rect")
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }
}