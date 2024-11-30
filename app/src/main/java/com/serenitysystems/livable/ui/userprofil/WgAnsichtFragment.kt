package com.serenitysystems.livable.ui.userprofil

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.PopupMenu
import com.serenitysystems.livable.R

class WgAnsichtFragment : Fragment() {

    private lateinit var wgAddressText: TextView
    private lateinit var wgAddressEdit: EditText
    private lateinit var roomCountText: TextView
    private lateinit var roomCountEdit: EditText
    private lateinit var wgSizeText: TextView
    private lateinit var wgSizeEdit: EditText
    private lateinit var saveButton: Button
    private lateinit var avatarGallery: LinearLayout
    private lateinit var optionsMenu: AppCompatImageView

    private var isEditMode = false
    private var isLeiter = false  // Flag to check if the user is the "Leiter"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wg_ansicht, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        wgAddressText = view.findViewById(R.id.wgAddressText)
        wgAddressEdit = view.findViewById(R.id.wgAddressEdit)
        roomCountText = view.findViewById(R.id.roomCountText)
        roomCountEdit = view.findViewById(R.id.roomCountEdit)
        wgSizeText = view.findViewById(R.id.wgSizeText)
        wgSizeEdit = view.findViewById(R.id.wgSizeEdit)
        saveButton = view.findViewById(R.id.saveButton)
        avatarGallery = view.findViewById(R.id.avatarGallery)
        optionsMenu = view.findViewById(R.id.optionsMenu)

        isLeiter = checkIfUserIsLeiter()

        if (isLeiter) {
            optionsMenu.visibility = View.VISIBLE
        } else {
            optionsMenu.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            Log.d("WgAnsichtFragment", "Save button clicked.")
            saveChanges()
        }

        optionsMenu.setOnClickListener {
            Log.d("WgAnsichtFragment", "Options menu clicked.")
            showOptionsMenu(it)
        }

        populateAvatarGallery()
    }

    private fun saveChanges() {
        // Retrieve input values
        val wgAddress = wgAddressEdit.text.toString().trim()
        val roomCount = roomCountEdit.text.toString().trim()
        val wgSize = wgSizeEdit.text.toString().trim()

        // Debugging logs for values
        Log.d("WgAnsichtFragment", "Saving changes with the following values:")
        Log.d("WgAnsichtFragment", "wgAddress: $wgAddress, roomCount: $roomCount, wgSize: $wgSize")

        // Validate inputs before saving
        if (wgAddress.isEmpty() || roomCount.isEmpty() || wgSize.isEmpty()) {
            // Handle missing inputs
            Log.e("WgAnsichtFragment", "Error: One or more fields are empty!")
            Toast.makeText(requireContext(), "Bitte füllen Sie alle Felder aus.", Toast.LENGTH_SHORT).show()
            return
        }

        // Apply changes to the TextViews
        wgAddressText.text = wgAddress
        roomCountText.text = roomCount
        wgSizeText.text = wgSize

        // Save changes persistently (e.g., to database or shared preferences)
        try {
            saveDataToStorage(wgAddress, roomCount, wgSize)
            Log.d("WgAnsichtFragment", "Data saved successfully.")
        } catch (e: Exception) {
            Log.e("WgAnsichtFragment", "Error saving data: ${e.message}")
            Toast.makeText(requireContext(), "Fehler beim Speichern der Daten.", Toast.LENGTH_SHORT).show()
            return
        }

        // Exit edit mode
        toggleEditMode(false)

        // Show confirmation
        Toast.makeText(requireContext(), "Änderungen gespeichert.", Toast.LENGTH_SHORT).show()
    }

    private fun saveDataToStorage(
        wgAddress: String,
        roomCount: String,
        wgSize: String,
    ) {
        // Simulate saving data (e.g., to a database or shared preferences)
        // For demonstration, we're logging the data
        Log.d("WgAnsichtFragment", "Saved Data: wgAddress=$wgAddress, roomCount=$roomCount, wgSize=$wgSize")
        // Simulate potential failure
        if (roomCount.toIntOrNull() == null) {
            throw IllegalArgumentException("Room count must be a valid number.")
        }
    }

    private fun checkIfUserIsLeiter(): Boolean {
        // Here, we assume a check for the user role, e.g., from a user ID or another source
        return true  // For testing, we set the user as "Leiter"
    }

    private fun showOptionsMenu(anchor: View) {
        val popupMenu = PopupMenu(requireContext(), anchor)
        popupMenu.menuInflater.inflate(R.menu.options_menu, popupMenu.menu)

        // Menu actions
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.editOption -> {
                    Log.d("WgAnsichtFragment", "Edit option selected.")
                    toggleEditMode(true) // Enable edit mode
                    true
                }
                R.id.deleteOption -> {
                    Log.d("WgAnsichtFragment", "Delete option selected.")
                    deleteWG()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditMode = enable

        // Debugging log for edit mode toggle
        Log.d("WgAnsichtFragment", "Edit mode: $enable")

        // Control visibility of TextViews and EditTexts
        wgAddressText.visibility = if (enable) View.GONE else View.VISIBLE
        wgAddressEdit.visibility = if (enable) View.VISIBLE else View.GONE
        roomCountText.visibility = if (enable) View.GONE else View.VISIBLE
        roomCountEdit.visibility = if (enable) View.VISIBLE else View.GONE
        wgSizeText.visibility = if (enable) View.GONE else View.VISIBLE
        wgSizeEdit.visibility = if (enable) View.VISIBLE else View.GONE

        // Save button visibility
        saveButton.visibility = if (enable) View.VISIBLE else View.GONE
    }

    private fun deleteWG() {
        // Deletion logic here
        Log.d("WgAnsichtFragment", "Deleting WG.")
        Toast.makeText(requireContext(), "WG wurde gelöscht.", Toast.LENGTH_SHORT).show()
    }

    private fun populateAvatarGallery() {
        val roommates = listOf(
            Pair("Haneen", R.drawable.pp_placeholder),
            Pair("Safak", R.drawable.pp_placeholder),
            Pair("Lorenz", R.drawable.pp_placeholder),
            Pair("Eray", R.drawable.pp_placeholder)
        )

        avatarGallery.removeAllViews()

        for ((name, avatarRes) in roommates) {
            val avatarContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 15
                }
            }

            val avatarCard = CardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(60.dpToPx(), 60.dpToPx())
                radius = 30.dpToPx().toFloat()
                elevation = 4.dpToPx().toFloat()
            }

            val avatarImage = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(avatarRes)
            }

            avatarCard.addView(avatarImage)

            val avatarName = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 7.dpToPx()
                }
                text = name
                textSize = 12f
                setTextColor(resources.getColor(R.color.own_text_Farbe, null))
                gravity = android.view.Gravity.CENTER
            }

            avatarContainer.addView(avatarCard)
            avatarContainer.addView(avatarName)

            avatarGallery.addView(avatarContainer)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }
}
