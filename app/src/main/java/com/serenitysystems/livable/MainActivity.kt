package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import android.view.View
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
                    setupMainActivity(userToken.email,userToken.wgId) // Übergebe WgId für die Sidebar
                } else {
                    // Kein Benutzer eingeloggt, navigiere zur LoginActivity
                    navigateToLoginActivity()
                }
            }
        }
    }

    private fun setupMainActivity(email: String? ,wgId: String?) {
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
                R.id.nav_wochenplan, R.id.nav_todo, R.id.nav_einkaufsliste, R.id.nav_haushaltsbuch
            ), drawerLayout
        )
        // Einrichten der ActionBar mit dem NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Einrichten der NavigationView mit dem NavController
        navView.setupWithNavController(navController)

        // Sidebar (NavigationView) anpassen, um WgId und E-Mail anzuzeigen
        val headerView: View = navView.getHeaderView(0) // Header-Layout des NavigationView abrufen
        val wgIdTextView: TextView = headerView.findViewById(R.id.textViewWgId) // Füge ein TextView für WgId hinzu
        val userEmailTextView = headerView.findViewById<TextView>(R.id.user_email_text_view)

        // Setze die E-Mail und WgId im Sidebar-Header
        wgIdTextView.text = "WG ID: $wgId"
        userEmailTextView.text = "User: $email"
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
