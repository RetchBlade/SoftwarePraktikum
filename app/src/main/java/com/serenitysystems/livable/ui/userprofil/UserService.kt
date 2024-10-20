package com.serenitysystems.livable.ui.userprofil

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// Interface für die User-Service API
interface UserService {

    // API call to get user details by ID
    @GET("user/{id}")
    fun getUser(@Path("id") userId: Int): Call<UserResponse>

    // API call to upload a profile picture
    @Multipart
    @POST("upload/profile-picture")
    fun uploadProfilePicture(@Part file: MultipartBody.Part): Call<UploadResponse>
}

// Datenklasse für die Antwort der getUser-Methode
data class UserResponse(
    val userId: Int,
    val email: String,
    val role: String
)

// Datenklasse für die Antwort der Bild-Upload-Methode
data class UploadResponse(
    val success: Boolean,
    val message: String
)
