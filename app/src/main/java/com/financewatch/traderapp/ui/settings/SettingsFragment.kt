package com.financewatch.traderapp.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.financewatch.traderapp.auth.AuthRepository
import com.financewatch.traderapp.databinding.FragmentSettingsBinding
import com.financewatch.traderapp.service.NewsSyncService
import com.financewatch.traderapp.ui.auth.LoginActivity
import com.financewatch.traderapp.util.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { setSync(true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepo = AuthRepository()

        // Afiseaza versiunea aplicatiei
        binding.tvVersion.text = "Versiune 1.0"

        // Afiseaza emailul utilizatorului logat
        binding.tvEmail.text = authRepo.getCurrentEmail() ?: "Necunoscut"

        // Buton de logout cu confirmare
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_SETTINGS, Context.MODE_PRIVATE)
        binding.switchSync.isChecked = prefs.getBoolean(Constants.KEY_BACKGROUND_SYNC, false)
        binding.switchSync.setOnCheckedChangeListener { _, checked ->
            val needsPermission = checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                setSync(checked)
            }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Deconectare")
                .setMessage("Esti sigur ca vrei sa te deconectezi?")
                .setPositiveButton("Da") { _, _ ->
                    setSync(false)
                    authRepo.logout()
                    startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("Nu", null)
                .show()
        }
    }

    private fun setSync(enabled: Boolean) {
        val context = context ?: return
        context.getSharedPreferences(Constants.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(Constants.KEY_BACKGROUND_SYNC, enabled).apply()
        if (enabled) NewsSyncService.start(context) else NewsSyncService.stop(context)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
