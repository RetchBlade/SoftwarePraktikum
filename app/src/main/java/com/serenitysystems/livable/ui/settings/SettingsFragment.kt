package com.serenitysystems.livable.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var userPic: ImageView
    private lateinit var wgIdText: TextView

    // Firestore-Instanz, um die WG-ID abzurufen
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserProfile()
        setupClickListeners()
        setupThemeToggle()
        setupWgId() // WG-ID-Logik initialisieren
    }

    private fun setupUserProfile() {
        userPic = binding.profileImage

        viewModel.userPic.observe(viewLifecycleOwner, Observer { profileImageUrl ->
            if (!profileImageUrl.isNullOrEmpty()) {
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
        // Navigiere zur FAQ-Seite
        binding.faqCard.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_faqFragment)
        }
    }

    private fun setupThemeToggle() {
        val themeSwitch = binding.themeSwitch
        val themeLabel = binding.themeLabel
        val sharedPref = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        val isDarkModeEnabled = sharedPref.getBoolean("dark_mode", false)

        // Setze den Night Mode entsprechend
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        themeSwitch.isChecked = isDarkModeEnabled
        themeLabel.text = if (isDarkModeEnabled) "Dunkel" else "Hell"

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            themeSwitch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).withEndAction {
                themeSwitch.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )

            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            themeLabel.text = if (isChecked) "Dunkel" else "Hell"
        }

        userPic.setOnClickListener {
            findNavController().navigate(R.id.nav_profilansicht)
        }
    }


    private fun setupWgId() {
        wgIdText = binding.wgIdText

        viewModel.fetchUserToken { userToken ->
            userToken?.let { token ->
                db.collection("users")
                    .document(token.email)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            wgIdText.text = "WG-ID: Nicht verfügbar"
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val wgId = snapshot.getString("wgId") ?: "Nicht verfügbar"
                            wgIdText.text = "WG-ID: $wgId"
                        }
                    }
            }
        }

        binding.copyWgIdIcon.setOnClickListener {
            copyToClipboard(wgIdText)
        }
    }

    private fun copyToClipboard(textView: TextView) {
        val wgId = textView.text.toString().removePrefix("WG-ID:").trim()
        if (wgId.isEmpty() || wgId == "Nicht verfügbar") {
            Snackbar.make(textView, "Keine WG-ID vorhanden", Snackbar.LENGTH_SHORT).show()
            return
        }

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WG ID", wgId)
        clipboard.setPrimaryClip(clip)

        textView.text = "✅ WG ID kopiert!"
        textView.postDelayed({
            textView.text = "WG-ID: $wgId"
        }, 1500)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
