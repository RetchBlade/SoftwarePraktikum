package com.serenitysystems.livable.ui.todo.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.databinding.TodoItemBinding
import com.serenitysystems.livable.ui.todo.data.TodoItem

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
            binding.todoCheckBox.isChecked = todo.isDone

            // Set initial state of the detailed description
            binding.todoDetailedDescription.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Set strike-through if the task is done
            updateStrikeThrough(binding.todoDescription, todo.isDone)

            // Toggle visibility of detailed description on click
            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                binding.todoDetailedDescription.visibility = if (isExpanded) View.VISIBLE else View.GONE
            }

            // Handle checkbox state change
            binding.todoCheckBox.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(binding.todoDescription, isChecked)
                val updatedTodo = todo.copy(isDone = isChecked)
                onTodoClick(updatedTodo)
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


