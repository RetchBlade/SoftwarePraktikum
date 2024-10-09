package com.serenitysystems.livable.ui.einkaufsliste

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddItemBinding
import java.text.SimpleDateFormat
import java.util.*

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    var onAddItem: ((Produkt) -> Boolean)? = null
    private var selectedDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddItemBinding.inflate(inflater, container, false)
        val view = binding.root

        // Metallischer Hintergrund für den Dialog
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

        // Hinzufügen-Button
        binding.btnAdd.setOnClickListener {
            val name = binding.editItemName.text.toString()
            val quantity = binding.editItemQuantity.text.toString()
            val unit = binding.spinnerUnit.selectedItem.toString()
            val category = binding.spinnerCategory.selectedItem.toString()
            val date = selectedDate?.let { dateFormat.format(it.time) }

            val imageResId = getImageResourceId(name)

            val newItem = Produkt(name, quantity, unit, category, imageResId, date)
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

    private fun getImageResourceId(productName: String): Int {
        return when (productName.toLowerCase(Locale.ROOT)) {
            "apfel" -> R.drawable.apple
            "banane" -> R.drawable.banana
            "bier" -> R.drawable.beer
            else -> R.drawable.ic_placeholder_image
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
