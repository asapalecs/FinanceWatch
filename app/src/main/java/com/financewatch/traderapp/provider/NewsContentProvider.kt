package com.financewatch.traderapp.provider

import android.content.*
import android.database.Cursor
import android.net.Uri
import com.financewatch.traderapp.data.local.AppDatabase
import kotlinx.coroutines.runBlocking

/**
 * Content Provider care expune stirile din baza de date locala.
 *
 * Permite altor aplicatii sau widgeturi sa acceseze stirile salvate.
 * Suporta doua URI-uri:
 *  - content://com.financewatch.traderapp.provider/news     → cache stiri
 *  - content://com.financewatch.traderapp.provider/saved    → stiri salvate
 *
 * Aceasta componenta demonstreaza utilizarea Content Provider in Android.
 */
class NewsContentProvider : ContentProvider() {

    private lateinit var database: AppDatabase

    companion object {
        const val AUTHORITY = "com.financewatch.traderapp.provider"
        val NEWS_URI: Uri = Uri.parse("content://$AUTHORITY/news")
        val SAVED_URI: Uri = Uri.parse("content://$AUTHORITY/saved")

        // Coduri pentru UriMatcher
        private const val NEWS = 100
        private const val SAVED = 200

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "news", NEWS)
            addURI(AUTHORITY, "saved", SAVED)
        }
    }

    override fun onCreate(): Boolean {
        database = AppDatabase.getDatabase(context!!)
        return true
    }

    /**
     * Interogheaza baza de date si returneaza un Cursor.
     * Sincron (runBlocking) deoarece ContentProvider nu suporta coroutines nativ.
     */
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val db = database.openHelper.readableDatabase
        return when (uriMatcher.match(uri)) {
            NEWS -> db.query("SELECT * FROM news_cache ORDER BY published_at DESC LIMIT 50")
            SAVED -> db.query("SELECT * FROM saved_news ORDER BY saved_at DESC")
            else -> throw IllegalArgumentException("URI necunoscut: $uri")
        }
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        NEWS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.news"
        SAVED -> "vnd.android.cursor.dir/vnd.$AUTHORITY.saved"
        else -> null
    }

    // Insert, update, delete sunt read-only in acest provider
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
}
