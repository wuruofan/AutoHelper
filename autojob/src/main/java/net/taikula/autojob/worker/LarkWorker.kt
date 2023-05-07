package net.taikula.autojob.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.taikula.autojob.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LarkWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val timeStart = "09:00:00"
    private val timeEnd = "10:30:00"


    override fun doWork(): Result {

        val currentTime = Calendar.getInstance().time

        // Set the start and end times for the desired range
        val startTime = simpleDateFormat.parse(timeStart)
        val endTime = simpleDateFormat.parse(timeEnd)

        // Check if the current time falls within the desired range
        if (currentTime.after(startTime) && currentTime.before(endTime)) {
            AppUtils.startLark(applicationContext)
        }

        // 返回任务是否成功完成的结果
        return Result.success()
    }
}