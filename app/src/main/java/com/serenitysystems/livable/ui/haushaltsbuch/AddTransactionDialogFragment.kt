package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddTransactionBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    companion object {
        private const val ARG_IS_EINNAHME = "is_einnahme"
        private const val ARG_EXPENSE = "expense"

        fun newInstance(isEinnahme: Boolean): AddTransactionDialogFragment {
            val args = Bundle().apply {
                putBoolean(ARG_IS_EINNAHME, isEinnahme)
            }
            return AddTransactionDialogFragment().apply {
                arguments = args
            }
        }

        fun newInstance(expense: Expense): AddTransactionDialogFragment {
            val args = Bundle().apply {
                putParcelable(ARG_EXPENSE, expense)
            }
            return AddTransactionDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEinnahme = arguments?.getBoolean(ARG_IS_EINNAHME)
        val expense = arguments?.getParcelable<Expense>(ARG_EXPENSE)

        if (expense != null) {
            // Setze Titel und Hintergrundfarbe basierend auf `istEinnahme` aus der Transaktion
            if (expense.istEinnahme) {
                binding.title.text = getString(R.string.einnahme_button_text)
                binding.title.setBackgroundResource(R.drawable.green_filled_background)
            } else {
                binding.title.text = getString(R.string.ausgabe_button_text)
                binding.title.setBackgroundResource(R.drawable.red_filled_background)
            }
        } else {
            // Standard: Basierend auf ARG_IS_EINNAHME
            if (isEinnahme == true) {
                binding.title.text = getString(R.string.einnahme_button_text)
                binding.title.setBackgroundResource(R.drawable.green_filled_background)
            } else {
                binding.title.text = getString(R.string.ausgabe_button_text)
                binding.title.setBackgroundResource(R.drawable.red_filled_background)
            }
        }

        // Setze Kategorien, Werte und Datum basierend auf der bestehenden Transaktion (falls vorhanden)
        val categories = haushaltsbuchViewModel.categories
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        expense?.let {
            binding.editAmount.setText(it.betrag.toString())
            binding.editNote.setText(it.notiz)
            binding.editDate.setText(it.datum)
            val categoryIndex = categories.indexOf(it.kategorie)
            if (categoryIndex >= 0) {
                binding.spinnerCategory.setSelection(categoryIndex)
            }
        }

        binding.editDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.saveButton.setOnClickListener {
            saveTransaction(expense?.istEinnahme ?: isEinnahme ?: false)
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                binding.editDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun saveTransaction(isEinnahme: Boolean) {
        val amount = binding.editAmount.text.toString().toFloatOrNull()
        val note = binding.editNote.text.toString()
        val date = binding.editDate.text.toString()
        val category = binding.spinnerCategory.selectedItem?.toString()

        if (amount == null || date.isEmpty() || category.isNullOrEmpty()) {
            Snackbar.make(binding.root, "Bitte alle Felder ausfüllen", Snackbar.LENGTH_LONG).show()
            return
        }

        val expense = arguments?.getParcelable<Expense>(ARG_EXPENSE)
        val newExpense = expense?.copy(
            kategorie = category,
            betrag = amount,
            notiz = note,
            datum = date,
            istEinnahme = isEinnahme // Beibehalten von istEinnahme
        ) ?: Expense(
            kategorie = category,
            betrag = amount,
            notiz = note,
            datum = date,
            istEinnahme = isEinnahme
        )

        if (expense != null && expense.id.isNotEmpty()) {
            haushaltsbuchViewModel.updateExpenseInFirestore(newExpense)
        } else {
            haushaltsbuchViewModel.addExpenseToFirestore(newExpense)
        }
        dismiss()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
