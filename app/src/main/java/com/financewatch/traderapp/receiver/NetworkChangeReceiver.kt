package com.financewatch.traderapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.financewatch.traderapp.util.Constants

/**
 * Broadcast Receiver care detecteaza schimbarile de conectivitate la internet.
 *
 * Cand conexiunea se schimba (WiFi / Mobile Data activat/dezactivat),
 * trimite un broadcast local pe care MainActivity il asculta
 * pentru a afisa/ascunde mesajul "Fara conexiune la internet".
 *
 * Aceasta componenta demonstreaza utilizarea BroadcastReceiver in Android.
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isConnected = isNetworkAvailable(context)

        // Trimite un broadcast local cu starea conexiunii
        val localIntent = Intent(Constants.ACTION_CONNECTIVITY_CHANGED).apply {
            putExtra(Constants.EXTRA_IS_CONNECTED, isConnected)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
    }

    /**
     * Verifica disponibilitatea retelei, compatibil cu toate versiunile Android.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }
}
