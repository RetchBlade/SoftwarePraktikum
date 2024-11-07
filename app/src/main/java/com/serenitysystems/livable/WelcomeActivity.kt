package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.serenitysystems.livable.ui.login.LoginActivity
import com.serenitysystems.livable.ui.login.data.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences // Variable für UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_page) // Setze das Layout für die Aktivität

        // Initialisiere UserPreferences
        userPreferences = UserPreferences(this)

        // Hole Referenzen zu den Views
        val logoImage: ImageView = findViewById(R.id.logoImage) // Logo-ImageView
        val motivText: TextView = findViewById(R.id.motivText) // Motiv-TextView

        // Lade Animationen
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_animation) // Lade Logo-Animation
        val textFadeIn = AnimationUtils.loadAnimation(this, R.anim.text_fade_in) // Lade Text-Fade-In-Animation

        // Setze Sichtbarkeit auf INVISIBLE, bevor die Animationen starten
        motivText.visibility = TextView.INVISIBLE

        // Starte beide Animationen gleichzeitig
        logoImage.startAnimation(logoAnim) // Starte Logo-Animation
        motivText.startAnimation(textFadeIn) // Starte Text-Animation

        // Navigiere nach einer Verzögerung
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserToken() // Überprüfe den UserToken und navigiere entsprechend
        }, 3000) // Verzögerung in Millisekunden
    }

    private fun checkUserToken() {
        lifecycleScope.launch {
            val userToken = userPreferences.userToken.first() // Hole den UserToken

            // Navigiere basierend darauf, ob der UserToken existiert
            val intent = if (userToken != null) {
                Intent(this@WelcomeActivity, MainActivity::class.java) // Navigiere zur MainActivity
            } else {
                Intent(this@WelcomeActivity, LoginActivity::class.java) // Navigiere zur LoginActivity
            }
            startActivity(intent) // Starte die entsprechende Aktivität
            finish() // Schließe die WelcomeActivity, damit die Benutzer nicht zurückkehren können
        }
    }
}
