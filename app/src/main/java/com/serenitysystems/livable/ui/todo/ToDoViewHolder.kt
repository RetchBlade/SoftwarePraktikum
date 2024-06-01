package com.serenitysystems.livable.ui.todo

import android.content.Context
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.databinding.TaskItemCellBinding
import java.time.format.DateTimeFormatter

class ToDoViewHolder(
    private val context:Context,
    private val binding: TaskItemCellBinding,
    private val clickListener:ToDoClickListener
):RecyclerView.ViewHolder(binding.root)
{
    @RequiresApi(Build.VERSION_CODES.O)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    @RequiresApi(Build.VERSION_CODES.O)
    fun bindTaskItem(taskItem: ToDoItem) {
        binding.name.text = taskItem.name

        if(taskItem.isCompleted()) {
            binding.name.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            binding.dueTime.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        }

        binding.completeButton.setImageResource(taskItem.imageResource())
        binding.completeButton.setColorFilter(taskItem.imageColor(context))

        binding.completeButton.setOnClickListener {
            clickListener.completeTaskItem(taskItem)
        }
        binding.taskCellContainer.setOnClickListener{
            clickListener.editTaskItem(taskItem)
        }

        if(taskItem.dueTime != null) {
            binding.dueTime.text = timeFormat.format(taskItem.dueTime)
        } else {
            binding.dueTime.text = ""
        }
    }
}