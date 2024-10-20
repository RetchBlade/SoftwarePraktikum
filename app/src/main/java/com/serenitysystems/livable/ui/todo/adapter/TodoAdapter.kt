package com.serenitysystems.livable.ui.todo.adapter

import android.graphics.Paint
import android.view.LayoutInflater
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
        fun bind(todo: TodoItem) {
            binding.todoDescription.text = todo.description
            binding.todoCheckBox.isChecked = todo.isDone

            // Set the initial strike-through based on the todo's done status
            updateStrikeThrough(binding.todoDescription, todo.isDone)

            // Set a listener for the checkbox to update the UI and the item's state
            binding.todoCheckBox.setOnCheckedChangeListener { _, isChecked ->
                // Update the UI to reflect the new state
                updateStrikeThrough(binding.todoDescription, isChecked)

                // Optionally, trigger any logic when a todo's done state changes
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


