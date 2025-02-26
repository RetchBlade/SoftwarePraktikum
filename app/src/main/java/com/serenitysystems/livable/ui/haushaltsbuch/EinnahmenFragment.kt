package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.serenitysystems.livable.databinding.FragmentEinnahmenBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment für eine **monatliche** Ansicht von Einnahmen (istEinnahme = true).
 * - onResume: Aktualisierung bei Tab-Wechsel
 * - Datum klicken => Day-Picker nur für den gewählten Monat
 */
class EinnahmenFragment : Fragment() {

    private var _binding: FragmentEinnahmenBinding? = null
    private val binding get() = _binding!!

    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()
    private lateinit var adapter: ExpenseAdapter

    // Ausgewählter Monat/Jahr
    private var selectedMonth: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEinnahmenBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * UI-Elemente einrichten, nachdem die View erstellt wurde.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    /**
     * Wenn das Fragment wieder erscheint (Tab-Wechsel etc.),
     * lädt loadCurrentMonth erneut.
     */
    override fun onResume() {
        super.onResume()
        loadCurrentMonth()
    }

    private fun initView() {
        // RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ExpenseAdapter(
            expenses = mutableListOf(),
            onEditClick = { expense -> showEditTransactionDialog(expense) },
            onExpenseUpdated = { haushaltsbuchViewModel.updateExpenseInFirestore(it) },
            onExpenseRemoved = { haushaltsbuchViewModel.deleteExpenseFromFirestore(it) },
            onRequestDelete = { expense, _ ->
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Bestätigung")
                builder.setMessage("Möchten Sie diesen Eintrag wirklich löschen?")
                builder.setPositiveButton("Ja") { dialog, _ ->
                    haushaltsbuchViewModel.deleteExpenseFromFirestore(expense)
                    dialog.dismiss()
                }
                builder.setNegativeButton("Nein") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.show()
            },
            // Datum-Klick => nur aktueller Monat
            onDateClick = { expense -> showDayPickerForThisMonth(expense) }
        )
        binding.recyclerView.adapter = adapter

        // allExpenses => sobald neue Daten vorliegen => loadCurrentMonth
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            loadCurrentMonth()
        }

        // selectedDateExpenses => nur Einnahmen filtern
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { list ->
            val einnahmen = list.filter { it.istEinnahme }
            adapter.updateExpenses(einnahmen)
        }

        // Kontostand
        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner) { kontostand ->
            binding.textViewKontostand.text = "Kontostand: %.2f EUR".format(kontostand)
        }

        // FAB => neue Einnahme, Standard Tag=1
        binding.fab.setOnClickListener {
            val dateStr = "01.${formatMonthForFirestore(selectedMonth)}"
            showAddTransactionDialog(true, dateStr)
        }

        updateMonthHeader()

        // Pfeile => Monat +/- 1
        binding.leftArrow.setOnClickListener {
            selectedMonth.add(Calendar.MONTH, -1)
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
            updateMonthHeader()
        }
        binding.rightArrow.setOnClickListener {
            selectedMonth.add(Calendar.MONTH, 1)
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
            updateMonthHeader()
        }

        // Klick auf dateTextView => nur Monat/Jahr wählen
        binding.dateTextView.setOnClickListener {
            showMonthPickerDialog()
        }

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(requireContext(), adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun loadCurrentMonth() {
        val mmYYYY = formatMonthForFirestore(selectedMonth)
        haushaltsbuchViewModel.loadExpensesForMonth(mmYYYY)
    }

    private fun updateMonthHeader() {
        binding.dateTextView.text = formatMonthName(selectedMonth)
        loadCurrentMonth()
    }

    private fun showMonthPickerDialog() {
        val year = selectedMonth.get(Calendar.YEAR)
        val month = selectedMonth.get(Calendar.MONTH)

        val dialog = DatePickerDialog(
            requireContext(),
            { _, newYear, newMonth, _ ->
                selectedMonth.set(Calendar.YEAR, newYear)
                selectedMonth.set(Calendar.MONTH, newMonth)
                selectedMonth.set(Calendar.DAY_OF_MONTH, 1)
                updateMonthHeader()
            },
            year,
            month,
            1
        )
        dialog.show()
    }

    private fun showAddTransactionDialog(isEinnahme: Boolean, dateStr: String) {
        val dialog = AddTransactionDialogFragment.newInstance(isEinnahme, dateStr)
        dialog.show(parentFragmentManager, "AddTransactionDialogFragment")
    }

    private fun showEditTransactionDialog(expense: Expense) {
        val dialog = AddTransactionDialogFragment.newInstance(expense)
        dialog.show(parentFragmentManager, "EditTransactionDialogFragment")
    }

    /**
     * Day-Picker, nur innerhalb dieses Monats.
     */
    private fun showDayPickerForThisMonth(expense: Expense) {
        val parts = expense.datum.split(".")
        if (parts.size != 3) return
        val currentDay = parts[0].toIntOrNull() ?: 1

        val year = selectedMonth.get(Calendar.YEAR)
        val month = selectedMonth.get(Calendar.MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, pickedYear, pickedMonth, pickedDay ->
                if (pickedYear == year && pickedMonth == month) {
                    val newDate = String.format("%02d.%02d.%04d", pickedDay, month + 1, year)
                    val updated = expense.copy(datum = newDate)
                    haushaltsbuchViewModel.updateExpenseInFirestore(updated)
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Falscher Monat")
                        .setMessage("Bitte wählen Sie einen Tag in diesem Monat!")
                        .setPositiveButton("OK", null)
                        .show()
                }
            },
            year,
            month,
            currentDay
        )

        val calMin = Calendar.getInstance()
        calMin.set(year, month, 1, 0, 0, 0)
        datePicker.datePicker.minDate = calMin.timeInMillis

        val maxDay = selectedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val calMax = Calendar.getInstance()
        calMax.set(year, month, maxDay, 23, 59, 59)
        datePicker.datePicker.maxDate = calMax.timeInMillis

        datePicker.show()
    }

    private fun formatMonthForFirestore(cal: Calendar): String {
        val sdf = SimpleDateFormat("MM.yyyy", Locale("de","DE"))
        return sdf.format(cal.time)
    }

    private fun formatMonthName(cal: Calendar): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("de","DE"))
        return sdf.format(cal.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
