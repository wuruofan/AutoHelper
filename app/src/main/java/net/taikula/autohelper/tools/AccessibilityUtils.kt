package net.taikula.autohelper.tools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.provider.Settings
import android.text.TextUtils
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import net.taikula.autohelper.tools.Extensions.TAG

object AccessibilityUtils {

    /**
     * 是否授予辅助功能权限
     */
    fun isPermissionGranted(serviceName: String, ctx: Context): Boolean {
        try {
            val enable = Settings.Secure.getInt(
                ctx.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )

            if (enable != 1)
                return false

            val services = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            if (TextUtils.isEmpty(services)) {
                return false
            }

            // 尝试遍历所有已开启辅助功能的服务名
            val split = SimpleStringSplitter(':')
            split.setString(services)

            while (split.hasNext()) {
                if (split.next()
                        .equals(ctx.packageName + "/" + serviceName, ignoreCase = true)
                ) {
                    return true
                }
            }
        } catch (e: Throwable) {
            Log.e(ContentValues.TAG, "isPermissionGranted", e)
        }

        return false
    }

    /**
     * 请求辅助功能权限
     */
    fun requestPermission(cxt: Context) {
        try {
            cxt.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                cxt.startActivity(intent)
            } catch (e: Throwable) {
                Log.e(ContentValues.TAG, "requestPermission", e)
            }
        }
    }

    /**
     * 点击指定坐标，点击时长在50ms到200ms之间随机
     *
     * 参考：https://juejin.cn/post/7111372688392159268/
     * https://codelabs.developers.google.com/codelabs/developing-android-a11y-service#7
     */
    fun AccessibilityService.click(x: Float, y: Float) {
        Log.d(TAG, "click: ($x, $y)")

        val path = Path().apply {
            moveTo(x, y)
//            lineTo(x, y)
        }

        val builder = GestureDescription.Builder()
        builder.addStroke(
            GestureDescription.StrokeDescription(
                path,
                0,
                (50..200).random().toLong()
            )
        )
        this.dispatchGesture(
            builder.build(),
            object : AccessibilityService.GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "click onCancelled!")
                }

                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.w(TAG, "click onCompleted!")
                }
            },
            null
        )
    }
}