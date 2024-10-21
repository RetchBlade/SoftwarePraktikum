
package com.serenitysystems.livable.ui.userprofil


data class Profile(
    val profilePictureUrl: String, // URL of the profile picture
    val email: String,
    val username: String,
    val birthdate: String,
    val wgRole: String, // WG Role
    val gender: String // Gender
)