package com.serenitysystems.livable.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.serenitysystems.livable.R

class HomePageActivity : AppCompatActivity() {

    private var dialog: AlertDialog? = null // Variable to hold the dialog reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home_page)

        // WG-Verwaltung Button (Kachel) Referenz
        val wgVerwaltungButton: FrameLayout = findViewById(R.id.wgVerwaltungButton)

        // Setze einen Click-Listener auf den WG-Verwaltung Button
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it) // Pass the view reference to position the dialog
        }
    }

    private fun showWGOptionsDialog(anchorView: View) {
        // Inflate das Dialog-Layout
        val dialogView: View = LayoutInflater.from(this).inflate(R.layout.dialog_wg_options, null)

        // Erstelle den AlertDialog mit dem benutzerdefinierten Layout
        dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true) // Allow dismissing when clicking outside
            .create()

        // Setze die Click-Listener für die Buttons innerhalb des Dialogs
        dialogView.findViewById<Button>(R.id.createWGButton).setOnClickListener {
            createWG()
            dialog?.dismiss() // Schließe den Dialog nach Auswahl
        }

        dialogView.findViewById<Button>(R.id.joinWGButton).setOnClickListener {
            joinWG()
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.leaveWGButton).setOnClickListener {
            leaveWG()
            dialog?.dismiss()
        }

        // Show dialog
        dialog?.show()

        // Positioning the dialog beneath the button
        positionDialogUnderView(anchorView)
    }

    private fun positionDialogUnderView(anchorView: View) {
        dialog?.window?.let { window ->
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location) // Get the button's location on screen

            // Set the dialog's Y position just below the button
            window.attributes.y = location[1] + anchorView.height + 20 // Adding a small margin of 20 pixels
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Set the layout
        }
    }

    private fun createWG() {
        // Logic to create WG
    }

    private fun joinWG() {
        // Logic to join WG
    }

    private fun leaveWG() {
        // Logic to leave WG
    }
}
