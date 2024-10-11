package com.serenitysystems.livable.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.PopupWindowCompat
import com.serenitysystems.livable.R
import android.widget.PopupWindow

class HomePageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home_page)

        // WG-Verwaltung Button (Kachel) Referenz
        val wgVerwaltungButton: FrameLayout = findViewById(R.id.wgVerwaltungButton)

        // Setze einen Click-Listener auf den WG-Verwaltung Button
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it) // Methode zum Anzeigen des benutzerdefinierten Dialogs aufrufen
        }
    }

    private fun showWGOptionsDialog(anchorView: View) {
        // Inflate das Dialog-Layout
        val dialogView: View = LayoutInflater.from(this).inflate(R.layout.dialog_wg_options, null)

        // Erstelle ein PopupWindow, um den Dialog direkt unter dem Button erscheinen zu lassen
        val popupWindow = PopupWindow(
            dialogView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Setze den abgerundeten Hintergrund mit Schatten
        popupWindow.setBackgroundDrawable(getDrawable(R.drawable.dialog_background))

        // Berechne die Position des Buttons und setze den Dialog darunter
        PopupWindowCompat.showAsDropDown(popupWindow, anchorView, 0, 20, Gravity.START)

        // Setze die Click-Listener für die Buttons innerhalb des Dialogs
        dialogView.findViewById<Button>(R.id.createWGButton).setOnClickListener {
            createWG()
            popupWindow.dismiss() // Schließe den Popup-Dialog nach Auswahl
        }

        dialogView.findViewById<Button>(R.id.joinWGButton).setOnClickListener {
            joinWG()
            popupWindow.dismiss()
        }

        dialogView.findViewById<Button>(R.id.leaveWGButton).setOnClickListener {
            leaveWG()
            popupWindow.dismiss()
        }
    }

    private fun createWG() {
        // Logik zum Anlegen einer WG
    }

    private fun joinWG() {
        // Logik zum Beitreten einer WG
    }

    private fun leaveWG() {
        // Logik zum Verlassen einer WG
    }
}
