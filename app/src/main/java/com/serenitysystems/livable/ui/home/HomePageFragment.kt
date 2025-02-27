package com.serenitysystems.livable.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.wochenplan.WochenplanViewModel
import com.serenitysystems.livable.ui.wochenplan.data.DynamicTask
import java.text.SimpleDateFormat
import java.util.*

class HomePageFragment : Fragment() {

    private val homePageViewModel: HomePageViewModel by viewModels()
    private lateinit var wochenplanViewModel: WochenplanViewModel
    private var dialog: AlertDialog? = null
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var userNicknameTextView: TextView
    private lateinit var userPic: ImageView
    private lateinit var todayTasksContainer: LinearLayout
    private lateinit var noTasksMessage: TextView
    private lateinit var overdueTasksContainer: LinearLayout
    private lateinit var overdueTasksTitle: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_page, container, false)

        wochenplanViewModel = ViewModelProvider(this)[WochenplanViewModel::class.java]

        welcomeMessageTextView = view.findViewById(R.id.greetingText)
        userNicknameTextView = view.findViewById(R.id.userNickname)
        userPic = view.findViewById(R.id.imageView)
        todayTasksContainer = view.findViewById(R.id.todayTasksContainer)
        noTasksMessage = view.findViewById(R.id.noTasksMessage)
        overdueTasksContainer = view.findViewById(R.id.overdueTasksContainer)
        overdueTasksTitle = view.findViewById(R.id.overdueTasksTitle)

        homePageViewModel.userNickname.observe(viewLifecycleOwner) { nickname ->
            userNicknameTextView.text = nickname ?: ""
        }

        userPic.setOnClickListener {
            findNavController().navigate(R.id.nav_profilansicht)
        }

        homePageViewModel.userPic.observe(viewLifecycleOwner) { profileImageUrl ->
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.pp)
                .error(R.drawable.pp)
                .into(userPic)
        }

        val wgVerwaltungButton: FrameLayout = view.findViewById(R.id.wgVerwaltungButton)
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it)
        }

        val toDoButton: FrameLayout = view.findViewById(R.id.toDoButton)
        toDoButton.setOnClickListener {
            findNavController().navigate(R.id.nav_todo)
        }

        observeTodayTasksLive()
        observeOverdueTasksLive()

        return view
    }

    private fun getOverdueDays(task: DynamicTask): Int {
        val today = Calendar.getInstance()
        val taskDate = Calendar.getInstance()

        return try {
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY)
            taskDate.time = dateFormat.parse(task.date)!!
            ((today.timeInMillis - taskDate.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }


    private fun observeOverdueTasksLive() {
        homePageViewModel.userNickname.observe(viewLifecycleOwner) { userNickname ->
            if (userNickname.isNullOrEmpty()) return@observe

            wochenplanViewModel.lastWeekTasks.observe(viewLifecycleOwner) { lastWeekTasks ->
                wochenplanViewModel.thisWeekTasks.observe(viewLifecycleOwner) { thisWeekTasks ->
                    homePageViewModel.todayTasks.observe(viewLifecycleOwner) { todayTasks ->
                        overdueTasksContainer.removeAllViews()

                        val filteredOverdueTasks = (lastWeekTasks + thisWeekTasks).filter { task ->
                            !task.isDone && task.assignee == userNickname && getOverdueDays(task) > 0
                        }

                        val filteredTodayTasks = todayTasks.filter { task ->
                            !task.isDone && task.assignee == userNickname
                        }

                        if (filteredOverdueTasks.isNotEmpty() || filteredTodayTasks.isNotEmpty()) {
                            overdueTasksContainer.visibility = View.VISIBLE

                            filteredOverdueTasks.forEach { task ->
                                val taskView = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.task_item_overdue, overdueTasksContainer, false)

                                val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
                                val overdueInfoTextView = taskView.findViewById<TextView>(R.id.taskOverdueInfo)
                                val pointsTextView = taskView.findViewById<TextView>(R.id.taskPoints)

                                descriptionTextView.text = task.description
                                overdueInfoTextView.text = "Überfällig seit ${getOverdueDays(task)} Tagen"
                                pointsTextView.text = "${task.points} Punkte"

                                overdueTasksContainer.addView(taskView)
                            }

                            filteredTodayTasks.forEach { task ->
                                val taskView = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.task_item_todo, overdueTasksContainer, false)

                                val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
                                val priorityTextView = taskView.findViewById<TextView>(R.id.taskPriority)
                                val pointsTextView = taskView.findViewById<TextView>(R.id.taskPoints)

                                descriptionTextView.text = task.description
                                priorityTextView.text = "Priorität: ${task.priority}"
                                pointsTextView.text = "${task.points} Punkte"

                                overdueTasksContainer.addView(taskView)
                            }

                        } else {
                            overdueTasksContainer.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }



    private fun observeTodayTasksLive() {
        homePageViewModel.fetchTodayTasks(wochenplanViewModel)

        homePageViewModel.todayTasks.observe(viewLifecycleOwner) { tasks ->
            todayTasksContainer.removeAllViews()

            if (tasks.isNullOrEmpty()) {
                noTasksMessage.visibility = View.VISIBLE
                todayTasksContainer.visibility = View.GONE
            } else {
                noTasksMessage.visibility = View.GONE
                todayTasksContainer.visibility = View.VISIBLE

                tasks.forEach { task ->
                    val taskView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.task_item_todo, todayTasksContainer, false)

                    val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
                    val priorityTextView = taskView.findViewById<TextView>(R.id.taskPriority)
                    val pointsTextView = taskView.findViewById<TextView>(R.id.taskPoints)

                    descriptionTextView.text = task.description
                    priorityTextView.text = "Priorität: ${task.priority}"
                    pointsTextView.text = "${task.points} Punkte"

                    todayTasksContainer.addView(taskView)
                }
            }
        }
    }

    private fun showWGOptionsDialog(anchorView: View) {
        val dialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.home_dialog_wg_options, null)

        dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.createWGButton).setOnClickListener {
            findNavController().navigate(R.id.nav_wg_registrierung)
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.joinWGButton).setOnClickListener {
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.leaveWGButton).setOnClickListener {
            homePageViewModel.leaveWG()
            dialog?.dismiss()
        }

        dialogView.findViewById<Button>(R.id.showWGInfo).setOnClickListener {
            findNavController().navigate(R.id.nav_wgansichtFragment)
            dialog?.dismiss()
        }

        dialog?.show()
    }
}
