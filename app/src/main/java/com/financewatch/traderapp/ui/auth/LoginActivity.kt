package com.financewatch.traderapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.financewatch.traderapp.auth.AuthRepository
import com.financewatch.traderapp.databinding.ActivityLoginBinding
import com.financewatch.traderapp.ui.MainActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Ecranul de autentificare.
 * Foloseste ViewBinding pentru acces sigur la view-uri (fara findViewById).
 * Autentificarea se face prin Firebase Auth via AuthRepository.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Buton Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }

        // Link catre ecranul de inregistrare
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Introdu un email valid"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Parola trebuie sa aiba minim 6 caractere"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            val result = authRepository.login(email, password)

            result.onSuccess {
                // Login reusit → navigheaza la MainActivity si inchide LoginActivity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }

            result.onFailure { error ->
                setLoading(false)
                Snackbar.make(binding.root, error.message ?: "Eroare la autentificare", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
