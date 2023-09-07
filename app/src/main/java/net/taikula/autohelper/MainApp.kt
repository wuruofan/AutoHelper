package net.taikula.autohelper

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.kongzue.dialogx.DialogX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.taikula.autohelper.data.ClickRepository
import net.taikula.autohelper.data.db.ClickDatabase

class MainApp : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    private val database by lazy { ClickDatabase.instance }
    val repository by lazy { ClickRepository(database.clickDao()) }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // 动态主题颜色
        DynamicColors.applyToActivitiesIfAvailable(this)

        // 创建通知频道
        createNotificationChannel(this)

        // 初始化
        DialogX.init(this);
    }

    /**
     * 创建通知频道
     */
    private fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "运行状态",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
            private set

        const val NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}_Channel"
    }
}