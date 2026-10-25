package com.financewatch.traderapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.financewatch.traderapp.util.Constants

/**
 * Clasa Application — initializari globale la pornirea aplicatiei.
 * Declarata in AndroidManifest cu android:name=".FinanceWatchApp"
 */
class FinanceWatchApp : Application() {

    companion object {
        lateinit var instance: FinanceWatchApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificari pentru stiri financiare noi"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
