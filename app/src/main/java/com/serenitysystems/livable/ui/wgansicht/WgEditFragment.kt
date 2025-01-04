package com.serenitysystems.livable.ui.wgansicht

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WgEditFragment : Fragment() {

    private lateinit var wgAddressEdit: EditText
    private lateinit var roomCountEdit: EditText
    private lateinit var wgSizeEdit: EditText
    private lateinit var saveButton: Button

    private val sharedViewModel: WgSharedViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()

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
        sharedViewModel.wgAddress.observe(viewLifecycleOwner) {
            wgAddressEdit.setText(it)
        }
        sharedViewModel.roomCount.observe(viewLifecycleOwner) {
            roomCountEdit.setText(it)
        }
        sharedViewModel.wgSize.observe(viewLifecycleOwner) { size ->
            wgSizeEdit.setText("$size")
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

        lifecycleScope.launch {
            try {
                saveToFirestore(newAddress, newRoomCount, newWgSize)
                sharedViewModel.setWgDetails(newAddress, newRoomCount, newWgSize)
                Toast.makeText(requireContext(), "Änderungen gespeichert!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fehler beim Speichern der Änderungen!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveToFirestore(address: String, rooms: String, size: String) {
        val wgId = sharedViewModel.wgId.value ?: throw Exception("WG ID nicht verfügbar!")
        val wgData = mapOf(
            "adresse" to address,
            "zimmerAnzahl" to rooms,
            "groesse" to size
        )

        withContext(Dispatchers.IO) {
            firestore.collection("WGs")
                .document(wgId)
                .update(wgData)
                .await()
        }
    }
}
