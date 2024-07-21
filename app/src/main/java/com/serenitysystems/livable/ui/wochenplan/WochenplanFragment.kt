package com.serenitysystems.livable.ui.wochenplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentWochenplanBinding
import com.serenitysystems.livable.ui.wochenplan.data.Task
import java.util.*

class WochenplanFragment : Fragment() {

    private var _binding: FragmentWochenplanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val wochenplanViewModel =
            ViewModelProvider(this).get(WochenplanViewModel::class.java)

        _binding = FragmentWochenplanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        wochenplanViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            displayTasks(tasks)
        }

        return root
    }

    private fun displayTasks(tasks: List<Task>) {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val daysOfWeek = arrayOf("Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag")
        val startIndex = today - 1

        val layout = binding.taskLayout
        layout.removeAllViews()

        for (i in 0..4) {
            val dayIndex = (startIndex + i) % 7
            val day = daysOfWeek[dayIndex]

            val dayTasks = tasks.filter { it.day == day }

            val dayHeader = LayoutInflater.from(context).inflate(R.layout.wochenplan_day_header, layout, false)
            dayHeader.findViewById<TextView>(R.id.dayTitle).text = day
            layout.addView(dayHeader)

            for (task in dayTasks) {
                val taskView = LayoutInflater.from(context).inflate(R.layout.wochenplan_task_item, layout, false)
                taskView.findViewById<TextView>(R.id.taskDescription).text = task.description
                taskView.findViewById<TextView>(R.id.taskPriority).text = task.priority
                taskView.findViewById<TextView>(R.id.taskPoints).text = "${task.points} Punkte"
                taskView.findViewById<ImageView>(R.id.taskAssigneeAvatar).setImageResource(task.avatar)
                layout.addView(taskView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
