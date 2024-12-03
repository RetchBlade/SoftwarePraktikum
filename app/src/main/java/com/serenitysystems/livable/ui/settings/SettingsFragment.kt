package com.serenitysystems.livable.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPic = binding.profileImage
        // Beobachten der Benutzerdaten
        // Das Benutzerbild vom ViewModel beobachten
        viewModel.userPic.observe(viewLifecycleOwner, Observer { profileImageUrl ->
            if (profileImageUrl != null) {
                // Bild mit Glide laden
                Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.pp) // Platzhalterbild während des Ladens
                    .error(R.drawable.pp) // Fehlerbild, falls das Laden fehlschlägt
                    .into(userPic)
            } else {
                userPic.setImageResource(R.drawable.pp) // Platzhalter setzen, falls kein Bild vorhanden ist
            }
        })

        // Set up the listener for the "WG-Info" card click
        binding.wgInfoCard.setOnClickListener {
            // Navigate to the WG Ansicht fragment
            findNavController().navigate(R.id.action_settingsFragment_to_wgAnsichtFragment)
        }

        // You can also add other click listeners or functionality here for the other buttons
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profilverwaltenFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
