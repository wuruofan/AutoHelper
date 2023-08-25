package net.taikula.autohelper.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import net.taikula.autohelper.tools.Extensions.TAG


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

    /**
     * 清除之前数据，并加入一条线
     */
    fun new(line: LineInfo) {
        reset()
        append(line)
    }

    /**
     * 清除所有数据
     */
    fun reset() {
        lineList.clear()
    }

    /**
     * 加入一条线
     */
    fun append(line: LineInfo) {
        lineList.add(line)
    }

    /**
     * 移出最后一条线
     */
    fun pop() {
        lineList.removeLast()
    }

    /**
     * 是否数据为空
     */
    fun isEmpty(): Boolean {
        return lineList.isEmpty()
    }

    /**
     * 将当前所有线条信息，绘制到画布上
     */
    fun draw(canvas: Canvas, painter: Paint) {
        for (line in lineList) {
            line.draw(canvas, painter)
        }
    }


    /**
     * 外框矩形
     */
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

    /**
     * 返回涂抹区域里随机一个点
     */
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
        return "区域: ${rect.left},${rect.top}-${rect.right},${rect.bottom}"
    }

    override fun hashCode(): Int {
        var result = lineList.hashCode()
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        return result
    }

    /******************* inner class *******************/

    /**
     * 线条
     */
    class LineInfo constructor(startPoint: PointInfo? = null) : java.io.Serializable {
        private val pointList: ArrayList<PointInfo> = ArrayList()

        init {
            startPoint?.let {
                pointList.add(startPoint)
            }
        }

        /**
         * 清空数据
         */
        fun reset() {
            pointList.clear()
        }

        /**
         * 追加一个点
         */
        fun append(p: PointInfo): LineInfo {
            if (p == pointList.lastOrNull()) {
                Log.i(TAG, "point == last point")
            }
            pointList.add(p)
            return this
        }

        /**
         * 将线条绘制到画布上
         */
        fun draw(canvas: Canvas, painter: Paint) {
            if (pointList.size <= 1)
                return

            for (i in 0 until pointList.size - 1) {
                val startPoint = pointList[i]
                val endPoint = pointList[i + 1]
                canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, painter)
            }
        }

        /**
         * 线条的外框矩形
         */
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

        /**
         * 随机返回线条上的一个点
         */
        fun randomPoint(): PointInfo {
            return pointList.random()
        }
    }

    /**
     * 点
     */
    data class PointInfo(val x: Float, val y: Float) : java.io.Serializable {
        override fun equals(other: Any?): Boolean {
            if (other !is PointInfo)
                return false
            return this.x == other.x && this.y == other.y
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            return result
        }

        override fun toString(): String {
            return "[$x,$y]"
        }
    }

    companion object {
        const val INVALID_VALUE = -1

        /**
         * 判断矩形框范围是否有效
         */
        fun isValid(rect: Rect): Boolean {
            if (rect.left == INVALID_VALUE)
                return false

            if (rect.width() <= 0 || rect.height() <= 0)
                return false

            return true
        }
    }

}