package com.financewatch.traderapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitate Room pentru preferintele utilizatorului privind pietele urmarite.
 * Fiecare piata poate fi activata sau dezactivata din interfata.
 */
@Entity(tableName = "market_preferences")
data class MarketPreferenceEntity(
    // ID unic al pietei: "nasdaq", "gold", "forex", "crypto", "general"
    @PrimaryKey
    @ColumnInfo(name = "market_id")
    val marketId: String,

    // Numele afisat in interfata
    @ColumnInfo(name = "display_name")
    val displayName: String,

    // Daca piata este activata (include stirile ei in feed)
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    // Keyword-ul folosit in query-ul catre NewsAPI
    @ColumnInfo(name = "query_keyword")
    val queryKeyword: String
)
