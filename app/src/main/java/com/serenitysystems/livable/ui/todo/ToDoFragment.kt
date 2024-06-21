package com.serenitysystems.livable.ui.todo

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.serenitysystems.livable.databinding.FragmentTodoBinding

class ToDoFragment : Fragment(), ToDoClickListener {

    private var _binding: FragmentTodoBinding? = null
    private lateinit var taskViewModel: ToDoViewModel

    // Diese Eigenschaft ist nur zwischen onCreateView und onDestroyView gültig.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        taskViewModel = ViewModelProvider(this).get(ToDoViewModel::class.java)

        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.newTaskButton.setOnClickListener {
            NewTaskSheet(null).show(childFragmentManager, "newTaskTag")
        }
        setRecyclerView()
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setRecyclerView() {
        val mainActivity = this
        taskViewModel.taskItems.observe(viewLifecycleOwner) { taskList ->
            binding.todoListRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = ToDoAdapter(taskList, mainActivity)
            }
        }
    }

    override fun editTaskItem(taskItem: ToDoItem) {
        NewTaskSheet(taskItem).show(childFragmentManager, "newTaskTag")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun completeTaskItem(taskItem: ToDoItem) {
        taskViewModel.setCompleted(taskItem)
    }
}
