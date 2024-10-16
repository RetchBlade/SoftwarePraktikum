package com.serenitysystems.livable.ui.einkaufsliste

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddItemBinding
import java.text.SimpleDateFormat
import java.util.*
import android.database.Cursor
import android.provider.OpenableColumns

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    var onAddItem: ((Produkt) -> Boolean)? = null
    private var selectedDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private var selectedImageUri: Uri? = null // Ausgewähltes Produktbild

    // Maximale Bildgröße in Bytes (hier 2 MB)
    private val MAX_IMAGE_SIZE = 2 * 1024 * 1024 // 2 MB

    // ActivityResultLauncher für die Bildauswahl
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Überprüfe die Dateigröße des ausgewählten Bildes
            val fileSize = getFileSize(uri)
            if (fileSize <= MAX_IMAGE_SIZE) {
                // Wenn die Dateigröße innerhalb des Limits liegt, Bild setzen
                selectedImageUri = uri
                binding.productImage.setImageURI(uri)
            } else {
                // Wenn die Datei zu groß ist, Fehlermeldung anzeigen
                Toast.makeText(
                    requireContext(),
                    "Das Bild ist zu groß. Bitte wählen Sie ein Bild, das kleiner als 2 MB ist.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddItemBinding.inflate(inflater, container, false)
        val view = binding.root

        // Hintergrund für den Dialog setzen
        dialog?.window?.setBackgroundDrawableResource(R.drawable.metallischer_hintergrund)

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

        // Datumsauswahl einrichten
        binding.etSelectedDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = calendar
                binding.etSelectedDate.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            datePicker.show()
        }

        // Klick-Listener für das Produktbild hinzufügen
        binding.productImage.setOnClickListener {
            selectImage()
        }

        // Hinzufügen-Button
        binding.btnAdd.setOnClickListener {
            val name = binding.editItemName.text.toString()
            val quantity = binding.editItemQuantity.text.toString()
            val unit = binding.spinnerUnit.selectedItem.toString()
            val category = binding.spinnerCategory.selectedItem.toString()
            val date = selectedDate?.let { dateFormat.format(it.time) }

            val imageUriString = selectedImageUri?.toString()

            val newItem = Produkt(
                name = name,
                quantity = quantity,
                unit = unit,
                category = category,
                imageUri = imageUriString,
                date = date
            )
            onAddItem?.invoke(newItem)

            dismiss()
        }

        // Abbrechen-Button
        binding.btnCancel.setOnClickListener { dismiss() }

        return view
    }

    override fun onResume() {
        super.onResume()
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    // Funktion zum Auswählen eines Bildes aus der Galerie
    private fun selectImage() {
        imagePickerLauncher.launch("image/*")
    }

    // Funktion zum Ermitteln der Dateigröße eines ausgewählten Bildes
    private fun getFileSize(uri: Uri): Long {
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
