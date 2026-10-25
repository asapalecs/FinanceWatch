package com.financewatch.traderapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.financewatch.traderapp.R
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.remote.RetrofitClient
import com.financewatch.traderapp.repository.NewsRepository
import com.financewatch.traderapp.ui.MainActivity
import com.financewatch.traderapp.util.Constants
import kotlinx.coroutines.*

/**
 * Service de fundal care sincronizeaza stirile periodic.
 *
 * Este un Foreground Service (necesar pentru Android 8+) care:
 *  1. Ruleaza in fundal la interval de 30 minute
 *  2. Descarca stiri noi din API
 *  3. Trimite notificare locala cand gaseste stiri noi
 *  4. Curata cache-ul mai vechi de 24 ore
 *
 * Aceasta componenta demonstreaza utilizarea Service in Android.
 */
class NewsSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: NewsRepository
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initializeaza repository-ul cu dependentele necesare
        val db = AppDatabase.getDatabase(applicationContext)
        repository = NewsRepository(
            newsDao = db.newsDao(),
            savedNewsDao = db.savedNewsDao(),
            apiService = RetrofitClient.newsApi
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Porneste serviciul in foreground cu o notificare persistenta
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())

        // Lanseaza coroutine-ul de sincronizare periodica
        if (syncJob?.isActive != true) {
            syncJob = serviceScope.launch { syncLoop() }
        }

        // START_STICKY: sistemul restarteaza serviciul daca e omorat
        return START_STICKY
    }

    /**
     * Bucla de sincronizare: ruleaza la fiecare 30 de minute.
     */
    private suspend fun syncLoop() {
        while (true) {
            syncAllCategories()
            delay(Constants.SYNC_INTERVAL_MS)
        }
    }

    /**
     * Sincronizeaza toate categoriile activate si trimite notificare daca sunt stiri noi.
     */
    private suspend fun syncAllCategories() {
        val categories = listOf("nasdaq", "gold", "forex", "crypto", "general")
        var totalNewArticles = 0

        categories.forEach { category ->
            totalNewArticles += repository.refreshNews(category).getOrNull()?.size ?: 0
        }

        // Curata cache-ul vechi dupa sincronizare
        repository.clearOldCache()

        // Trimite notificare daca sincronizarea a reusit
        if (totalNewArticles > 0) {
            sendNewsNotification("Stiri noi disponibile pentru pietele urmarite!")
        }
    }

    /**
     * Construieste si trimite o notificare locala cu stirile noi.
     */
    private fun sendNewsNotification(message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_news)
            .setContentTitle("FinanceWatch")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NEWS_NOTIFICATION_ID, notification)
    }

    /**
     * Creeaza canalul de notificari (obligatoriu pe Android 8.0+).
     */
    private fun createNotificationChannel() {
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

    /**
     * Notificarea foreground (obligatorie pentru ca serviciul sa ruleze in fundal).
     */
    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_news)
            .setContentTitle("FinanceWatch")
            .setContentText("Sincronizare stiri financiare...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null // Unbound service

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Anuleaza toate coroutine-urile la oprirea serviciului
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, NewsSyncService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NewsSyncService::class.java))
        }

        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val NEWS_NOTIFICATION_ID = 1002
    }
}
