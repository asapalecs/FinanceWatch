package com.financewatch.traderapp.data.local.dao

import androidx.room.*
import com.financewatch.traderapp.data.local.entity.MarketPreferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO pentru preferintele pietelor urmarite de utilizator.
 */
@Dao
interface MarketPreferenceDao {

    /**
     * Returneaza toate pietele disponibile in aplicatie.
     */
    @Query("SELECT * FROM market_preferences ORDER BY market_id")
    fun getAllMarkets(): Flow<List<MarketPreferenceEntity>>

    /**
     * Returneaza doar pietele activate de utilizator.
     * Folosit de Repository pentru a sti ce categorii sa descarce.
     */
    @Query("SELECT * FROM market_preferences WHERE is_enabled = 1 ORDER BY market_id")
    fun getEnabledMarkets(): Flow<List<MarketPreferenceEntity>>

    /**
     * Insereaza o piata. REPLACE suprascrie daca exista deja.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(market: MarketPreferenceEntity)

    /**
     * Insereaza mai multe piete deodata (folosit la popularea initiala).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(markets: List<MarketPreferenceEntity>)

    /**
     * Activeaza sau dezactiveaza o piata dupa ID.
     */
    @Query("UPDATE market_preferences SET is_enabled = :isEnabled WHERE market_id = :marketId")
    suspend fun updateEnabled(marketId: String, isEnabled: Boolean)

    /**
     * Returneaza numarul de piete (sincrona, pentru verificare la startup).
     */
    @Query("SELECT COUNT(*) FROM market_preferences")
    suspend fun getCount(): Int
}
