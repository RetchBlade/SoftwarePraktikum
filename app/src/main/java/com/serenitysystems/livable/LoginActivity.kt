package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.serenitysystems.livable.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Beispiel: Logik für den Login-Button
        binding.btnLogin.setOnClickListener {
            // Hier kannst du die Logik für die Anmeldung implementieren
            // Angenommen, der Login ist erfolgreich, dann navigiere zur MainActivity
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Optional: Beende die com.serenitysystems.livable.LoginActivity, um sie aus dem Back-Stack zu entfernen
    }
}
