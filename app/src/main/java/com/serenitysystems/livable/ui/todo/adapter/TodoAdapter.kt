package com.serenitysystems.livable.ui.todo.adapter

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.TodoItemBinding
import com.serenitysystems.livable.ui.todo.data.TodoItem
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import android.view.inputmethod.InputMethodManager


class TodoAdapter(private val onTodoClick: (TodoItem) -> Unit) :
    ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = TodoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = getItem(position)
        holder.bind(todo)
    }

    inner class TodoViewHolder(private val binding: TodoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(todo: TodoItem) {
            binding.todoDescription.text = todo.description
            binding.todoDate.text =
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(todo.date)
            isExpanded = false
            binding.todoDetailedDescription.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.deleteButton.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.todoDetailedDescription.setText(todo.detailedDescription)

            when (todo.priority) {
                "Mittel" -> binding.root.setBackgroundResource(R.drawable.rounded_todo_item_medium)
                "Hoch" -> binding.root.setBackgroundResource(R.drawable.rounded_todo_item_high)
                else -> binding.root.setBackgroundResource(R.drawable.rounded_todo_item)
            }

            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                binding.todoDetailedDescription.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE
                binding.deleteButton.visibility = if (isExpanded) View.VISIBLE else View.GONE


                // Den Fokus von der EditText entfernen
                binding.todoDetailedDescription.clearFocus()

                // Tastatur schließen
                val imm =
                    binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.todoDetailedDescription.windowToken, 0)

                // Todo speichern, falls notwendig
                val updatedTodo =
                    todo.copy(detailedDescription = binding.todoDetailedDescription.text.toString())
                onTodoClick(updatedTodo)
            }

            binding.todoCheckBox.isChecked = todo.isDone
            updateStrikeThrough(binding.todoDescription, todo.isDone)

            binding.todoCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(binding.todoDescription, isChecked)

                // Erstelle das aktualisierte Todo
                val updatedTodo = todo.copy(isDone = isChecked)

                // Logging, um sicherzustellen, dass die richtigen Daten verarbeitet werden
                Log.d("Todo", "Updating todo: ${updatedTodo.id}, isDone: ${updatedTodo.isDone}")

                // Todo weitergeben
                onTodoClick(updatedTodo)
            }

            binding.deleteButton.setOnClickListener {
                // Informiere ViewModel, dass der Button-Klick ein forceDelete auslösen soll
                onTodoClick(todo.copy(detailedDescription = "deleted_by_button"))
            }
        }

        private fun updateStrikeThrough(textView: TextView, isChecked: Boolean) {
            if (isChecked) {
                textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
}



