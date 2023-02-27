package com.rfw.clickhelper.model

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

class ClickArea {
    private val lineList: ArrayList<LineInfo> = ArrayList()

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

    fun draw(canvas: Canvas, painter: Paint) {
        for (line in lineList) {
            line.draw(canvas, painter)
        }
    }

    fun outlineRect(): Rect {
        var top = -1
        var bottom = -1
        var left = -1
        var right = -1

        for (line in lineList) {
            val tmpRect = line.outlineRect()
            if (top == -1) {
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

//        if (top == -1) {
//            return null
//        }

        return Rect(left, top, right, bottom)
    }

    fun randomPoint(): PointInfo {
        return lineList.random().randomPoint()
    }

    /******************* inner class *******************/

    class LineInfo constructor(startPoint: PointInfo? = null) {
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
            var top = -1F
            var bottom = -1F
            var left = -1F
            var right = -1F

            for (point in pointList) {
                if (top == -1F) {
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

    data class PointInfo(val x: Float, val y: Float)
}