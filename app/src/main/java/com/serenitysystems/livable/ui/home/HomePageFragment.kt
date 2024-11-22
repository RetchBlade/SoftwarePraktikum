package com.serenitysystems.livable.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.serenitysystems.livable.R

class HomePageFragment : Fragment() {

    private val homePageViewModel: HomePageViewModel by viewModels()
    private var dialog: AlertDialog? = null
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var userNicknameTextView: TextView
    private lateinit var userPic: ImageView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_page, container, false)


        // Referenzen zu den TextViews abrufen
        welcomeMessageTextView = view.findViewById(R.id.greetingText)
        userNicknameTextView = view.findViewById(R.id.userNickname)
        userPic = view.findViewById(R.id.imageView)

        // Den Benutzernamen vom ViewModel beobachten
        homePageViewModel.userNickname.observe(viewLifecycleOwner, Observer { nickname ->
            userNicknameTextView.text = nickname ?: ""
        })

        // Das Benutzerbild vom ViewModel beobachten
        homePageViewModel.userPic.observe(viewLifecycleOwner, Observer { profileImageUrl ->
            if (profileImageUrl != null) {
                // Bild mit Glide laden
                Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.pp) // Platzhalterbild während des Ladens
                    .error(R.drawable.pp) // Fehlerbild, falls das Laden fehlschlägt
                    .into(userPic)
            } else {
                userPic.setImageResource(R.drawable.pp) // Platzhalter setzen, falls kein Bild vorhanden ist
            }
        })

        // WG-Verwaltung Button (Kachel) Referenz
        val wgVerwaltungButton: FrameLayout = view.findViewById(R.id.wgVerwaltungButton)
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it)
        }

        // To-Do List Button Referenz
        val toDoButton: FrameLayout = view.findViewById(R.id.toDoButton)
        toDoButton.setOnClickListener {
            findNavController().navigate(R.id.nav_todo)
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


        val createWGButton: Button = dialogView.findViewById(R.id.createWGButton)
        val joinWGButton: Button = dialogView.findViewById(R.id.joinWGButton)
        val leaveWGButton: Button = dialogView.findViewById(R.id.leaveWGButton)
        val deleteWGButton: Button = dialogView.findViewById(R.id.deleteWGButton)
        val showWGInfoButton: Button = dialogView.findViewById(R.id.showWGInfo)

        homePageViewModel.fetchUserWGInfo({ wgId, wgRole ->
            if (wgId.isNullOrEmpty()) {
                // Benutzer hat keine WG-ID
                deleteWGButton.visibility = View.GONE
                leaveWGButton.visibility = View.GONE
                showWGInfoButton.visibility = View.GONE
            } else {
                // Benutzer hat eine WG-ID
                createWGButton.visibility = View.GONE // Nicht anzeigen, wenn in einer WG
                joinWGButton.visibility = View.GONE // Nicht anzeigen, wenn in einer WG

                if (wgRole == "Wg-Leiter") {
                    deleteWGButton.visibility = View.VISIBLE // Zeige löschen, wenn Wg-Leiter
                } else {
                    deleteWGButton.visibility = View.GONE // Nicht anzeigen, wenn kein Wg-Leiter
                }

                leaveWGButton.visibility = View.VISIBLE // Zeige verlassen an
                showWGInfoButton.visibility = View.VISIBLE // Zeige Info an
            }
        }, { errorMessage ->
            showErrorDialog(errorMessage) // Fehlerbehandlung
        })

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
                homePageViewModel.joinWG(wgId) { errorMessage ->
                    showErrorDialog(errorMessage)
                }
                joinWGDialog.dismiss() // Nur hier schließen, wenn WG-ID gültig
            } else {
                showErrorDialog("Bitte geben Sie eine WG-ID ein.")
            }
        }

        joinWGDialog.show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Fehler")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
