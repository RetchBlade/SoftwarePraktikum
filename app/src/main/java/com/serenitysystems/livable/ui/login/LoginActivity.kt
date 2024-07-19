package com.serenitysystems.livable.ui.login

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
import com.serenitysystems.livable.ui.register.RegistrierungFragment
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Click Listener for registration
        binding.Registrieren.setOnClickListener {
            val fragment = RegistrierungFragment()
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    fragment
                )
                .addToBackStack(null)
                .commit()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.usernameEditText.text.toString()
            val password = binding.editTextNumberPassword.text.toString()
            lifecycleScope.launch {
                loginViewModel.loginUser(email, password)
            }
        }

        // Beobachte das Login-Ergebnis
        loginViewModel.loginSuccess.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                navigateToMainActivity()
            }
        })

        // Beobachte Login-Fehlermeldungen
        loginViewModel.loginError.observe(this, Observer { errorMessage ->
            binding.usernameEditText.error = null
            binding.editTextNumberPassword.error = null

            when (errorMessage) {
                "Benutzer existiert nicht" -> binding.usernameEditText.error = "Benutzer existiert nicht"
                "Falsches Passwort" -> binding.editTextNumberPassword.error = "Falsches Passwort"
                else -> binding.usernameEditText.error = errorMessage
            }
        })
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
