package com.serenitysystems.livable.ui.userprofil

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.serenitysystems.livable.R

class WgEditFragment : Fragment() {

    private lateinit var wgAddressEdit: EditText
    private lateinit var roomCountEdit: EditText
    private lateinit var wgSizeEdit: EditText
    private lateinit var saveButton: Button

    private val sharedViewModel: WgSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wg_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        populateFields()

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun initializeViews(view: View) {
        wgAddressEdit = view.findViewById(R.id.wgAddressEdit)
        roomCountEdit = view.findViewById(R.id.roomCountEdit)
        wgSizeEdit = view.findViewById(R.id.wgSizeEdit)
        saveButton = view.findViewById(R.id.saveButton)
    }

    private fun populateFields() {
        // Daten aus dem SharedViewModel einfügen
        sharedViewModel.wgAddress.observe(viewLifecycleOwner) {
            wgAddressEdit.setText(it)
        }
        sharedViewModel.roomCount.observe(viewLifecycleOwner) {
            roomCountEdit.setText(it)
        }
        sharedViewModel.wgSize.observe(viewLifecycleOwner) {
            wgSizeEdit.setText(it)
        }
    }

    private fun saveChanges() {
        val newAddress = wgAddressEdit.text.toString().trim()
        val newRoomCount = roomCountEdit.text.toString().trim()
        val newWgSize = wgSizeEdit.text.toString().trim()

        if (newAddress.isEmpty() || newRoomCount.isEmpty() || newWgSize.isEmpty()) {
            Toast.makeText(requireContext(), "Alle Felder ausfüllen!", Toast.LENGTH_SHORT).show()
            return
        }

        // Änderungen im SharedViewModel speichern
        sharedViewModel.setWgDetails(newAddress, newRoomCount, newWgSize)
        Toast.makeText(requireContext(), "Änderungen gespeichert!", Toast.LENGTH_SHORT).show()

        // Zurück zur Ansicht navigieren
        findNavController().popBackStack()
    }
}