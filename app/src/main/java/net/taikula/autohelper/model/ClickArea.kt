package net.taikula.autohelper.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect


/**
 * 点击区域
 */
class ClickArea : java.io.Serializable {
    private val lineList: ArrayList<LineInfo> = ArrayList()
    var imagePath: String? = null
//    var outRect: Rect? = null

    @Transient
    var bitmap: Bitmap? = null

    @Transient
    var phash = 0L

    fun new(line: LineInfo) {
        reset()
        add(line)
    }

    fun reset() {
        lineList.clear()
    }

    fun add(line: LineInfo) {
        lineList.add(line)
    }

    fun pop() {
        lineList.removeLast()
    }

    fun isEmpty(): Boolean {
        return lineList.isEmpty()
    }

    fun draw(canvas: Canvas, painter: Paint) {
        for (line in lineList) {
            line.draw(canvas, painter)
        }
    }

    fun outlineRect(): Rect {
        var top = INVALID_VALUE
        var bottom = INVALID_VALUE
        var left = INVALID_VALUE
        var right = INVALID_VALUE

        for (line in lineList) {
            val tmpRect = line.outlineRect()
            if (top == INVALID_VALUE) {
                top = tmpRect.top
                bottom = tmpRect.bottom
                left = tmpRect.left
                right = tmpRect.right
                continue
            }

            if (tmpRect.top < top) {
                top = tmpRect.top
            }

            if (tmpRect.bottom > bottom) {
                bottom = tmpRect.bottom
            }

            if (tmpRect.left < left) {
                left = tmpRect.left
            }

            if (tmpRect.right > right) {
                right = tmpRect.right
            }
        }

//        if (top == INVALID_VALUE) {
//            return null
//        }

        return Rect(left, top, right, bottom)
    }

    fun randomPoint(): PointInfo {
        return lineList.random().randomPoint()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ClickArea)
            return false

        return lineList.size == other.lineList.size
                && imagePath?.equals(other.imagePath) ?: false
    }

    override fun toString(): String {
        val rect = outlineRect()
        return "区域: ${rect.left}, ${rect.top} - ${rect.right}, ${rect.bottom}"
    }

    override fun hashCode(): Int {
        var result = lineList.hashCode()
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        return result
    }

    /******************* inner class *******************/

    class LineInfo constructor(startPoint: PointInfo? = null) : java.io.Serializable {
        private val pointList: ArrayList<PointInfo> = ArrayList()

        init {
            startPoint?.let {
                pointList.add(startPoint)
            }
        }

        fun append(p: PointInfo): LineInfo {
            pointList.add(p)
            return this
        }

        fun draw(canvas: Canvas, painter: Paint) {
            if (pointList.size <= 1)
                return

            for (i in 0 until pointList.size - 1) {
                val startPoint = pointList[i]
                val endPoint = pointList[i + 1]
                canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, painter)
            }
        }

        fun outlineRect(): Rect {
            var top = INVALID_VALUE.toFloat()
            var bottom = INVALID_VALUE.toFloat()
            var left = INVALID_VALUE.toFloat()
            var right = INVALID_VALUE.toFloat()

            for (point in pointList) {
                if (top == INVALID_VALUE.toFloat()) {
                    top = point.y
                    bottom = point.y
                    left = point.x
                    right = point.x

                    continue
                }

                if (point.x < left) {
                    left = point.x
                } else if (point.x > right) {
                    right = point.x
                }

                if (point.y < top) {
                    top = point.y
                } else if (point.y > bottom) {
                    bottom = point.y
                }
            }
            return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }

        fun randomPoint(): PointInfo {
            return pointList.random()
        }
    }

    data class PointInfo(val x: Float, val y: Float) : java.io.Serializable

    companion object {
        const val INVALID_VALUE = -1


    }

}