package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentHaushaltsbuchBinding
import java.text.SimpleDateFormat
import java.util.*

class HaushaltsbuchFragment : Fragment() {

    private var _binding: FragmentHaushaltsbuchBinding? = null
    private val binding get() = _binding!!
    private lateinit var haushaltsbuchViewModel: HaushaltsbuchViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        haushaltsbuchViewModel =
            ViewModelProvider(this).get(HaushaltsbuchViewModel::class.java)

        _binding = FragmentHaushaltsbuchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        haushaltsbuchViewModel.selectedDate.observe(viewLifecycleOwner, Observer { date ->
            updateDateText(date)
        })

        haushaltsbuchViewModel.expensesForDate.observe(viewLifecycleOwner, Observer { expenses ->
            updateExpensesUI(expenses)
            updateBarChart()
        })

        binding.buttonLeft.setOnClickListener {
            haushaltsbuchViewModel.changeDate(-1)
        }

        binding.buttonRight.setOnClickListener {
            haushaltsbuchViewModel.changeDate(1)
        }

        binding.buttonEinnahme.setOnClickListener {
            showAddTransactionDialog(true)
        }

        binding.buttonAusgabe.setOnClickListener {
            showAddTransactionDialog(false)
        }

        binding.textMonat.setOnClickListener {
            showDatePickerDialog { date -> haushaltsbuchViewModel.changeDateTo(date) }
        }

        return root
    }

    private fun updateDateText(date: Date) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding.textMonat.text = dateFormat.format(date)
    }

    private fun showDatePickerDialog(onDateSet: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSet(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showAddTransactionDialog(isEinnahme: Boolean) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(if (isEinnahme) "Einnahme hinzufügen" else "Ausgabe hinzufügen")

        val view = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        builder.setView(view)

        val categoryInput = view.findViewById<EditText>(R.id.editCategory)
        val amountInput = view.findViewById<EditText>(R.id.editAmount)
        val noteInput = view.findViewById<EditText>(R.id.editNote)
        val dateInput = view.findViewById<EditText>(R.id.editDate)

        dateInput.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))
        dateInput.setOnClickListener {
            showDatePickerDialog { date -> dateInput.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)) }
        }

        builder.setPositiveButton("Hinzufügen") { dialog, _ ->
            val category = categoryInput.text.toString()
            val amount = amountInput.text.toString().toFloatOrNull() ?: 0f
            val note = noteInput.text.toString()
            val date = dateInput.text.toString()

            haushaltsbuchViewModel.addExpense(category, amount, note, isEinnahme)
            dialog.dismiss()
        }

        builder.setNegativeButton("Abbrechen") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun updateExpensesUI(expenses: List<Expense>) {
        binding.listeLayout.removeAllViews()

        expenses.forEach { expense ->
            val expenseView = LayoutInflater.from(context).inflate(R.layout.expense_item, null)
            val textViewCategory = expenseView.findViewById<TextView>(R.id.textViewCategory)
            val textViewAmount = expenseView.findViewById<TextView>(R.id.textViewAmount)
            val textViewDescription = expenseView.findViewById<TextView>(R.id.textViewDescription)

            textViewCategory.text = expense.kategorie
            textViewAmount.text = "${expense.betrag} EUR"
            textViewDescription.text = expense.beschreibung

            if (expense.isStrikethrough) {
                expenseView.alpha = 0.5f
                textViewCategory.paintFlags = textViewCategory.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            }

            binding.listeLayout.addView(expenseView)
        }
    }

    private fun updateBarChart() {
        binding.kategoriLayout.removeAllViews()

        haushaltsbuchViewModel.getCategories().forEach { category ->
            val totalExpenseForCategory = haushaltsbuchViewModel.expensesForDate.value
                ?.filter { it.kategorie == category && !it.istEinnahme }
                ?.sumOf { it.betrag.toDouble() }
                ?: 0.0

            if (totalExpenseForCategory > 0f) {
                val barView = LayoutInflater.from(context).inflate(R.layout.bar_item, null)
                val textViewCategory = barView.findViewById<TextView>(R.id.textViewCategory)
                val bar = barView.findViewById<View>(R.id.balken)

                textViewCategory.text = category
                bar.setBackgroundColor(android.graphics.Color.parseColor(haushaltsbuchViewModel.getCategoryColor(category)))

                val kontostand = haushaltsbuchViewModel.kontostand.value ?: 1f
                val width = (totalExpenseForCategory / kontostand) * binding.kategoriLayout.width
                bar.layoutParams.width = width.toInt()

                binding.kategoriLayout.addView(barView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
