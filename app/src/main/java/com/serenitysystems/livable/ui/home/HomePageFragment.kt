package com.serenitysystems.livable.ui.home

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.serenitysystems.livable.R

class HomePageFragment : Fragment() {

    private val homePageViewModel: HomePageViewModel by viewModels()
    private var dialog: AlertDialog? = null
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var userNicknameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_page, container, false)

        // Get references to the TextViews
        welcomeMessageTextView = view.findViewById(R.id.greetingText)
        userNicknameTextView = view.findViewById(R.id.userNickname)

        // Observe the user nickname from the ViewModel
        homePageViewModel.userNickname.observe(viewLifecycleOwner, Observer { nickname ->
            userNicknameTextView.text = nickname ?: ""
        })

        // WG-Verwaltung Button (Kachel) Referenz
        val wgVerwaltungButton: FrameLayout = view.findViewById(R.id.wgVerwaltungButton)
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it)
        }

        // To-Do List Button Referenz
        val toDoButton: FrameLayout = view.findViewById(R.id.toDoButton)
        toDoButton.setOnClickListener {
            // TODO: Handle To-Do list actions
        }

        return view
    }

    private fun showWGOptionsDialog(anchorView: View) {
        val dialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.home_dialog_wg_options, null)

        dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.createWGButton).setOnClickListener {
            findNavController().navigate(R.id.nav_wg_registrierung)
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.joinWGButton).setOnClickListener {
            showJoinWGDialog()
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.leaveWGButton).setOnClickListener {
            homePageViewModel.leaveWG()
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

    private fun showJoinWGDialog() {
        val dialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.home_dialog_join_wg, null)

        val joinWGDialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val wgIdInput: EditText = dialogView.findViewById(R.id.wgIdInput)
        val submitButton: Button = dialogView.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val wgId = wgIdInput.text.toString().trim()
            if (wgId.isNotEmpty()) {
                homePageViewModel.joinWG(wgId)
                joinWGDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Bitte geben Sie eine WG-ID ein.", Toast.LENGTH_SHORT).show()
            }
        }

        joinWGDialog.show()
    }
}
