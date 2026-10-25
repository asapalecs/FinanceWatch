package com.financewatch.traderapp.auth

import android.content.Context
import android.content.SharedPreferences
import com.financewatch.traderapp.FinanceWatchApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Repository pentru autentificare.
 *
 * Strategie dual-layer:
 *  1. Incearca Firebase Auth (cloud) — functioneaza cand google-services.json e real
 *  2. Fallback automat la autentificare locala (SharedPreferences) — functioneaza
 *     si fara un proiect Firebase configurat (ex. in mediu de dev/facultate)
 *
 * Parola e stocata local ca SHA-256 hash (niciodata in clar).
 */
class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val prefs: SharedPreferences by lazy {
        FinanceWatchApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "local_auth_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_EMAIL = "current_email"
        private const val ACC_PREFIX = "acc_"
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Autentifica utilizatorul.
     * Incearca Firebase; daca Firebase nu e configurat corect, foloseste auth local.
     * Returneaza Result.success(email) sau Result.failure(Exception).
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Autentificare esuata")
            Result.success(user.email ?: email)
        } catch (e: Exception) {
            if (isConfigurationError(e)) {
                localLogin(email, password)
            } else {
                Result.failure(mapAuthError(e))
            }
        }
    }

    /**
     * Creeaza un cont nou.
     * Incearca Firebase; daca Firebase nu e configurat, salveaza contul local.
     */
    suspend fun register(email: String, password: String): Result<String> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Inregistrare esuata")
            Result.success(user.email ?: email)
        } catch (e: Exception) {
            if (isConfigurationError(e)) {
                localRegister(email, password)
            } else {
                Result.failure(mapAuthError(e))
            }
        }
    }

    /**
     * Deconecteaza utilizatorul (Firebase + local).
     */
    fun logout() {
        firebaseAuth.signOut()
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .remove(KEY_EMAIL)
            .apply()
    }

    /**
     * Returneaza emailul utilizatorului curent, sau null daca nu e logat.
     */
    fun getCurrentEmail(): String? {
        return firebaseAuth.currentUser?.email
            ?: if (prefs.getBoolean(KEY_LOGGED_IN, false))
                prefs.getString(KEY_EMAIL, null)
            else
                null
    }

    /**
     * Verifica daca exista o sesiune activa (Firebase sau locala).
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null ||
               prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    // ─── Local Auth ────────────────────────────────────────────────────────────

    private fun localLogin(email: String, password: String): Result<String> {
        val storedHash = prefs.getString("$ACC_PREFIX${email.lowercase()}", null)
        return when {
            storedHash == null ->
                Result.failure(Exception("Contul nu exista. Inregistreaza-te mai intai."))
            storedHash == hashPassword(password) -> {
                prefs.edit()
                    .putBoolean(KEY_LOGGED_IN, true)
                    .putString(KEY_EMAIL, email)
                    .apply()
                Result.success(email)
            }
            else ->
                Result.failure(Exception("Email sau parola incorecta."))
        }
    }

    private fun localRegister(email: String, password: String): Result<String> {
        val key = "$ACC_PREFIX${email.lowercase()}"
        return if (prefs.contains(key)) {
            Result.failure(Exception("Acest email este deja inregistrat."))
        } else {
            prefs.edit()
                .putString(key, hashPassword(password))
                .apply()
            Result.success(email)
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Detecteaza daca eroarea Firebase provine dintr-o configurare incorecta
     * (google-services.json fake/lipsa, reCAPTCHA, etc.) si NU dintr-o
     * parola gresita sau email inexistent.
     */
    private fun isConfigurationError(e: Exception): Boolean {
        val msg = (e.message ?: "").lowercase()
        val causeMsg = (e.cause?.message ?: "").lowercase()
        val combined = "$msg $causeMsg"
        return combined.contains("recaptcha") ||
               combined.contains("app-not-authorized") ||
               combined.contains("missing_client") ||
               combined.contains("internal error") ||
               combined.contains("network error") ||
               combined.contains("network_error") ||
               combined.contains("timeout") ||
               combined.contains("connection") ||
               combined.contains("unable to resolve") ||
               combined.contains("failed to connect")
    }

    private fun mapAuthError(e: Exception): Exception {
        val message = when {
            e.message?.contains("email address is already in use") == true ->
                "Acest email este deja inregistrat."
            e.message?.contains("password is invalid") == true ||
            e.message?.contains("no user record") == true ->
                "Email sau parola incorecta."
            e.message?.contains("badly formatted") == true ->
                "Formatul email-ului este invalid."
            e.message?.contains("network error") == true ->
                "Eroare de retea. Verifica conexiunea."
            else -> "Eroare: ${e.message}"
        }
        return Exception(message)
    }
}
