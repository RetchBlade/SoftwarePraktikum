package com.serenitysystems.livable.ui.todo.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
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

            // Streichen des Textes, wenn das Todo erledigt ist
            if (todo.isDone) {
                binding.todoDescription.paintFlags = binding.todoDescription.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.todoDescription.paintFlags = binding.todoDescription.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Entferne den Klicklistener, der das Dialogfenster aufruft
            // Hier könnte man eine andere Aktion beim Klick hinzufügen, wenn gewünscht
            // binding.root.setOnClickListener {
            //     // Zum Beispiel: eine Snackbar anzeigen oder keine Aktion ausführen
            // }
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

