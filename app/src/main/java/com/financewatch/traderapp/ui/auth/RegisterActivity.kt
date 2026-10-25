package com.financewatch.traderapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.financewatch.traderapp.auth.AuthRepository
import com.financewatch.traderapp.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (validateInputs(email, password, confirm)) {
                performRegister(email, password)
            }
        }

        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun validateInputs(email: String, password: String, confirm: String): Boolean {
        var ok = true
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email invalid"; ok = false
        } else binding.tilEmail.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Minim 6 caractere"; ok = false
        } else binding.tilPassword.error = null

        if (password != confirm) {
            binding.tilConfirmPassword.error = "Parolele nu coincid"; ok = false
        } else binding.tilConfirmPassword.error = null

        return ok
    }

    private fun performRegister(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.register(email, password)

            result.onSuccess {
                Snackbar.make(binding.root, "Cont creat! Te poti autentifica acum.", Snackbar.LENGTH_LONG).show()
                finish() // Inapoi la Login
            }

            result.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                Snackbar.make(binding.root, error.message ?: "Eroare la inregistrare", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
