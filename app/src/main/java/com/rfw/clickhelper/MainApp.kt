package com.rfw.clickhelper

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class MainApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
            private set

        const val NOTIFICATION_CHANNEL_ID = "running"

        private fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "正在识别屏幕...",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        createNotificationChannel(this)
    }
}