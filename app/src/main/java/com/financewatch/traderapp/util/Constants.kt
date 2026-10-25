package com.financewatch.traderapp.util

object Constants {
    const val DATABASE_NAME = "financewatch.db"

    // Cache-ul expira dupa 24 de ore
    const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L

    // Sincronizare la fiecare 30 de minute
    const val SYNC_INTERVAL_MS = 30 * 60 * 1000L

    const val PAGE_SIZE = 20

    val MARKETS = listOf("nasdaq", "gold", "forex", "crypto", "general")

    // Actiune broadcast pentru schimbari de conectivitate
    const val ACTION_CONNECTIVITY_CHANGED = "com.financewatch.ACTION_CONNECTIVITY_CHANGED"
    const val EXTRA_IS_CONNECTED = "is_connected"

    // Notificari
    const val PREFS_SETTINGS = "settings"
    const val KEY_BACKGROUND_SYNC = "background_sync"

    const val NOTIFICATION_CHANNEL_ID = "news_sync_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Sincronizare stiri"
}
