package net.taikula.autohelper.model

import net.taikula.autohelper.data.db.entity.ClickData


/**
 * 当前执行的点击任务
 */
class ClickTask(clickData: List<ClickData>) {
    var runningCount = 0

    val currentIndex
        get() = if (clickList.isEmpty()) 0 else runningCount % clickList.size

    val clickList: List<ClickData> = clickData

    val currentClickArea: ClickArea
        get() = clickList[currentIndex].clickArea

    val currentDstPHash: Long
        get() = clickList[currentIndex].clickArea.phash

    val currentClickPoint: ClickArea.PointInfo
        get() = clickList[currentIndex].clickArea.randomPoint()

    override fun toString(): String {
        return "[ClickTask: currentIndex=$currentIndex, runningCount=$runningCount, clickArea=${currentClickArea.outlineRect()}]"
    }
}