package net.taikula.autojob.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log


object AppUtils {
    fun getAppProcessName(context: Context) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        // get all apps
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        for (i in apps.indices) {
            val targetActivity = apps[i].activityInfo.targetActivity
            val name = apps[i].activityInfo.packageName
            if (!name.contains("huawei") && !name.contains("android")) {
                Log.i(
                    "TAG", "getAppProcessName: " +
                            apps[i].activityInfo.applicationInfo.loadLabel(packageManager)
                                .toString() + "---" +
                            apps[i].activityInfo.packageName
                )
            }
        }
    }

    /**
     * 启动飞书
     */
    fun startLark(context: Context) {
        val packageName = "com.ss.android.lark"
        val activityPath = "com.ss.android.lark.main.app.MainActivity"
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK //可选
        val comp = ComponentName(packageName, activityPath)
        intent.component = comp
        context.startActivity(intent)
    }

}