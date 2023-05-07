package net.taikula.autojob.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoWorkerManager {

    fun startLarkJob(context: Context) {
        // 创建一个约束，使任务只在设备联网时运行
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 创建一个PeriodicWorkRequest，以便每隔1小时运行任务
        val periodicWorkRequest = PeriodicWorkRequestBuilder<LarkWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WorkerConstants.WORKER_TAG_LARK)
            .build()


        // 调度任务
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkerConstants.WORKER_TAG_LARK,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )
    }
}