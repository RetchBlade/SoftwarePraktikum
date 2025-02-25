package com.serenitysystems.livable.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var userPic: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserProfile()
        setupClickListeners()
        setupThemeToggle()
    }

    private fun setupUserProfile() {
        userPic = binding.profileImage

        // Beobachte das Benutzerprofilbild
        viewModel.userPic.observe(viewLifecycleOwner, Observer { profileImageUrl ->
            if (!profileImageUrl.isNullOrEmpty()) {
                // Bild mit Glide laden
                Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.pp)
                    .error(R.drawable.pp)
                    .into(userPic)
            } else {
                userPic.setImageResource(R.drawable.pp)
            }
        })
    }

    private fun setupClickListeners() {
        // Navigiere zur WG-Ansicht
        binding.wgInfoCard.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_wgAnsichtFragment)
        }

        // Navigiere zur Profilverwaltung
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profilverwaltenFragment)
        }

        // ✅ Corrected FAQ Navigation
        binding.faqCard.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_faqFragment)
        }
    }

    private fun setupThemeToggle() {
        val themeSwitch = binding.themeSwitch
        val themeLabel = binding.themeLabel
        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        val isDarkModeEnabled = sharedPref.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        themeSwitch.isChecked = isDarkModeEnabled
        themeLabel.text = if (isDarkModeEnabled) "Dunkel" else "Hell"

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            themeSwitch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
                themeSwitch.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )

            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            themeLabel.text = if (isChecked) "Dunkel" else "Hell"
        }
        userPic.setOnClickListener {
            findNavController().navigate(R.id.nav_profilansicht)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}