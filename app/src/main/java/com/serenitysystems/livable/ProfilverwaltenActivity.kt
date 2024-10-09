package com.serenitysystems.livable

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException

class ProfilverwaltenActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profilverwalten)

        profileImageView = findViewById(R.id.profileImageView)
        val uploadImageButton = findViewById<Button>(R.id.uploadImageButton)

        // Open image chooser on button click
        uploadImageButton.setOnClickListener {
            openImageChooser()
        }
    }

    // Method to open the image chooser
    private fun openImageChooser() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), PICK_IMAGE_REQUEST)
    }

    private fun uploadImage() {
        imageUri?.let { uri ->
            Log.d("ImageUri", "Selected Image URI: $uri")
            val file = File(uri.path ?: return) // Convert Uri to File

            if (file.exists()) {
                val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("picture", file.name, requestBody)

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://your-backend-url.com/") // Replace with your backend URL
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(UserService::class.java)

                service.uploadProfilePicture(body).enqueue(object : Callback<UploadResponse> {
                    override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProfilverwaltenActivity, "Bild erfolgreich hochgeladen!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProfilverwaltenActivity, "Fehler beim Hochladen: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                        Toast.makeText(this@ProfilverwaltenActivity, "Fehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Die Datei existiert nicht", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Handle the result after selecting an image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            try {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                profileImageView.setImageBitmap(bitmap)

                // Call uploadImage method to upload the selected image to the server
                uploadImage()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
