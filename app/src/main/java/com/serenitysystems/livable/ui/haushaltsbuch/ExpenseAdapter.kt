package com.serenitysystems.livable.ui.haushaltsbuch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemExpenseBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense

/**
 * Adapter für eine Liste von Expense-Objekten (Einnahmen oder Ausgaben).
 *
 * - onEditClick: Callback für Bearbeiten
 * - onExpenseUpdated: Wird aufgerufen, wenn ein Eintrag (z. B. nach Undo) wiederhergestellt wird
 * - onExpenseRemoved: Beim finalen Entfernen (Swipe)
 * - onRequestDelete: Zeigt ggf. einen Bestätigungsdialog
 * - onDateClick: Neu: Klick auf das Datum (textViewDate) => öffnet Day-Picker im Fragment
 */
class ExpenseAdapter(
    private var expenses: MutableList<Expense>,
    private val onEditClick: (Expense) -> Unit,
    private val onExpenseUpdated: (Expense) -> Unit,
    private val onExpenseRemoved: (Expense) -> Unit,
    private val onRequestDelete: (Expense, Int) -> Unit,
    private val onDateClick: (Expense) -> Unit  // NEU: Datum-Klick
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(expense)
    }

    override fun getItemCount(): Int = expenses.size

    /**
     * Aktualisiert die gesamte Liste und benachrichtigt den Adapter.
     */
    fun updateExpenses(newExpenses: List<Expense>) {
        expenses.clear()
        expenses.addAll(newExpenses)
        notifyDataSetChanged()
    }

    fun getExpenseAtPosition(position: Int): Expense = expenses[position]

    /**
     * Entfernt ein Item (z.B. beim Swipe) und ruft onExpenseRemoved auf.
     */
    fun removeItem(position: Int) {
        if (position in expenses.indices) {
            val removedExpense = expenses.removeAt(position)
            notifyItemRemoved(position)
            onExpenseRemoved(removedExpense)
        }
    }

    /**
     * Stellt ein zuvor entferntes Item (Undo) wieder her.
     */
    fun restoreExpense(expense: Expense, position: Int) {
        expenses.add(position, expense)
        notifyItemInserted(position)
        onExpenseUpdated(expense)
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Klick auf das gesamte Item => z.B. Menü Bearbeiten/Löschen
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showEditMenu(expenses[position])
                }
            }
            // Klick auf das Datum => onDateClick-Callback
            binding.textViewDate.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val expense = expenses[position]
                    onDateClick(expense)
                }
            }
        }

        fun bind(expense: Expense) {
            binding.textViewCategory.text = expense.kategorie
            binding.textViewAmount.text =
                if (expense.istEinnahme) "+${expense.betrag} EUR" else "-${expense.betrag} EUR"
            binding.textViewDate.text = expense.datum
            binding.textViewNotiz.text = expense.notiz
            binding.textViewUser.text = expense.userNickname

            val ctx = binding.root.context
            val color = if (expense.istEinnahme) ctx.getColor(R.color.green) else ctx.getColor(R.color.red)
            binding.textViewAmount.setTextColor(color)
        }

        private fun showEditMenu(expense: Expense) {
            val builder = AlertDialog.Builder(binding.root.context)
            builder.setTitle("Aktion auswählen")
            builder.setItems(arrayOf("Bearbeiten", "Löschen", "Abbrechen")) { dialog, which ->
                when (which) {
                    0 -> onEditClick(expense)
                    1 -> onRequestDelete(expense, adapterPosition)
                    else -> dialog.dismiss()
                }
            }
            builder.show()
        }
    }
}
