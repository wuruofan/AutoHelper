package com.rfw.clickhelper.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.rfw.clickhelper.R
import com.rfw.clickhelper.model.ClickArea
import com.rfw.clickhelper.model.ClickAreaModel
import com.rfw.clickhelper.tool.Extensions.TAG


/**
 * 参考：https://github.com/452896915/SnapShotMonitor
 */
class DoodleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    private val lineList: ArrayList<ClickArea.LineInfo> = ArrayList()
    private val painter = Paint()

    private val rectPainter = Paint()

    private lateinit var currentLine: ClickArea.LineInfo
    private lateinit var outlineRect: Rect

    init {
        configPaint()
    }

    private fun configPaint() {
        painter.color = resources.getColor(R.color.light_pink, null)
        painter.strokeWidth = 20F

        rectPainter.style = Paint.Style.STROKE
        rectPainter.strokeWidth = 5F
        rectPainter.color = resources.getColor(R.color.snow_purple, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val point = ClickArea.PointInfo(event.x, event.y)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentLine = ClickArea.LineInfo()
                    ClickAreaModel.clickArea.new(currentLine)
                    drawPointOfLine(currentLine, point)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    drawPointOfLine(currentLine, point)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    drawPointOfLine(currentLine, point)
                }
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null)
            return

        ClickAreaModel.clickArea.draw(canvas, painter)

        outlineRect = ClickAreaModel.clickArea.outlineRect()

        if (outlineRect.left != -1) {
            canvas.drawRect(outlineRect, rectPainter)
//            Log.w(TAG, "onDraw outlineRect=$outlineRect")
        }
    }

    private fun drawPointOfLine(line: ClickArea.LineInfo, point: ClickArea.PointInfo) {
        line.append(point)
        invalidate()
    }

    fun doodledBitmap(): Bitmap? {
        if (drawable == null)
            return null;
        val bitmap = (drawable as BitmapDrawable).bitmap
        Log.w(TAG, "doodledBitmap: origin size ${bitmap.width} x ${bitmap.height}")
        return cropDoodleRectBitmap(bitmap)
    }

    private fun cropDoodleRectBitmap(bitmap: Bitmap): Bitmap? {
        val rect = ClickAreaModel.clickArea.outlineRect()
        if (rect.left == -1) {
            return null
        }

        Log.w(TAG, "cropDoodleRectBitmap outlineRect=$rect")
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }
}