package net.taikula.autohelper

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
        net.taikula.autohelper.MainApp.Companion.appContext = applicationContext

        createNotificationChannel(this)
    }

    private fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            net.taikula.autohelper.MainApp.Companion.NOTIFICATION_CHANNEL_ID,
            "正在识别屏幕...",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
            private set

        const val NOTIFICATION_CHANNEL_ID = "running"
    }
}