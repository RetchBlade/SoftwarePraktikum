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

/**
 * Fragment für eine **monatliche** Ansicht von Ausgaben (istEinnahme=false).
 * - onResume: lädt den aktuellen Monat erneut (Tab-Wechsel-Refresh).
 * - Klick auf das Datum (textViewDate) erlaubt nur Tage innerhalb dieses Monats.
 */
class AusgabenFragment : Fragment() {

    private var _binding: FragmentAusgabenBinding? = null
    private val binding get() = _binding!!

    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()
    private lateinit var adapter: ExpenseAdapter

    // Speichert das aktuell ausgewählte Monat/Jahr
    private var selectedMonth: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAusgabenBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Initialisierung der UI, nachdem die View erstellt wurde.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    /**
     * Beim Tab-Wechsel oder erneuten Betreten des Fragments:
     * onResume -> Liste erneut laden, damit keine Daten "verschwinden".
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
            // NEU: onDateClick => Day-Picker nur für diesen Monat
            onDateClick = { expense -> showDayPickerForThisMonth(expense) }
        )
        binding.recyclerView.adapter = adapter

        // Beobachte alle Ausgaben/Einnahmen (allExpenses) => Monat filtern
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            loadCurrentMonth()
        }

        // selectedDateExpenses => nur Ausgaben
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { fullList ->
            val ausgaben = fullList.filter { !it.istEinnahme }
            adapter.updateExpenses(ausgaben)
        }

        // Kontostand (Monatsbilanz)
        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner) { kontostand ->
            binding.textViewKontostand.text = "Kontostand: %.2f EUR".format(kontostand)
        }

        // Neue Ausgabe hinzufügen (Tag=1)
        binding.fab.setOnClickListener {
            val dateStr = "01.${formatMonthForFirestore(selectedMonth)}"
            showAddTransactionDialog(false, dateStr)
        }

        // Monat in dateTextView anzeigen
        updateMonthHeader()

        // Pfeile: Monat -1, +1
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

        // Klick auf das Datum => DatePickerDialog nur für Monat/Jahr
        binding.dateTextView.setOnClickListener {
            showMonthPickerDialog()
        }

        // SwipeToDelete
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

    /**
     * Erlaubt die Auswahl von Monat/Jahr (Tag wird ignoriert).
     */
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
     * Öffnet einen Day-Picker, der nur Tage des aktuellen Monats zulässt.
     */
    private fun showDayPickerForThisMonth(expense: Expense) {
        val parts = expense.datum.split(".") // z.B. "15.03.2025"
        if (parts.size != 3) return
        val currentDay = parts[0].toIntOrNull() ?: 1

        val year = selectedMonth.get(Calendar.YEAR)
        val month = selectedMonth.get(Calendar.MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, pickedYear, pickedMonth, pickedDay ->
                if (pickedYear == year && pickedMonth == month) {
                    val newDate = String.format("%02d.%02d.%04d", pickedDay, month + 1, year)
                    val updatedExpense = expense.copy(datum = newDate)
                    haushaltsbuchViewModel.updateExpenseInFirestore(updatedExpense)
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

        // minDate
        val calMin = Calendar.getInstance()
        calMin.set(year, month, 1, 0, 0, 0)
        datePicker.datePicker.minDate = calMin.timeInMillis

        // maxDate
        val maxDay = selectedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val calMax = Calendar.getInstance()
        calMax.set(year, month, maxDay, 23, 59, 59)
        datePicker.datePicker.maxDate = calMax.timeInMillis

        datePicker.show()
    }

    /**
     * Formatiert das Calendar-Objekt als "MM.yyyy" (z.B. "03.2025").
     */
    private fun formatMonthForFirestore(cal: Calendar): String {
        val sdf = SimpleDateFormat("MM.yyyy", Locale("de","DE"))
        return sdf.format(cal.time)
    }

    /**
     * Nur Anzeige: "März 2025".
     */
    private fun formatMonthName(cal: Calendar): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("de","DE"))
        return sdf.format(cal.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
