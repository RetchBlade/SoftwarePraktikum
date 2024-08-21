package com.serenitysystems.livable.ui.haushaltsbuch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onEditClick: (Expense) -> Unit
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
        expenses = newExpenses
        notifyDataSetChanged()
    }

    inner class ExpenseViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(expenses[position])
                }
            }
        }

        fun bind(expense: Expense) {
            binding.textViewCategory.text = expense.kategorie
            binding.textViewAmount.text = if (expense.istEinnahme) "+${expense.betrag} EUR" else "-${expense.betrag} EUR"
            binding.textViewDate.text = expense.datum
            binding.textViewNotiz.text = expense.notiz  // Notiz'i burada bağlayın

            binding.textViewAmount.setTextColor(
                if (expense.istEinnahme) binding.root.context.getColor(R.color.green)
                else binding.root.context.getColor(R.color.red)
            )
        }
    }
}
