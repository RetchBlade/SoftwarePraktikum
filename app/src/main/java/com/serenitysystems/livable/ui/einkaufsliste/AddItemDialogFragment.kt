// AddItemDialogFragment.kt
package com.serenitysystems.livable.ui

import android.app.DatePickerDialog
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddItemBinding
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    private var currentItem: Produkt? = null
    var onAddItem: ((Produkt, Produkt?) -> Boolean)? = null

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var selectedDate: Calendar? = null

    private var selectedImageUri: Uri? = null

    // Maximale Bildgröße (5 MB)
    private val MAX_IMAGE_SIZE = 5 * 1024 * 1024

    // ImagePicker Launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val fileSize = getFileSize(it)
                if (fileSize <= MAX_IMAGE_SIZE) {
                    withContext(Dispatchers.Main) {
                        selectedImageUri = it
                        binding.productImage.setImageURI(it)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Das Bild ist zu groß. Bitte wählen Sie ein Bild, das kleiner als 5 MB ist.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddItemBinding.inflate(inflater, container, false)
        val view = binding.root

        // Transparenter Hintergrund für den Dialog
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Einheit-Spinner einrichten
        val unitAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.unit_array, android.R.layout.simple_spinner_item
        )
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUnit.adapter = unitAdapter

        // Kategorie-Spinner einrichten
        val categoryAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.einkaufsliste_category_array, android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        // Wenn ein bestehendes Produkt bearbeitet wird, Felder füllen
        currentItem?.let {
            populateFields(it)
        }

        // Datumsauswahl einrichten
        binding.etSelectedDate.setOnClickListener {
            showDatePicker()
        }

        // Klick-Listener für das Produktbild hinzufügen
        binding.productImage.setOnClickListener {
            selectImage() // Bildauswahl öffnen
        }

        // Hinzufügen-Button: Neuen Artikel hinzufügen oder bestehendes Produkt aktualisieren
        binding.btnAdd.setOnClickListener {
            addItem()
        }

        // Abbrechen-Button: Dialog schließen
        binding.btnCancel.setOnClickListener { dismiss() }

        return view
    }

    // Funktion zur Anzeige des DatePickers
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val today = Calendar.getInstance()
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                // Überprüfen, ob das ausgewählte Datum in der Vergangenheit liegt
                if (selectedCalendar.before(today)) {
                    Toast.makeText(
                        requireContext(),
                        "Sie können kein Datum in der Vergangenheit wählen.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    selectedDate = selectedCalendar
                    binding.etSelectedDate.setText(dateFormat.format(selectedCalendar.time))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = Calendar.getInstance().timeInMillis // Verhindert die Auswahl vergangener Daten
        datePicker.show()
    }

    // Funktion zum Hinzufügen oder Aktualisieren eines Produkts
    private fun addItem() {
        val name = binding.editItemName.text.toString().trim()
        val quantity = binding.editItemQuantity.text.toString().trim()
        val unit = binding.spinnerUnit.selectedItem?.toString() ?: ""
        val category = binding.spinnerCategory.selectedItem?.toString() ?: ""
        val date = selectedDate?.let { dateFormat.format(it.time) }

        // Eingabevalidierung
        if (name.isEmpty()) {
            binding.editItemName.error = "Bitte geben Sie einen Produktnamen ein."
            return
        }

        if (quantity.isEmpty()) {
            binding.editItemQuantity.error = "Bitte geben Sie eine Menge ein."
            return
        }

        if (date == null) {
            Toast.makeText(requireContext(), "Bitte wählen Sie ein Datum aus.", Toast.LENGTH_SHORT).show()
            return
        }

        // URI des ausgewählten Bildes als String speichern
        val imageUriString = selectedImageUri?.toString() ?: currentItem?.imageUri

        // Hier die ID vom aktuellen Item übernehmen oder neue ID generieren
        val newItemId = currentItem?.id ?: UUID.randomUUID().toString()

        val newItem = Produkt(
            id = newItemId, // Setzen der ID
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            imageUri = imageUriString,
            date = date,
            isChecked = currentItem?.isChecked ?: false, // Status vom bestehenden Produkt beibehalten
            statusIcon = currentItem?.statusIcon // Statusicon beibehalten
        )
        onAddItem?.invoke(newItem, currentItem) // Callback für das Hinzufügen oder Aktualisieren aufrufen

        dismiss() // Dialog schließen
    }

    // Funktion zum Befüllen der Felder mit den vorhandenen Produktdaten
    private fun populateFields(item: Produkt) {
        binding.apply {
            editItemName.setText(item.name)
            editItemQuantity.setText(item.quantity)
            spinnerUnit.setSelection((spinnerUnit.adapter as? ArrayAdapter<String>)?.getPosition(item.unit) ?: 0)
            spinnerCategory.setSelection((spinnerCategory.adapter as? ArrayAdapter<String>)?.getPosition(item.category) ?: 0)
            etSelectedDate.setText(item.date)
            selectedDate = item.date?.let {
                val calendar = Calendar.getInstance()
                calendar.time = dateFormat.parse(it)!!
                calendar
            }
            item.imageUri?.let {
                selectedImageUri = Uri.parse(it)
                productImage.setImageURI(selectedImageUri)
            }
        }
    }

    // Setzt das aktuelle Produkt zum Bearbeiten
    fun setCurrentItem(item: Produkt) {
        currentItem = item
    }

    override fun onResume() {
        super.onResume()
        // Setzt die Breite und Höhe des Dialogs
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    // Funktion zum Auswählen eines Bildes aus der Galerie
    private fun selectImage() {
        imagePickerLauncher.launch("image/*") // Bildauswahl starten
    }

    // Funktion zur Ermittlung der Dateigröße eines ausgewählten Bildes
    private suspend fun getFileSize(uri: Uri): Long {
        var fileSize: Long = 0
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = it.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
