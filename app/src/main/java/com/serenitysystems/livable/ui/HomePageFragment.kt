package com.serenitysystems.livable.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.serenitysystems.livable.R
import com.serenitysystems.livable.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomePageFragment : Fragment() {

    private var dialog: AlertDialog? = null
    private lateinit var userPreferences: UserPreferences
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var userNicknameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate das Layout für dieses Fragment
        val view = inflater.inflate(R.layout.fragment_home_page, container, false)

        // Initialize UserPreferences
        userPreferences = UserPreferences(requireContext())

        // Get references to the TextViews
        welcomeMessageTextView = view.findViewById(R.id.greetingText)
        userNicknameTextView = view.findViewById(R.id.userNickname)

        // Fetch user nickname from preferences
        fetchUserNickname()

        // WG-Verwaltung Button (Kachel) Referenz
        val wgVerwaltungButton: FrameLayout = view.findViewById(R.id.wgVerwaltungButton)

        // Set Click Listener on WG-Verwaltung Button
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it)
        }

        // To-Do List Button Referenz
        val toDoButton: FrameLayout = view.findViewById(R.id.toDoButton)

        // Set Click Listener on To-Do Button (kann noch angepasst werden)
        toDoButton.setOnClickListener {
            // TODO: Handle To-Do list actions
        }

        return view
    }

    private fun fetchUserNickname() {
        CoroutineScope(Dispatchers.Main).launch {
            userPreferences.userToken.collect { userToken ->
                userNicknameTextView.text = userToken?.nickname ?: ""
            }
        }
    }

    private fun showWGOptionsDialog(anchorView: View) {
        val dialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wg_options, null)

        dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.createWGButton).setOnClickListener {
            showWGRegister()
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.joinWGButton).setOnClickListener {
            showJoinWGDialog()
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.leaveWGButton).setOnClickListener {
            leaveWG()
            dialog?.dismiss()
        }

        dialog?.show()
        positionDialogUnderView(anchorView)
    }

    private fun positionDialogUnderView(anchorView: View) {
        dialog?.window?.let { window ->
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            window.attributes.y = location[1] + anchorView.height + 20
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun showWGRegister() {
        // Verwende den NavController, um zum Wg_registrierungFragment zu navigieren
        findNavController().navigate(R.id.nav_wg_registrierung)
    }

    private fun showJoinWGDialog() {
        val dialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_wg, null)

        val joinWGDialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val wgIdInput: EditText = dialogView.findViewById(R.id.wgIdInput)
        val submitButton: Button = dialogView.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val wgId = wgIdInput.text.toString().trim()
            if (wgId.isNotEmpty()) {
                joinWG(wgId)
                joinWGDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Bitte geben Sie eine WG-ID ein.", Toast.LENGTH_SHORT).show()
            }
        }

        joinWGDialog.show()
    }

    private fun createWG() {
        // Logic to create WG
    }

    private fun joinWG(wgId: String) {
        Toast.makeText(requireContext(), "Sie sind der WG mit der ID: $wgId beigetreten.", Toast.LENGTH_SHORT).show()
    }

    private fun leaveWG() {
        // Logic to leave WG
    }
}
