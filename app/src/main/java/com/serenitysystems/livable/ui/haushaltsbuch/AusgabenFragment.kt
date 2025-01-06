package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.serenitysystems.livable.databinding.FragmentAusgabenBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import java.text.SimpleDateFormat
import java.util.*

class AusgabenFragment : Fragment() {

    private var _binding: FragmentAusgabenBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()
    private lateinit var adapter: ExpenseAdapter

    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAusgabenBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // RecyclerView setup
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ExpenseAdapter(
            mutableListOf(),
            onEditClick = { expense -> showEditTransactionDialog(expense) },
            onExpenseUpdated = { haushaltsbuchViewModel.updateExpenseInFirestore(it) },
            onExpenseRemoved = { haushaltsbuchViewModel.deleteExpenseFromFirestore(it) },
            onRequestDelete = { expense, _ ->
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Bestätigung")
                builder.setMessage("Möchten Sie diesen Eintrag wirklich löschen?")
                builder.setPositiveButton("Ja") { dialog, _ ->
                    adapter.confirmDelete(expense)
                    dialog.dismiss()
                }
                builder.setNegativeButton("Nein") { dialog, _ ->
                    adapter.restoreExpense(expense)
                    dialog.dismiss()
                }
                builder.show()
            }
        )

        binding.recyclerView.adapter = adapter

        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            val ausgaben = expenses.filter { !it.istEinnahme }
            adapter.updateExpenses(ausgaben)
            updateKontostand()
        }

        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner) { kontostand ->
            binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
        }

        binding.fab.setOnClickListener {
            showAddTransactionDialog(false)
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

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(requireContext(), adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

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
        binding.dateTextView.text = formatDate(selectedDate)
        haushaltsbuchViewModel.loadExpensesForDate(formatDate(selectedDate))
    }

    private fun changeDate(days: Int) {
        selectedDate.add(Calendar.DAY_OF_MONTH, days)
        updateDateDisplay()
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, monthOfYear)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
