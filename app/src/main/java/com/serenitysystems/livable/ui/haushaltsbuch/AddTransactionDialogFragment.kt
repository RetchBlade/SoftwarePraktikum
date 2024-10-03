package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddTransactionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var haushaltsbuchViewModel: HaushaltsbuchViewModel

    companion object {
        const val REQUEST_KEY = "AddTransactionRequest"
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        haushaltsbuchViewModel = ViewModelProvider(requireActivity()).get(HaushaltsbuchViewModel::class.java)
        setStyle(STYLE_NORMAL, R.style.CustomDialogTheme)

        // Dialog boyutlarını genişletin
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Spinner'ı ve kategori seçimini genişletin
        val layoutParams = binding.spinnerCategory.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.spinnerCategory.layoutParams = layoutParams


        val isEinnahme = arguments?.getBoolean(ARG_IS_EINNAHME) ?: false
        val expense = arguments?.getParcelable<Expense>(ARG_EXPENSE)

        binding.spinnerCategory.visibility = View.VISIBLE
        if (isEinnahme) {
            binding.title.text = getString(R.string.einnahme_button_text)
            binding.title.setBackgroundResource(R.drawable.green_filled_background)
        } else {
            binding.title.text = getString(R.string.ausgabe_button_text)
            binding.title.setBackgroundResource(R.drawable.red_filled_background)
        }

        val categories = haushaltsbuchViewModel.getCategories()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        expense?.let {
            binding.editAmount.setText(it.betrag.toString())
            binding.editNote.setText(it.notiz)
            binding.editDate.setText(it.datum)
            val categoryIndex = haushaltsbuchViewModel.getCategories().indexOf(it.kategorie)
            binding.spinnerCategory.setSelection(categoryIndex)
        }

        binding.editDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.saveButton.setOnClickListener {
            val amount = binding.editAmount.text.toString().toFloatOrNull()
            val note = binding.editNote.text.toString()
            val date = binding.editDate.text.toString()

            if (amount == null) {
                Toast.makeText(requireContext(), "Betrag girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = binding.spinnerCategory.selectedItem.toString()

            val newExpense = Expense(
                category,
                amount,
                note,
                date,
                isEinnahme
            )

            val resultBundle = Bundle().apply {
                putParcelable("expense", newExpense)
            }
            parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
            dismiss()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
