package net.taikula.autohelper.tools

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.lang.reflect.Field
import java.lang.reflect.Method


object FloatWindowUtils {
    /**
     * 判断是否开启悬浮窗权限
     */
    fun isPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                var cls = Class.forName("android.content.Context")
                val declaredField: Field = cls.getDeclaredField("APP_OPS_SERVICE")
                declaredField.isAccessible = true
                var obj: Any? = declaredField.get(cls) as? String ?: return false
                val str2 = obj as String
                obj = cls.getMethod("getSystemService", String::class.java).invoke(context, str2)
                cls = Class.forName("android.app.AppOpsManager")
                val declaredField2: Field = cls.getDeclaredField("MODE_ALLOWED")
                declaredField2.isAccessible = true
                val checkOp: Method = cls.getMethod(
                    "checkOp", Integer.TYPE, Integer.TYPE,
                    String::class.java
                )
                val result =
                    checkOp.invoke(obj, 24, Binder.getCallingUid(), context.packageName) as Int
                return result == declaredField2.getInt(cls)
            } catch (e: Exception) {
                return false
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appOpsMgr = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsMgr.checkOpNoThrow(
                    "android:system_alert_window", Process.myUid(), context
                        .packageName
                )
                return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED
            } else {
                return Settings.canDrawOverlays(context)
            }
        }
    }


    fun requestPermission(activity: Activity) {
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt >= Build.VERSION_CODES.O) { //8.0以上
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            activity.startActivityForResult(intent, 0x5252, null)
        } else if (sdkInt >= Build.VERSION_CODES.M) { //6.0-8.0
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:" + activity.packageName)
            activity.startActivityForResult(intent, 0x5252)
        } else { //4.4-6.0以下
            //无需处理了
        }
    }
}