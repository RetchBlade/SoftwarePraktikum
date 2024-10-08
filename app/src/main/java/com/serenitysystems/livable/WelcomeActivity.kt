package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.serenitysystems.livable.ui.login.LoginActivity  // Importiere die LoginActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_page)

        // Get references to the views
        val logoImage: ImageView = findViewById(R.id.logoImage)
        val motivText: TextView = findViewById(R.id.motivText)

        // Load animations
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val textFadeIn = AnimationUtils.loadAnimation(this, R.anim.text_fade_in)

        // Start logo animation
        logoImage.startAnimation(logoAnim)

        // Set an animation listener for when the logo animation ends
        logoAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                // Do something when animation starts (if needed)
            }

            override fun onAnimationEnd(animation: Animation) {
                // When the logo animation ends, fade in the text
                motivText.visibility = TextView.VISIBLE
                motivText.startAnimation(textFadeIn)
            }

            override fun onAnimationRepeat(animation: Animation) {
                // Handle animation repeat (if needed)
            }
        })

        // Navigate to the login activity after a 4-second delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@WelcomeActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finish WelcomeActivity so users can't go back to it
        }, 4000) // 4-second delay
    }
}
