// ItemOptionsDialogFragment.kt
package com.serenitysystems.livable.ui.einkaufsliste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.serenitysystems.livable.databinding.DialogItemOptionsBinding
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt

class ItemOptionsDialogFragment(
    private val produkt: Produkt,
    private val onEditItem: (Produkt) -> Unit,
    private val onMarkAsBought: (Produkt) -> Unit,
    private val onMoveToAnotherDay: (Produkt) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogItemOptionsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this dialog
        binding = DialogItemOptionsBinding.inflate(inflater, container, false)

        // Mark as bought (first button)
        binding.btnMarkAsBought.setOnClickListener {
            onMarkAsBought(produkt)
            dismiss()
        }

        // Move to another day (second button)
        binding.btnMoveToAnotherDay.setOnClickListener {
            onMoveToAnotherDay(produkt)
            dismiss()
        }

        // Edit Item (Bearbeiten) (last button)
        binding.btnEditItem.setOnClickListener {
            onEditItem(produkt) // Immediately call the edit callback
            dismiss() // Close the dialog after the action
        }

        // If the product is already marked as bought, hide the "Mark as Bought" button
        if (produkt.isChecked) {
            binding.btnMarkAsBought.visibility = View.GONE
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Adjust dialog size to match the screen width
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}
