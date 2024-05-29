package com.example.todolist3

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.ui.todo.ToDoItem
import com.serenitysystems.livable.databinding.TaskItemCellBinding

class ToDoAdapter(
    private val taskItems:List<ToDoItem>,
    private val clickListener:ToDoClickListener
):RecyclerView.Adapter<ToDoViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = TaskItemCellBinding.inflate(from, parent, false)
        return ToDoViewHolder(parent.context, binding, clickListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
        holder.bindTaskItem(taskItems[position])
    }

    override fun getItemCount(): Int = taskItems.size
}