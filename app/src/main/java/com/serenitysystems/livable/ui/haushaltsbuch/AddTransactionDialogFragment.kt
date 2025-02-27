package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.DialogAddTransactionBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import java.util.Calendar

/**
 * DialogFragment zum Hinzufügen oder Bearbeiten einer Transaktion (Einnahme oder Ausgabe).
 * Hier kann der Benutzer ein Datum direkt im Dialog wählen,
 * indem er auf das Feld editDate klickt (DatePickerDialog).
 */
class AddTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    companion object {
        private const val ARG_IS_EINNAHME = "is_einnahme"
        private const val ARG_EXPENSE = "expense"
        private const val ARG_DATE = "selected_date"

        /**
         * Konstruktor für NEUE Transaktion.
         * isEinnahme + ausgewähltes Datum (dd.MM.yyyy) kann übergeben werden.
         */
        fun newInstance(isEinnahme: Boolean, selectedDate: String): AddTransactionDialogFragment {
            val args = Bundle().apply {
                putBoolean(ARG_IS_EINNAHME, isEinnahme)
                putString(ARG_DATE, selectedDate)
            }
            return AddTransactionDialogFragment().apply { arguments = args }
        }

        /**
         * Konstruktor für BEARBEITEN einer bestehenden Transaktion.
         * Datum bleibt wie im Expense-Objekt gespeichert.
         */
        fun newInstance(expense: Expense): AddTransactionDialogFragment {
            val args = Bundle().apply {
                putParcelable(ARG_EXPENSE, expense)
            }
            return AddTransactionDialogFragment().apply { arguments = args }
        }
    }

    override fun onStart() {
        super.onStart()
        // Dialoggröße anpassen
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

    /**
     * Befüllt die Felder (Betrag, Notiz, Kategorie) und bindet einen Klicklistener
     * für das Datum-Feld (editDate), damit ein DatePickerDialog geöffnet wird.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expenseArg = arguments?.getParcelable<Expense>(ARG_EXPENSE)
        val isEinnahmeArg = arguments?.getBoolean(ARG_IS_EINNAHME)
        val selectedDateArg = arguments?.getString(ARG_DATE) // "dd.MM.yyyy"

        // Bestimmen, ob Einnahme oder Ausgabe
        val isEinnahme = expenseArg?.istEinnahme ?: (isEinnahmeArg ?: false)

        // Titel und Hintergrundfarbe
        if (isEinnahme) {
            binding.title.text = getString(R.string.einnahme_button_text)
            binding.title.setBackgroundResource(R.drawable.green_filled_background)
        } else {
            binding.title.text = getString(R.string.ausgabe_button_text)
            binding.title.setBackgroundResource(R.drawable.red_filled_background)
        }

        // Kategorien ins Spinner-Widget
        val categories = haushaltsbuchViewModel.categories
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Falls wir eine vorhandene Transaktion bearbeiten, Felder befüllen
        expenseArg?.let { expense ->
            binding.editAmount.setText(expense.betrag.toString())
            binding.editNote.setText(expense.notiz)

            val idx = categories.indexOf(expense.kategorie)
            if (idx >= 0) {
                binding.spinnerCategory.setSelection(idx)
            }
            // Datum in editDate
            binding.editDate.setText(expense.datum)
        } ?: run {
            // Neue Transaktion: Datum aus ARG_DATE (falls übergeben)
            if (!selectedDateArg.isNullOrEmpty()) {
                binding.editDate.setText(selectedDateArg)
            }
        }

        // Datum-Feld klickbar machen, damit ein DatePickerDialog erscheint
        binding.editDate.isClickable = true
        // Falls ein EditText: wir wollen keine direkte Texteingabe
        binding.editDate.isFocusable = false

        binding.editDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Speichern-Button
        binding.saveButton.setOnClickListener {
            saveTransaction(isEinnahme)
        }

        // Abbrechen-Button
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Zeigt einen DatePickerDialog an, basierend auf dem aktuell im Feld editDate stehenden Datum
     * oder, falls kein Datum gesetzt ist, dem heutigen Tag.
     * Das Ergebnis wird in editDate geschrieben.
     */
    private fun showDatePickerDialog() {
        val currentText = binding.editDate.text.toString()
        val parts = currentText.split(".")
        val cal = Calendar.getInstance()

        if (parts.size == 3) {
            // Versuch, Tag.Monat.Jahr zu parsen
            val day = parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
            val month = (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH)+1)) - 1
            val year = parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR)
            cal.set(year, month, day)
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, pickedYear, pickedMonth, pickedDay ->
                val newDate = String.format("%02d.%02d.%04d", pickedDay, pickedMonth + 1, pickedYear)
                binding.editDate.setText(newDate)
            },
            year,
            month,
            day
        )

        datePicker.show()
    }

    /**
     * Liest alle Felder aus und schreibt bzw. aktualisiert den Eintrag in Firestore.
     * Das Datum kommt jetzt aus editDate (Textfeld).
     */
    private fun saveTransaction(isEinnahme: Boolean) {
        val amount = binding.editAmount.text.toString().toFloatOrNull()
        val note = binding.editNote.text.toString()
        val category = binding.spinnerCategory.selectedItem?.toString()
        val date = binding.editDate.text.toString()

        if (amount == null || category.isNullOrEmpty() || date.isEmpty()) {
            Snackbar.make(
                binding.root,
                "Bitte Betrag, Kategorie und ein Datum angeben!",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val expenseArg = arguments?.getParcelable<Expense>(ARG_EXPENSE)

        val newExpense = expenseArg?.copy(
            betrag = amount,
            notiz = note,
            kategorie = category,
            datum = date,
            istEinnahme = isEinnahme
        ) ?: Expense(
            betrag = amount,
            notiz = note,
            kategorie = category,
            datum = date,
            istEinnahme = isEinnahme
        )

        // Neu oder Update?
        if (expenseArg != null && expenseArg.id.isNotEmpty()) {
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
