package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.serenitysystems.livable.databinding.FragmentEinnahmenBinding
import java.text.SimpleDateFormat
import java.util.*

class EinnahmenFragment : Fragment() {

    private var _binding: FragmentEinnahmenBinding? = null
    private val binding get() = _binding!!
    lateinit var haushaltsbuchViewModel: HaushaltsbuchViewModel

    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        haushaltsbuchViewModel =
            ViewModelProvider(requireActivity()).get(HaushaltsbuchViewModel::class.java)

        _binding = FragmentEinnahmenBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = ExpenseAdapter(emptyList()) { expense ->
            showEditTransactionDialog(expense)
        }
        binding.recyclerView.adapter = adapter

        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner, Observer { expenses ->
            val einnahmen = expenses.filter { it.istEinnahme }
            adapter.updateExpenses(einnahmen)
            updateKontostand()
        })

        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner, Observer { kontostand ->
            binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
        })

        binding.fab.setOnClickListener {
            showAddTransactionDialog(true)
        }

        updateDateDisplay()

        binding.leftArrow.setOnClickListener {
            changeDate(-1)
        }

        binding.rightArrow.setOnClickListener {
            changeDate(1)
        }

        binding.dateTextView.setOnClickListener {
            showDatePickerDialog()
        }

        // Dialog'dan sonuçları almak için listener ekleyin
        setFragmentResultListener(AddTransactionDialogFragment.REQUEST_KEY) { _, bundle ->
            val expense = bundle.getParcelable<Expense>("expense")
            expense?.let { haushaltsbuchViewModel.addExpense(it) }
        }

        return root
    }

    private fun updateKontostand() {
        val kontostand = haushaltsbuchViewModel.kontostand.value ?: 0f
        binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
    }

    private fun showAddTransactionDialog(isEinnahme: Boolean) {
        val dialog = AddTransactionDialogFragment.newInstance(isEinnahme)
        dialog.show(parentFragmentManager, "AddTransactionDialogFragment")
    }

    private fun showEditTransactionDialog(expense: Expense) {
        val dialog = AddTransactionDialogFragment.newInstance(expense)
        dialog.show(parentFragmentManager, "EditTransactionDialogFragment")
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(selectedDate.time)
    }

    private fun changeDate(days: Int) {
        selectedDate.add(Calendar.DAY_OF_MONTH, days)
        haushaltsbuchViewModel.clearExpensesForDate(selectedDate)
        updateDateDisplay()
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, monthOfYear)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                haushaltsbuchViewModel.clearExpensesForDate(selectedDate)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
