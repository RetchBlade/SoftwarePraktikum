package com.serenitysystems.livable.ui.login

import RegistrierungFragment
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.serenitysystems.livable.MainActivity
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels() // ViewModel für die Anmeldung
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) // Binding für das Layout
        setContentView(binding.root)

        // Setze den Click Listener für die Registrierung
        binding.Registrieren.setOnClickListener {
            val fragment = RegistrierungFragment() // Erstelle eine Instanz des Registrierungsfragments
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // Ersetze den aktuellen Fragmentcontainer mit dem Registrierungsfragment
                .addToBackStack(null) // Füge die Transaktion zum Backstack hinzu
                .commit()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.usernameEditText.text.toString() // Hole die eingegebene E-Mail
            val password = binding.editTextNumberPassword.text.toString() // Hole das eingegebene Passwort
            lifecycleScope.launch {
                loginViewModel.loginUser(email, password) // Versuche, den Benutzer anzumelden
            }
        }

        // Beobachte das Login-Ergebnis
        loginViewModel.loginSuccess.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                navigateToMainActivity() // Navigiere zur Hauptaktivität bei erfolgreichem Login
            }
        })

        // Beobachte Login-Fehlermeldungen
        loginViewModel.loginError.observe(this, Observer { errorMessage ->
            binding.usernameEditText.error = null // Setze den Fehler für das E-Mail-Feld zurück
            binding.editTextNumberPassword.error = null // Setze den Fehler für das Passwortfeld zurück

            // Setze entsprechende Fehlermeldungen
            when (errorMessage) {
                "Benutzer existiert nicht" -> binding.usernameEditText.error = "Benutzer existiert nicht"
                "Falsches Passwort" -> binding.editTextNumberPassword.error = "Falsches Passwort"
                else -> binding.usernameEditText.error = errorMessage // Allgemeine Fehlermeldung
            }
        })
    }

    // Navigiere zur Hauptaktivität
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java) // Erstelle einen Intent für die Hauptaktivität
        startActivity(intent) // Starte die Hauptaktivität
        finish() // Schließe die Login-Aktivität, sodass der Benutzer nicht zurückkehren kann
    }
}
