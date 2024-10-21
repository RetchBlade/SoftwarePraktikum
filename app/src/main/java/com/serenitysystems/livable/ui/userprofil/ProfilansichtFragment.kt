package com.serenitysystems.livable.ui.profilansicht

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentProfilansichtBinding
import com.bumptech.glide.Glide
import com.serenitysystems.livable.ui.userprofil.ProfilViewModel

class ProfilansichtFragment : Fragment() {

    private var _binding: FragmentProfilansichtBinding? = null
    private val binding get() = _binding!!

    private lateinit var profilViewModel: ProfilViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding
        _binding = FragmentProfilansichtBinding.inflate(inflater, container, false)

        // Initialize the ViewModel
        profilViewModel = ViewModelProvider(this).get(ProfilViewModel::class.java)

        // Set up the profile data dynamically
        setupProfileData()

        return binding.root
    }

    private fun setupProfileData() {
        // Observe the profile data from the ViewModel
        profilViewModel.profileData.observe(viewLifecycleOwner) { profile ->
            // Set profile picture (using Glide or any image loading library)
            Glide.with(this)
                .load(profile.profilePictureUrl)  // Profile picture URL
                .placeholder(R.drawable.pp) // Placeholder image
                .circleCrop()
                .into(binding.profilePicture)

            // Set the profile details
            binding.usernameText.text = profile.username
            binding.emailText.text = profile.email
            binding.birthdateText.text = profile.birthdate
            binding.roleText.text = profile.wgRole
            binding.genderText.text = profile.gender
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
