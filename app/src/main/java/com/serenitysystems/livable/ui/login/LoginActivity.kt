package com.serenitysystems.livable.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.serenitysystems.livable.MainActivity
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ActivityLoginBinding
import com.serenitysystems.livable.ui.register.RegistrierungFragment

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
                ) // R.id.fragment_container sollte das ID des Containers sein, in den du das Fragment einfügen willst.
                .addToBackStack(null)
                .commit()
        }

            binding.loginButton.setOnClickListener {
                val email = binding.usernameEditText.text.toString()
                val password = binding.editTextNumberPassword.text.toString()
                loginViewModel.login(email, password)
            }

            // Beobachte das Login-Ergebnis
            loginViewModel.loginResult.observe(this, Observer { isSuccess ->
                if (isSuccess) {
                    navigateToMainActivity()
                } else {
                    // Zeige Login-Fehler an
                    Toast.makeText(this, "Login fehlgeschlagen", Toast.LENGTH_SHORT).show()
                }
            })
        }

        private fun navigateToMainActivity() {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()

    }
}
