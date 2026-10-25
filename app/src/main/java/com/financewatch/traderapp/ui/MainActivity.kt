package com.financewatch.traderapp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.financewatch.traderapp.R
import com.financewatch.traderapp.databinding.ActivityMainBinding
import com.financewatch.traderapp.receiver.NetworkChangeReceiver
import com.financewatch.traderapp.service.NewsSyncService
import com.financewatch.traderapp.util.Constants
import com.google.android.material.snackbar.Snackbar

/**
 * Activity principala — host-ul pentru toate Fragment-urile.
 * Contine BottomNavigationView + NavController pentru navigare.
 * Asculta broadcasturile de la NetworkChangeReceiver.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val connectivityReceiver = NetworkChangeReceiver()

    // Receiver pentru schimbari de conectivitate
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isConnected = intent.getBooleanExtra(Constants.EXTRA_IS_CONNECTED, true)
            if (!isConnected) {
                Snackbar.make(binding.root, "Fara conexiune la internet", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Seteaza Toolbar-ul ca ActionBar
        setSupportActionBar(binding.toolbar)

        setupNavigation()

        val prefs = getSharedPreferences(Constants.PREFS_SETTINGS, MODE_PRIVATE)
        if (prefs.getBoolean(Constants.KEY_BACKGROUND_SYNC, false)) {
            NewsSyncService.start(this)
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Top-level destinations — fara buton Back in toolbar
        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.newsListFragment,
                R.id.bookmarksFragment,
                R.id.marketsFragment,
                R.id.settingsFragment
            )
        )

        // Conecteaza Toolbar-ul la NavController (afiseaza Back arrow pe detail)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)

        // Conecteaza BottomNavigationView la NavController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        // Inregistreaza receiver pentru detectia conexiunii
        LocalBroadcastManager.getInstance(this).registerReceiver(
            networkReceiver,
            IntentFilter(Constants.ACTION_CONNECTIVITY_CHANGED)
        )
        @Suppress("DEPRECATION")
        ContextCompat.registerReceiver(
            this,
            connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(networkReceiver)
        unregisterReceiver(connectivityReceiver)
    }
}
