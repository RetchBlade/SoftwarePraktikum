package com.serenitysystems.livable.ui.einkaufsliste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddItemBinding

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    var onAddItem: ((Produkt) -> Boolean)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddItemBinding.inflate(inflater, container, false)
        val view = binding.root

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val unitAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.unit_array,
            android.R.layout.simple_spinner_item
        )
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUnit.adapter = unitAdapter

        val categoryAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.category_array,
            android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        binding.btnAdd.setOnClickListener {
            val name = binding.editItemName.text.toString()
            val quantity = binding.editItemQuantity.text.toString()
            val unit = binding.spinnerUnit.selectedItem.toString()
            val category = binding.spinnerCategory.selectedItem.toString()
            val imageResId = when (name.toLowerCase()) {
                "apple" -> R.drawable.apple
                "banana" -> R.drawable.banana
                "beer" -> R.drawable.beer
                else -> R.drawable.ic_placeholder_image
            }

            val newItem = Produkt(name, quantity, unit, category, imageResId)
            val itemAdded = onAddItem?.invoke(newItem) ?: false

            if (!itemAdded) {
                // Produkt bereits vorhanden
                android.widget.Toast.makeText(requireContext(), "Dieses Produkt ist bereits vorhanden", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
