package com.serenitysystems.livable.ui.haushaltsbuch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemExpenseBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense

class ExpenseAdapter(
    private var expenses: MutableList<Expense>,
    private val onEditClick: (Expense) -> Unit,
    private val onExpenseUpdated: (Expense) -> Unit,
    private val onExpenseRemoved: (Expense) -> Unit,
    val onRequestDelete: (Expense, Int) -> Unit
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


    fun updateExpenses(newExpenses: List<Expense>) {
        expenses.clear()
        expenses.addAll(newExpenses)
        notifyDataSetChanged()
    }

    fun getExpenseAtPosition(position: Int): Expense = expenses[position]


    fun removeItem(position: Int) {
        if (position in expenses.indices) {
            val removedExpense = expenses.removeAt(position)
            notifyItemRemoved(position)
            onExpenseRemoved(removedExpense)
        }
    }


    fun restoreExpense(expense: Expense, position: Int) {
        expenses.add(position, expense)
        notifyItemInserted(position)
        onExpenseUpdated(expense)
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showEditMenu(expenses[position])
                }
            }
        }

        fun bind(expense: Expense) {
            binding.textViewCategory.text = expense.kategorie
            binding.textViewAmount.text =
                if (expense.istEinnahme) "+${expense.betrag} EUR" else "-${expense.betrag} EUR"
            binding.textViewDate.text = expense.datum
            binding.textViewNotiz.text = expense.notiz
            binding.textViewUser.text = expense.userNickname // Nickname anzeigen

            binding.textViewAmount.setTextColor(
                if (expense.istEinnahme) binding.root.context.getColor(R.color.green)
                else binding.root.context.getColor(R.color.red)
            )
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
