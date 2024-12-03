package com.serenitysystems.livable.ui.wochenplan

import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentWochenplanBinding
import com.serenitysystems.livable.ui.wochenplan.adapter.DayPagerAdapter
import com.serenitysystems.livable.ui.wochenplan.data.DynamicTask
import java.text.SimpleDateFormat
import java.util.*

class WochenplanFragment : Fragment() {
    private var _binding: FragmentWochenplanBinding? = null
    private val binding get() = _binding!!
    private lateinit var wochenplanViewModel: WochenplanViewModel
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("de", "DE"))
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        wochenplanViewModel = ViewModelProvider(this)[WochenplanViewModel::class.java]
        _binding = FragmentWochenplanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupTabs()

        wochenplanViewModel.tasks.observe(viewLifecycleOwner) {
            updateTabs()
            updateTabIcons()
        }

        wochenplanViewModel.lastWeekTasks.observe(viewLifecycleOwner) {
            updateTabIcons()
            if (binding.tabLayout.selectedTabPosition == 0) {
                loadLastWeek()
            }
        }

        wochenplanViewModel.thisWeekTasks.observe(viewLifecycleOwner) {
            updateTabIcons()
            if (binding.tabLayout.selectedTabPosition == 1) {
                loadThisWeek()
            }
        }

        wochenplanViewModel.nextWeekTasks.observe(viewLifecycleOwner) {
            if (binding.tabLayout.selectedTabPosition == 2) {
                loadNextWeek()
            }
        }

        binding.addTaskButton.setOnClickListener {
            showTaskDialog()
        }

        return root
    }

    private fun setupTabs() {
        binding.tabLayout.apply {
            // Tab for "Letzte Woche" with an icon
            val lastWeekTab =
                newTab().setCustomView(createTabWithIcon(R.drawable.ic_lastweek, false))
            addTab(lastWeekTab)

            // Tab for "Diese Woche" with an icon
            val thisWeekTab =
                newTab().setCustomView(createTabWithIcon(R.drawable.ic_wochencalender, false))
            addTab(thisWeekTab)

            // Tab for "Nächste Woche" with an icon
            val nextWeekTab =
                newTab().setCustomView(createTabWithIcon(R.drawable.ic_nextweek, false))
            addTab(nextWeekTab)

            // Set the second tab ("Diese Woche") as selected by default
            getTabAt(1)?.select()
            loadThisWeek()

            // Add tab selection listener
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> loadLastWeek()
                        1 -> loadThisWeek()
                        2 -> loadNextWeek()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    private fun createTabWithIcon(iconResId: Int, showWarningIcon: Boolean): View {
        return LayoutInflater.from(requireContext()).inflate(R.layout.tab_with_warning_icon, null)
            .apply {
                val tabIcon = findViewById<ImageView>(R.id.tabIcon)
                val warningIcon = findViewById<ImageView>(R.id.warningIcon)

                tabIcon.setImageResource(iconResId)
                warningIcon.visibility = if (showWarningIcon) View.VISIBLE else View.GONE
            }
    }


    private fun setupDayViewPager() {
        val dayAdapter = DayPagerAdapter(wochenplanViewModel.daysOfWeek) { day ->
            displayTasksForDay(day)
        }
        binding.dayViewPager.adapter = dayAdapter
        binding.dayViewPager.setCurrentItem(0, false)

        binding.dayViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                displayTasksForDay(wochenplanViewModel.daysOfWeek[position])
            }
        })
    }

    private fun loadLastWeek() {
        displayTasks(wochenplanViewModel.lastWeekTasks.value ?: emptyList())
    }

    private fun loadThisWeek() {
        displayTasks(wochenplanViewModel.thisWeekTasks.value ?: emptyList())
    }

    private fun loadNextWeek() {
        displayTasks(wochenplanViewModel.nextWeekTasks.value ?: emptyList())
    }

    private fun updateDaysList(startDate: Calendar, range: Int) {
        val days = mutableListOf<String>()
        for (i in 0 until range) {
            days.add(dateFormat.format(startDate.time))
            startDate.add(Calendar.DAY_OF_YEAR, 1)
        }
        wochenplanViewModel.daysOfWeek = days

        setupDayViewPager()
        displayTasksForDay(getSelectedDay())
    }

    private fun getSelectedDay(): String {
        return wochenplanViewModel.daysOfWeek[binding.dayViewPager.currentItem]
    }


    private fun isRecurringTask(task: DynamicTask, currentDay: Calendar): Boolean {
        val taskDate = try {
            Calendar.getInstance().apply {
                time = dateFormat.parse(task.date)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return when (task.repeatFrequency) {
            "Täglich" -> true
            "Wöchentlich" -> taskDate.get(Calendar.DAY_OF_WEEK) == currentDay.get(Calendar.DAY_OF_WEEK)
            "Monatlich" -> taskDate.get(Calendar.DAY_OF_MONTH) == currentDay.get(Calendar.DAY_OF_MONTH)
            else -> false
        }
    }

    private fun isSameWeek(date: Calendar, weekStart: Calendar): Boolean {
        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_YEAR, 6)
        return date.after(weekStart) && date.before(weekEnd)
    }

    private fun getDayOfWeekIndex(date: String): Int {
        return try {
            val taskDate = Calendar.getInstance()
            taskDate.time = dateFormat.parse(date)
            when (taskDate.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 8 // Default to end of the week if invalid
            }
        } catch (e: Exception) {
            e.printStackTrace()
            8 // Invalid date
        }
    }

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateTabs() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> loadLastWeek()
            1 -> loadThisWeek()
            2 -> loadNextWeek()
        }
    }

    private fun updateTabIcons() {
        val tabLayout = binding.tabLayout

        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val customView = tab?.customView
            val tabIcon = customView?.findViewById<ImageView>(R.id.tabIcon)
            val warningIcon = customView?.findViewById<ImageView>(R.id.warningIcon)

            // Determine if the tab should show a warning icon
            val showWarningIcon = when (i) {
                0 -> wochenplanViewModel.lastWeekTasks.value?.any { task ->
                    !task.isDone && task.priority.startsWith("Überfällig")
                } ?: false

                1 -> wochenplanViewModel.thisWeekTasks.value?.any { task ->
                    !task.isDone && task.priority.startsWith("Überfällig")
                } ?: false

                else -> false
            }

            // Set the icon and warning visibility
            when (i) {
                0 -> tabIcon?.setImageResource(R.drawable.ic_lastweek)
                1 -> tabIcon?.setImageResource(R.drawable.ic_wochencalender)
                2 -> tabIcon?.setImageResource(R.drawable.ic_nextweek)
            }

            warningIcon?.visibility = if (showWarningIcon) View.VISIBLE else View.GONE
        }
    }


    private fun displayTasksForDay(day: String) {
        val dayTasks = wochenplanViewModel.tasks.value?.filter {
            it.date == day || (it.isRepeating && shouldTaskBeShownToday(it, day))
        } ?: emptyList()
        displayTasks(dayTasks)
    }

    private fun displayTasks(tasks: List<DynamicTask>) {
        val layout = binding.taskLayout
        layout.removeAllViews()

        // Aufgaben nach Datum gruppieren
        val groupedTasks = tasks.groupBy { it.date }

        // Heutiges Datum und Mitternacht abrufen
        val today = Calendar.getInstance()
        val midnight = today.clone() as Calendar
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.set(Calendar.MILLISECOND, 0)

        for ((date, tasksForDate) in groupedTasks) {
            // Überschrift mit dem Datum hinzufügen
            val dateHeaderView = TextView(requireContext())
            dateHeaderView.text = formatDateHeader(date)
            dateHeaderView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.own_text_Farbe
                )
            )
            dateHeaderView.setPadding(16, 16, 16, 8)
            layout.addView(dateHeaderView)

            // Aufgaben für das jeweilige Datum hinzufügen
            for (task in tasksForDate) {
                val taskView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.wochenplan_task_item_new, layout, false)

                val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
                val priorityTextView = taskView.findViewById<TextView>(R.id.taskPriority)
                val pointsTextView = taskView.findViewById<TextView>(R.id.taskPoints)
                val assigneeTextView = taskView.findViewById<TextView>(R.id.taskAsignee)
                val assigneeAvatarImageView =
                    taskView.findViewById<ImageView>(R.id.taskAssigneeAvatar)
                val taskOptionsImageView = taskView.findViewById<ImageView>(R.id.taskOptions)

                // Originale Priorität speichern
                val originalPriority = task.priority

                // Aufgabendetails setzen
                descriptionTextView.text = task.description
                priorityTextView.text = task.priority
                pointsTextView.text = "${task.points} Punkte"
                assigneeTextView.text = task.assignee

                // Avatar mit Glide laden
                task.assigneeEmail?.let { assigneeEmail ->
                    db.collection("users").document(assigneeEmail)
                        .get()
                        .addOnSuccessListener { document ->
                            val profileImageUrl = document.getString("profileImageUrl")
                            if (!profileImageUrl.isNullOrEmpty()) {
                                Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(assigneeAvatarImageView)
                            } else {
                                // Wenn kein Bild vorhanden ist, Standard-Avatar setzen
                                assigneeAvatarImageView.setImageResource(R.drawable.logo)
                            }
                        }
                }

                // Dynamische Anpassung der Höhe der Beschreibung
                descriptionTextView.post {
                    descriptionTextView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    descriptionTextView.requestLayout()
                }

                // Datum der Aufgabe analysieren
                val taskDate = Calendar.getInstance()
                try {
                    taskDate.time = dateFormat.parse(task.date)

                    if (!task.isDone) {
                        if (taskDate.before(midnight)) {
                            // Aufgabe ist überfällig
                            val temp = task.priority

                            val overdueDays =
                                ((today.timeInMillis - taskDate.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                            val overdueText = if (overdueDays == 1) {
                                "Überfällig: 1 Tag"
                            } else {
                                "Überfällig: $overdueDays Tage"
                            }
                            task.priority = overdueText
                            if(task.priority != temp){
                                wochenplanViewModel.updateTask(task)
                            }
                            priorityTextView.text = overdueText
                            taskView.background = ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.overdue_task_background
                            )
                            priorityTextView.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.priority_overdue)
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Stil für abgeschlossene Aufgaben anwenden
                if (task.isDone) {
                    // Hintergrund für erledigte Aufgaben setzen
                    taskView.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.normal_task_background
                    )
                    descriptionTextView.paintFlags =
                        descriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    // Überfällige Aufgaben behalten ihre Farbe und werden gestrichen
                    if (originalPriority.startsWith("Überfällig")) {
                        priorityTextView.text = originalPriority
                        priorityTextView.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.own_light_gray)
                        )
                    } else {
                        // Originale Priorität und Textfarbe beibehalten
                        priorityTextView.text = originalPriority
                        when (originalPriority) {
                            "Hoch" -> priorityTextView.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.priority_high)
                            )

                            "Mittel" -> priorityTextView.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.priority_medium)
                            )

                            "Niedrig" -> priorityTextView.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.priority_low)
                            )
                        }
                    }
                } else if (!task.priority.startsWith("Überfällig")) {
                    // Farben basierend auf der Priorität setzen (für nicht überfällige Aufgaben)
                    when (task.priority) {
                        "Hoch" -> priorityTextView.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.priority_high)
                        )

                        "Mittel" -> priorityTextView.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.priority_medium)
                        )

                        "Niedrig" -> priorityTextView.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.priority_low)
                        )
                    }
                }

                // Klick-Listener für Aufgabenoptionen setzen
                taskOptionsImageView.setOnClickListener {
                    showTaskOptions(task)
                }

                // Aufgabe zur Layoutansicht hinzufügen
                layout.addView(taskView)
            }
        }
    }


    private fun formatDateHeader(date: String): String {
        return try {
            val parsedDate = dateFormat.parse(date)
            val dayOfWeekFormat =
                SimpleDateFormat("E", Locale("de", "DE")) // Short day format with one dot
            val dateFormat = SimpleDateFormat(
                "dd.MMM",
                Locale("de", "DE")
            ) // Date format with abbreviated month, no year
            parsedDate?.let { "${dayOfWeekFormat.format(it)}, ${dateFormat.format(it)}" } ?: date
        } catch (e: Exception) {
            e.printStackTrace()
            date
        }
    }

    private fun showTaskOptions(task: DynamicTask) {
        val options = if (task.isDone) {
            arrayOf("Nicht erledigt", "Bearbeiten", "Löschen")
        } else {
            arrayOf("Erledigt", "Bearbeiten", "Löschen")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.task_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (task.isDone) {
                            markTaskAsNotDone(task)
                        } else {
                            markTaskAsDone(task)
                        }
                    }

                    1 -> showTaskDialog(task)
                    2 -> deleteTask(task)
                }
            }
            .create()

        dialog.show()
    }


    private fun markTaskAsDone(task: DynamicTask) {
        val updatedTask = task.copy(isDone = true)
        // Update the task in the ViewModel
        wochenplanViewModel.updateTask(updatedTask)
        updateTabs()
    }

    private fun markTaskAsNotDone(task: DynamicTask) {
        task.copy().apply {
            isDone = false
            wochenplanViewModel.updateTask(this)
        }
        updateTabs()
    }

    private fun deleteTask(task: DynamicTask) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_task_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                wochenplanViewModel.deleteTask(task)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    private fun shouldTaskBeShownToday(task: DynamicTask, day: String): Boolean {
        val selectedDate = try {
            Calendar.getInstance().apply {
                time = dateFormat.parse(day)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        val taskDate = try {
            Calendar.getInstance().apply {
                time = dateFormat.parse(task.date)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return when (task.repeatFrequency) {
            "Wöchentlich" -> {
                taskDate.get(Calendar.DAY_OF_WEEK) == selectedDate.get(Calendar.DAY_OF_WEEK)
            }

            "Täglich" -> true
            "Monatlich" -> {
                taskDate.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
            }

            else -> false
        }
    }

    private fun showTaskDialog(existingTask: DynamicTask? = null) {
        val context = requireContext()
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.wochenplan_dialog_add_task, null)

        val datePickerIcon: ImageView = dialogView.findViewById(R.id.datePickerIcon)
        val dateTextView: TextView = dialogView.findViewById(R.id.dateTextView)
        val taskDescription: EditText = dialogView.findViewById(R.id.taskDescription)
        val taskPriority: Spinner = dialogView.findViewById(R.id.taskPriority)
        val taskPoints: EditText = dialogView.findViewById(R.id.taskPoints)
        val taskAssigneeSpinner: Spinner = dialogView.findViewById(R.id.taskAssigneeSpinner)
        val repeatToggle: Switch = dialogView.findViewById(R.id.repeatingTaskToggle)
        val repeatFrequencyLayout: View = dialogView.findViewById(R.id.repeatingTaskDetails)
        val repeatFrequencySpinner: Spinner = dialogView.findViewById(R.id.repeatFrequencySpinner)
        val repeatDaySpinner: Spinner = dialogView.findViewById(R.id.repeatDaySpinner)

        val selectedDate = Calendar.getInstance()
        existingTask?.let {
            val parsedDate = dateFormat.parse(it.date)
            parsedDate?.let { date -> selectedDate.time = date }
            taskDescription.setText(it.description)
            taskPriority.setSelection(
                resources.getStringArray(R.array.prio_liste).indexOf(it.priority)
            )
            taskPoints.setText(it.points.toString())
            repeatToggle.isChecked = it.isRepeating
            repeatFrequencyLayout.visibility = if (it.isRepeating) View.VISIBLE else View.GONE
            repeatFrequencySpinner.setSelection(
                resources.getStringArray(R.array.repeat_frequency_options)
                    .indexOf(it.repeatFrequency ?: "")
            )
            repeatDaySpinner.setSelection(
                resources.getStringArray(R.array.days_of_week).indexOf(it.repeatDay ?: "")
            )

            // Setze den Assignee Spinner auf den aktuellen Wert des Tasks
            val assignee = it.assignee
            wochenplanViewModel.assignees.observe(viewLifecycleOwner) { assignees ->
                val assigneeAdapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    assignees.map { it.first })
                assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                taskAssigneeSpinner.adapter = assigneeAdapter

                // Wenn der Assignee in der Liste vorhanden ist, setze ihn als ausgewählt
                val assigneePosition = assignees.indexOfFirst { it.first == assignee }
                if (assigneePosition != -1) {
                    taskAssigneeSpinner.setSelection(assigneePosition)
                }
            }
        }

        dateTextView.text = dateFormat.format(selectedDate.time)

        // Lade die Mitglieder der WG und deren Nicknames auch beim Erstellen eines neuen Tasks
        if (existingTask == null) {
            wochenplanViewModel.loadAssignees { assignees ->
                val assigneeAdapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    assignees.map { it.first })
                assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                taskAssigneeSpinner.adapter = assigneeAdapter
            }
        }

        datePickerIcon.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    dateTextView.text = dateFormat.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        repeatToggle.setOnCheckedChangeListener { _, isChecked ->
            repeatFrequencyLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialogTitle = if (existingTask == null) R.string.add_task else R.string.edit_task
        val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val id = existingTask?.id ?: UUID.randomUUID().toString()
                val date = dateFormat.format(selectedDate.time)
                val description = taskDescription.text.toString().trim()
                val priorityPosition = taskPriority.selectedItemPosition
                val priority =
                    if (priorityPosition > 0) taskPriority.selectedItem.toString().trim() else ""

                var isValid = true

                if (description.isEmpty()) {
                    taskDescription.error = "Bitte eine Aufgabenbeschreibung eingeben."
                    isValid = false
                }

                if (priority.isEmpty()) {
                    val priorityTextView = taskPriority.selectedView as? TextView
                    priorityTextView?.setTextColor(ContextCompat.getColor(context, R.color.red))
                    priorityTextView?.text = "Bitte wählen Sie eine Priorität aus."
                    isValid = false
                }

                if (!isValid) {
                    return@setOnClickListener
                }

                val points = taskPoints.text.toString().toIntOrNull() ?: 0

                // Hole die E-Mail-Adresse des zugewiesenen Benutzers
                val selectedAssignee = taskAssigneeSpinner.selectedItem.toString()
                val assigneeEmail =
                    wochenplanViewModel.assignees.value?.find { it.first == selectedAssignee }?.second

                val newTask = assigneeEmail?.let { it1 ->
                    DynamicTask(
                        id = id,
                        date = date,
                        description = description,
                        priority = priority,
                        points = points,
                        assignee = selectedAssignee,
                        assigneeEmail = it1,  // E-Mail-Adresse speichern
                        avatar = R.drawable.logo,
                        isRepeating = repeatToggle.isChecked,
                        repeatFrequency = if (repeatToggle.isChecked) repeatFrequencySpinner.selectedItem.toString() else null,
                        repeatDay = if (repeatToggle.isChecked) repeatDaySpinner.selectedItem.toString() else null
                    )
                }

                if (existingTask == null) {
                    if (newTask != null) {
                        wochenplanViewModel.addTask(newTask)
                    }
                } else {
                    if (newTask != null) {
                        wochenplanViewModel.updateTask(newTask)
                    }
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}