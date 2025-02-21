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
import java.util.Calendar
import java.util.Locale

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

        // ViewModel für Wochenplan initialisieren
        wochenplanViewModel = ViewModelProvider(this)[WochenplanViewModel::class.java]

        // UI-Elemente abrufen
        welcomeMessageTextView = view.findViewById(R.id.greetingText)
        userNicknameTextView = view.findViewById(R.id.userNickname)
        userPic = view.findViewById(R.id.imageView)
        todayTasksContainer = view.findViewById(R.id.todayTasksContainer)
        noTasksMessage = view.findViewById(R.id.noTasksMessage)
        overdueTasksContainer = view.findViewById(R.id.overdueTasksContainer)
        overdueTasksTitle = view.findViewById(R.id.overdueTasksTitle)

        // Nutzer-Info aus dem ViewModel abrufen
        homePageViewModel.userNickname.observe(viewLifecycleOwner, Observer { nickname ->
            userNicknameTextView.text = nickname ?: ""
        })

        userPic.setOnClickListener {
            findNavController().navigate(R.id.nav_profilansicht)
        }

        // Das Benutzerbild vom ViewModel beobachten
        homePageViewModel.userPic.observe(viewLifecycleOwner, Observer { profileImageUrl ->
            if (!profileImageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.pp)
                    .error(R.drawable.pp)
                    .into(userPic)
            } else {
                userPic.setImageResource(R.drawable.pp)
            }
        })

        // WG-Verwaltung Button
        val wgVerwaltungButton: FrameLayout = view.findViewById(R.id.wgVerwaltungButton)
        wgVerwaltungButton.setOnClickListener {
            showWGOptionsDialog(it)
        }

        // To-Do List Button
        val toDoButton: FrameLayout = view.findViewById(R.id.toDoButton)
        toDoButton.setOnClickListener {
            findNavController().navigate(R.id.nav_todo)
        }

        // Heutige Aufgaben laden und anzeigen
        observeTodayTasks()
        observeOverdueTasks()
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


    private fun observeOverdueTasks() {
        homePageViewModel.userNickname.observe(viewLifecycleOwner) { userNickname ->
            wochenplanViewModel.lastWeekTasks.observe(viewLifecycleOwner) { overdueTasks ->
                overdueTasksContainer.removeAllViews()

                val filteredOverdueTasks = overdueTasks.filter { it.assignee == userNickname }

                if (filteredOverdueTasks.isNotEmpty()) {
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
                } else {
                    overdueTasksContainer.visibility = View.GONE
                }
            }
        }
    }


    private fun observeTodayTasks() {
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

                    // Setzt die passende Farbe für die Priorität
                    when (task.priority) {
                        "Hoch" -> priorityTextView.setTextColor(resources.getColor(R.color.priority_high, null))
                        "Mittel" -> priorityTextView.setTextColor(resources.getColor(R.color.priority_medium, null))
                        "Niedrig" -> priorityTextView.setTextColor(resources.getColor(R.color.priority_low, null))
                        else -> priorityTextView.setTextColor(resources.getColor(R.color.own_text_Farbe, null))
                    }

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
            showJoinWGDialog()
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

        val createWGButton: Button = dialogView.findViewById(R.id.createWGButton)
        val joinWGButton: Button = dialogView.findViewById(R.id.joinWGButton)
        val leaveWGButton: Button = dialogView.findViewById(R.id.leaveWGButton)
        val deleteWGButton: Button = dialogView.findViewById(R.id.deleteWGButton)
        val showWGInfoButton: Button = dialogView.findViewById(R.id.showWGInfo)

        homePageViewModel.fetchUserWGInfo({ wgId, wgRole ->
            if (wgId.isNullOrEmpty()) {
                deleteWGButton.visibility = View.GONE
                leaveWGButton.visibility = View.GONE
                showWGInfoButton.visibility = View.GONE
            } else {
                createWGButton.visibility = View.GONE
                joinWGButton.visibility = View.GONE

                if (wgRole == "Wg-Leiter") {
                    deleteWGButton.visibility = View.VISIBLE
                } else {
                    deleteWGButton.visibility = View.GONE
                }

                leaveWGButton.visibility = View.VISIBLE
                showWGInfoButton.visibility = View.VISIBLE
            }
        }, { errorMessage ->
            showErrorDialog(errorMessage)
        })

        dialog?.show()
        positionDialogUnderView(anchorView)
    }

    private fun positionDialogUnderView(anchorView: View) {
        dialog?.window?.let { window ->
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            window.attributes.y = location[1] + anchorView.height + 20
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun showJoinWGDialog() {
        val dialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.home_dialog_join_wg, null)

        val joinWGDialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val wgIdInput: EditText = dialogView.findViewById(R.id.wgIdInput)
        val submitButton: Button = dialogView.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val wgId = wgIdInput.text.toString().trim()
            if (wgId.isNotEmpty()) {
                homePageViewModel.joinWG(wgId) { errorMessage ->
                    showErrorDialog(errorMessage)
                }
                joinWGDialog.dismiss()
            } else {
                showErrorDialog("Bitte geben Sie eine WG-ID ein.")
            }
        }

        joinWGDialog.show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Fehler")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
