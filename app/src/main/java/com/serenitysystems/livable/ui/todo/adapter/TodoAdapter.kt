package com.serenitysystems.livable.ui.todo.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.TodoItemBinding
import com.serenitysystems.livable.ui.todo.data.TodoItem
import java.text.SimpleDateFormat
import java.util.Locale

class TodoAdapter(private val onTodoClick: (TodoItem) -> Unit) : ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = TodoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = getItem(position)
        holder.bind(todo)
    }

    inner class TodoViewHolder(private val binding: TodoItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(todo: TodoItem) {
            binding.todoDescription.text = todo.description
            binding.todoDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(todo.date)
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
                binding.todoDetailedDescription.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.deleteButton.visibility = if (isExpanded) View.VISIBLE else View.GONE
            }

            binding.todoCheckBox.isChecked = todo.isDone
            updateStrikeThrough(binding.todoDescription, todo.isDone)

            binding.todoCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(binding.todoDescription, isChecked)
                val updatedTodo = todo.copy(isDone = isChecked)
                onTodoClick(updatedTodo) // Nur den Status aktualisieren
            }

            binding.deleteButton.setOnClickListener {
                // Informiere, dass das Todo durch den Müll-Knopf gelöscht wurde
                onTodoClick(todo.copy(isDone = true, detailedDescription = "deleted_by_button"))
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



