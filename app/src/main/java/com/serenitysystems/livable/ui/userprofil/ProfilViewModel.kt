package com.serenitysystems.livable.ui.userprofil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel



class ProfilViewModel : ViewModel() {

    // MutableLiveData for the profile data
    private val _profileData = MutableLiveData<Profile>()
    val profileData: LiveData<Profile> = _profileData

    // Simulate loading profile data
    init {
        loadProfileData()
    }

    // This function simulates data loading (you can replace it with real data fetching)
    private fun loadProfileData() {
        // Simulating a profile object (replace with real data fetching)
        val exampleProfile = Profile(
            profilePictureUrl = "https://example.com/profile-pic.jpg", // Replace with a real image URL
            email = "user@example.com",
            username = "Max Mustermann",
            birthdate = "01. Januar 1990",
            wgRole = "Mitbewohner",
            gender = "Männlich"
        )

        // Set the data to the LiveData
        _profileData.value = exampleProfile
    }
}
