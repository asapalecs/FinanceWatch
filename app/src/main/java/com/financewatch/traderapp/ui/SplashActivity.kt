package com.financewatch.traderapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.financewatch.traderapp.auth.AuthRepository
import com.financewatch.traderapp.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ecranul de splash — afisat la pornire.
 * Verifica daca utilizatorul este deja autentificat:
 *  - DA → MainActivity direct
 *  - NU → LoginActivity
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            delay(1200) // Arata splash-ul 1.2 secunde

            val authRepo = AuthRepository()
            val destination = if (authRepo.isLoggedIn()) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }

            startActivity(Intent(this@SplashActivity, destination))
            finish() // Inchide SplashActivity — nu e accesibila cu Back
        }
    }
}
