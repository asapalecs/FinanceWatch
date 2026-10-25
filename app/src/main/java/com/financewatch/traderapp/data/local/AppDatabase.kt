package com.financewatch.traderapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financewatch.traderapp.data.local.dao.MarketPreferenceDao
import com.financewatch.traderapp.data.local.dao.NewsDao
import com.financewatch.traderapp.data.local.dao.SavedNewsDao
import com.financewatch.traderapp.data.local.entity.MarketPreferenceEntity
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.data.local.entity.SavedNewsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Baza de date Room a aplicatiei FinanceWatch.
 *
 * Contine 3 tabele:
 *  - news_cache: stirile descarcate din API (expira dupa 24h)
 *  - saved_news: stirile salvate de utilizator (bookmarks permanente)
 *  - market_preferences: pietele urmarite (Nasdaq, Gold, Forex, Crypto)
 *
 * Implementeaza pattern Singleton pentru a evita crearea multipla
 * a instantei bazei de date (cost ridicat de resurse).
 */
@Database(
    entities = [
        NewsEntity::class,
        SavedNewsEntity::class,
        MarketPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun newsDao(): NewsDao
    abstract fun savedNewsDao(): SavedNewsDao
    abstract fun marketPreferenceDao(): MarketPreferenceDao

    companion object {
        // @Volatile garanteaza ca INSTANCE e vizibila imediat tuturor thread-urilor
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returneaza instanta singleton a bazei de date.
         * Daca nu exista, o creeaza thread-safe cu synchronized.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financewatch.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration() // sterge si recreaza la schimbare schema (dev only)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Callback apelat la prima creare a bazei de date.
     * Populeaza tabelul market_preferences cu datele initiale.
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prepopulateMarkets(database.marketPreferenceDao())
                }
            }
        }

        /**
         * Insereaza pietele disponibile in aplicatie.
         * Aceasta lista poate fi extinsa cu piete noi in versiuni viitoare.
         */
        private suspend fun prepopulateMarkets(dao: MarketPreferenceDao) {
            val markets = listOf(
                MarketPreferenceEntity(
                    marketId = "nasdaq",
                    displayName = "Nasdaq / S&P500",
                    isEnabled = true,
                    queryKeyword = "Nasdaq OR S&P500 OR stock market"
                ),
                MarketPreferenceEntity(
                    marketId = "gold",
                    displayName = "Aur (XAUUSD)",
                    isEnabled = true,
                    queryKeyword = "gold price OR XAUUSD OR gold market"
                ),
                MarketPreferenceEntity(
                    marketId = "forex",
                    displayName = "Forex (EURUSD)",
                    isEnabled = true,
                    queryKeyword = "EURUSD OR euro dollar OR forex currency"
                ),
                MarketPreferenceEntity(
                    marketId = "crypto",
                    displayName = "Crypto",
                    isEnabled = false,
                    queryKeyword = "Bitcoin OR Ethereum OR cryptocurrency"
                ),
                MarketPreferenceEntity(
                    marketId = "general",
                    displayName = "Stiri Generale",
                    isEnabled = true,
                    queryKeyword = "financial news OR economy OR market"
                )
            )
            dao.insertAll(markets)
        }
    }
}
