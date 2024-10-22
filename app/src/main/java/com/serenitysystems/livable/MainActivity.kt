package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import com.bumptech.glide.Glide
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.serenitysystems.livable.data.UserPreferences
import com.serenitysystems.livable.databinding.ActivityMainBinding
import com.serenitysystems.livable.ui.login.LoginActivity
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Konfiguration der AppBar
    private lateinit var appBarConfiguration: AppBarConfiguration
    // Binding für die Hauptaktivität
    private lateinit var binding: ActivityMainBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere UserPreferences
        userPreferences = UserPreferences(this)

        // Prüfe, ob UserToken vorhanden ist
        lifecycleScope.launch {
            userPreferences.userToken.collect { userToken ->
                if (userToken != null) {
                    // Benutzer ist eingeloggt, fahre mit MainActivity fort
                    setupMainActivity(userToken) // Übergebe usertoken für die Sidebar
                } else {
                    // Kein Benutzer eingeloggt, navigiere zur LoginActivity
                    navigateToLoginActivity()
                }
            }
        }
    }

    private fun setupMainActivity(userToken: UserToken) {
        // Aktivieren des randlosen Designs
        enableEdgeToEdge()

        // Initialisieren des Bindings mit dem Hauptlayout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setzen der Toolbar als Support-ActionBar
        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialisieren der Drawer-Layout- und Navigations-View-Komponenten
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Festlegen der Top-Level-Ziele für die Navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_homepage,
                R.id.nav_wochenplan,
                R.id.nav_todo,
                R.id.nav_einkaufsliste,
                R.id.nav_haushaltsbuch
            ), drawerLayout
        )

        // Einrichten der ActionBar mit dem NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Einrichten der NavigationView mit dem NavController
        navView.setupWithNavController(navController)

        // Sidebar (NavigationView) anpassen, um WgId und E-Mail anzuzeigen
        val headerView: View = navView.getHeaderView(0)
        val userNicknameTextView = headerView.findViewById<TextView>(R.id.user_nickname_text_view)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)

        // Setze die E-Mail im Sidebar-Header
        userNicknameTextView.text = userToken.nickname

        // Lade das Profilbild in die ImageView mit Glide
        Glide.with(this)
            .load(userToken.profileImageUrl)
            .placeholder(R.drawable.pp) // Fallback-Bild, wenn das Bild nicht geladen werden kann
            .into(profileImageView)

        // Setze einen Listener für die Navigation
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    // Führe den Logout durch
                    performLogout()
                    true
                }
                else -> {
                    // Navigiere zu den Fragmenten
                    navController.navigate(item.itemId)
                    drawerLayout.closeDrawers() // Schließe das DrawerLayout
                    true // Gebe true zurück, um anzuzeigen, dass das Element verarbeitet wurde
                }
            }
        }
    }


    private fun performLogout() {
        // Lösche den UserToken
        lifecycleScope.launch {
            userPreferences.clearUserToken()

            // Navigiere zur LoginActivity und beende die MainActivity
            navigateToLoginActivity()
        }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Beendet die MainActivity, damit der Benutzer nicht zurückkehren kann
    }

    // Handhaben der Navigation, wenn die Zurück-Taste in der ActionBar gedrückt wird
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
