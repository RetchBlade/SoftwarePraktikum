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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddItemBinding.inflate(inflater, container, false)
        val view = binding.root

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val unitAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.unit_array, android.R.layout.simple_spinner_item
        )
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUnit.adapter = unitAdapter

        val categoryAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.category_array, android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        binding.btnAdd.setOnClickListener {
            val name = binding.editItemName.text.toString()
            val quantity = binding.editItemQuantity.text.toString()
            val unit = binding.spinnerUnit.selectedItem.toString()
            val category = binding.spinnerCategory.selectedItem.toString()

            // Ürün resminin doğru şekilde atanması
            val imageResId = getImageResourceId(name)

            // Yeni ürün nesnesini oluşturuyoruz
            val newItem = Produkt(name, quantity, unit, category, imageResId)
            onAddItem?.invoke(newItem)

            dismiss()  // Ürün eklendiyse dialogu kapatıyoruz
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        return view
    }

    private fun getImageResourceId(productName: String): Int {
        return when (productName.toLowerCase()) {
            "apple" -> R.drawable.apple
            "banana" -> R.drawable.banana
            "beer" -> R.drawable.beer
            else -> R.drawable.ic_placeholder_image
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
