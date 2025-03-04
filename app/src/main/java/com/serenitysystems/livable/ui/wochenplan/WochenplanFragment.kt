package com.serenitysystems.livable.ui.wochenplan

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
    private var isTabSwitching = false // Flag für Animation

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

        binding.showPointsButton?.setOnClickListener {
            showPointsDialog()
        }


        return root


    }


    private fun setupTabs() {
        val weekTitle = binding.root.findViewById<TextView>(R.id.weekTitle)

        val tabIcons = listOf(
            R.drawable.ic_lastweek,
            R.drawable.ic_wochencalender,
            R.drawable.ic_nextweek
        )

        binding.tabLayout.apply {
            tabIcons.forEachIndexed { index, iconRes ->
                val tab = newTab().setCustomView(createTabWithIcon(iconRes, index == 0))
                addTab(tab)
            }

            // Standardmäßig den mittleren Tab auswählen
            getTabAt(1)?.select()
            weekTitle.text = "Aktueller Plan: Diese Woche"
            loadThisWeek()

            // Tab-Auswahl Listener
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    isTabSwitching = true // Setze Flag für Animation
                    val newText = when (tab.position) {
                        0 -> {
                            loadLastWeek()
                            "Rückblick: Letzte Woche"
                        }
                        1 -> {
                            loadThisWeek()
                            "Aktueller Plan: Diese Woche"
                        }
                        2 -> {
                            loadNextWeek()
                            "Vorschau: Kommende Woche"
                        }
                        else -> weekTitle.text.toString()
                    }
                    animateTitleChange(weekTitle, newText)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    // Helper function for smooth title animation
    private fun animateTitleChange(textView: TextView, newText: String) {
        textView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                textView.text = newText
                textView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
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
        val today = Calendar.getInstance()

        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val customView = tab?.customView
            val tabIcon = customView?.findViewById<ImageView>(R.id.tabIcon)
            val warningIcon = customView?.findViewById<ImageView>(R.id.warningIcon)

            // Hole die Aufgaben für letzte Woche und diese Woche
            val lastWeekTasks = wochenplanViewModel.lastWeekTasks.value ?: emptyList()
            val thisWeekTasks = wochenplanViewModel.thisWeekTasks.value ?: emptyList()

            // Prüfen, ob eine Aufgabe überfällig ist (Datum vor heute)
            val hasOverdueTasks = when (i) {
                0 -> lastWeekTasks.any { task -> isTaskOverdue(task, today) }
                1 -> thisWeekTasks.any { task -> isTaskOverdue(task, today) }
                else -> false // Nächste Woche ignorieren
            }

            when (i) {
                0 -> tabIcon?.setImageResource(R.drawable.ic_lastweek)
                1 -> tabIcon?.setImageResource(R.drawable.ic_wochencalender)
                2 -> tabIcon?.setImageResource(R.drawable.ic_nextweek)
            }

            // Visibility für das Warning-Icon steuern
            warningIcon?.visibility = if (hasOverdueTasks) View.VISIBLE else View.GONE
        }
    }


    private fun isTaskOverdue(task: DynamicTask, today: Calendar): Boolean {
        return try {
            val taskDate = Calendar.getInstance().apply {
                time = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY).parse(task.date)!!
            }
            !task.isDone && taskDate.before(today)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun displayTasksForDay(day: String) {
        val dayTasks = wochenplanViewModel.tasks.value?.filter {
            it.date == day || (it.repeating && shouldTaskBeShownToday(it, day))
        } ?: emptyList()
        displayTasks(dayTasks)
    }


    private fun displayTasks(tasks: List<DynamicTask>) {
        val layout = binding.taskLayout
        layout.removeAllViews()

        // Tasks nach Datum sortieren
        val sortedTasks = tasks.sortedBy { task ->
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY).parse(task.date)
        }

        // Tasks nach Datum gruppieren
        val groupedTasks = sortedTasks.groupBy { it.date }

        // Iteriere über jede Gruppe von Tasks basierend auf dem Datum
        for ((date, tasksForDate) in groupedTasks) {
            // Datum-Header hinzufügen
            val dateHeaderView = TextView(requireContext())
            dateHeaderView.text = formatDateHeader(date)
            dateHeaderView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.own_text_Farbe)
            )
            dateHeaderView.setPadding(16, 16, 16, 8)
            layout.addView(dateHeaderView)

            // Iteriere durch jeden Task in der Gruppe
            for (task in tasksForDate) {
                val taskView = if (task.assignee == "Unassigned") {
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.wochenplan_task_claimable, layout, false)
                } else {
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.wochenplan_task_item_new, layout, false)
                }

                val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
                val priorityTextView = taskView.findViewById<TextView>(R.id.taskPriority)
                val pointsTextView = taskView.findViewById<TextView>(R.id.taskPoints)
                val assigneeTextView = taskView.findViewById<TextView>(R.id.taskAsignee)
                val assigneeAvatarImageView = taskView.findViewById<ImageView>(R.id.taskAssigneeAvatar)
                val claimIcon = taskView.findViewById<ImageView>(R.id.claimIcon)
                val taskOptionsImageView = taskView.findViewById<ImageView>(R.id.taskOptions)

                // Task-Details setzen
                descriptionTextView.text = task.description
                priorityTextView.text = task.priority
                pointsTextView.text = "${task.points} Punkte"

                if (task.assignee == "Unassigned") {
                    // Task ist übernehmbar
                    claimIcon?.visibility = View.VISIBLE
                    assigneeTextView.visibility = View.GONE
                    assigneeAvatarImageView.visibility = View.GONE
                    claimIcon?.setOnClickListener {
                        assignTaskToCurrentUser(task)
                    }
                } else {
                    // Task ist bereits zugewiesen
                    claimIcon?.visibility = View.GONE
                    assigneeTextView.visibility = View.VISIBLE
                    assigneeAvatarImageView.visibility = View.VISIBLE
                    assigneeTextView.text = task.assignee

                    // Lade Avatar mit Glide
                    task.assigneeEmail?.let { email ->
                        db.collection("users").document(email)
                            .get()
                            .addOnSuccessListener { document ->
                                val profileImageUrl = document.getString("profileImageUrl")
                                if (!profileImageUrl.isNullOrEmpty()) {
                                    Glide.with(requireContext())
                                        .load(profileImageUrl)
                                        .circleCrop()
                                        .into(assigneeAvatarImageView)
                                } else {
                                    assigneeAvatarImageView.setImageResource(R.drawable.logo)
                                }
                            }
                    }
                }

                // Überfälligkeit des Tasks analysieren
                val taskDate = Calendar.getInstance()
                try {
                    taskDate.time = dateFormat.parse(task.date)
                    val today = Calendar.getInstance()
                    val midnight = today.clone() as Calendar
                    midnight.set(Calendar.HOUR_OF_DAY, 0)
                    midnight.set(Calendar.MINUTE, 0)
                    midnight.set(Calendar.SECOND, 0)
                    midnight.set(Calendar.MILLISECOND, 0)

                    if (!task.isDone && taskDate.before(midnight)) {
                        val overdueDays = ((today.timeInMillis - taskDate.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                        val overdueText = if (overdueDays == 1) {
                            "Überfällig: 1 Tag"
                        } else {
                            "Überfällig: $overdueDays Tage"
                        }
                        task.priority = overdueText
                        priorityTextView.text = overdueText
                        taskView.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.overdue_task_background
                        )
                        priorityTextView.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.priority_overdue)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Darstellung für abgeschlossene Tasks setzen
                if (task.isDone) {
                    taskView.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.wochenplan_rounded_corners
                    )
                    descriptionTextView.paintFlags =
                        descriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }

                // Farben basierend auf der Priorität setzen (wenn nicht überfällig)
                if (!task.priority.startsWith("Überfällig")) {
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

                // Optionen für den Task behandeln
                taskOptionsImageView.setOnClickListener {
                    showTaskOptions(task)
                }

                layout.addView(taskView)

                // Fade-In Animation nur bei Tabwechsel
                if (isTabSwitching) {
                    val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in)
                    taskView.startAnimation(animation)
                }
            }
        }
        isTabSwitching = false // Animation nur für Tabwechsel
    }







    private fun assignTaskToCurrentUser(task: DynamicTask) {
        wochenplanViewModel.fetchUserToken { token ->
            token?.let { userToken ->
                val userName = userToken.nickname
                val userEmail = userToken.email

                task.assignee = userName
                task.assigneeEmail = userEmail

                val taskView = findTaskView(task)
                taskView?.let {
                    val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.task_update)
                    it.startAnimation(animation)
                }

                wochenplanViewModel.updateTask(task)
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
        val options = mutableListOf<String>()

        val taskDate = Calendar.getInstance().apply {
            time = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY).parse(task.date)!!
        }

        val isLastWeekTask = wochenplanViewModel.isLastWeek(taskDate)

        if (!isLastWeekTask) {
            if (task.assignee != "Unassigned") {
                if (task.isDone) {
                    options.add("Nicht erledigt")
                } else {
                    options.add("Erledigt")
                    if (task.priority.startsWith("Überfällig")) {
                        options.add("Zuständigkeit abmelden") // Neue Option für überfällige Aufgaben
                    }
                }
            }
            options.add("Bearbeiten")
            options.add("Löschen")
        }

        if (options.isEmpty()) return

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.task_options)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Nicht erledigt" -> markTaskAsNotDone(task)
                    "Erledigt" -> markTaskAsDone(task)
                    "Bearbeiten" -> showTaskDialog(task)
                    "Löschen" -> deleteTask(task)
                    "Zuständigkeit abmelden" -> removeAssigneeFromTask(task) // Neue Funktion
                }
            }
            .create()

        dialog.show()
    }


    private fun removeAssigneeFromTask(task: DynamicTask) {
        if (task.assigneeEmail.isEmpty()) return // Kein gültiger Nutzer, nichts zu tun

        // Punkte des aktuellen Zuständigen abziehen, bevor er entfernt wird
        wochenplanViewModel.deductPointsBeforeUnassigning(task.assigneeEmail, task.points)

        val updatedTask = task.copy(assignee = "Unassigned", assigneeEmail = "")

        val taskView = findTaskView(task)
        taskView?.let {
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.task_update)
            it.startAnimation(animation)
        }

        // Aufgabe in Firestore aktualisieren
        wochenplanViewModel.updateTask(updatedTask)
        updateTabs()
    }





    private fun findTaskView(task: DynamicTask): View? {
        val layout = binding.taskLayout
        for (i in 0 until layout.childCount) {
            val childView = layout.getChildAt(i)
            val descriptionTextView = childView?.findViewById<TextView>(R.id.taskDescription)
            if (descriptionTextView != null && descriptionTextView.text == task.description) {
                return childView
            }
        }
        return null // Return null if no matching task view is found
    }



    private fun markTaskAsDone(task: DynamicTask) {
        val updatedTask = task.copy(isDone = true)

        // Find the corresponding task view
        val taskView = findTaskView(task)
        taskView?.let {
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.task_update)
            it.startAnimation(animation)
        }

        // Update the task in the ViewModel
        wochenplanViewModel.updateTask(updatedTask)
        updateTabs()
    }


    private fun markTaskAsNotDone(task: DynamicTask) {
        val updatedTask = task.copy(isDone = false)

        val taskView = findTaskView(task)
        taskView?.let {
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.task_update)
            it.startAnimation(animation)
        }

        wochenplanViewModel.updateTask(updatedTask)
        updateTabs()
    }


    private fun deleteTask(task: DynamicTask) {
        if (task.repeating) {
            AlertDialog.Builder(requireContext())
                .setTitle("Wiederkehrende Aufgabe löschen")
                .setMessage("Möchtest du nur diese Aufgabe oder alle zukünftigen löschen?")
                .setPositiveButton("Nur diese") { _, _ ->
                    wochenplanViewModel.deleteTask(task)
                }
                .setNegativeButton("Alle zukünftigen") { _, _ ->
                    wochenplanViewModel.deleteFutureRepeatingTasks(task)
                }
                .setNeutralButton("Abbrechen", null)
                .create()
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Aufgabe löschen")
                .setMessage("Möchtest du diese Aufgabe wirklich löschen?")
                .setPositiveButton("Löschen") { _, _ ->
                    wochenplanViewModel.deleteTask(task)
                }
                .setNegativeButton("Abbrechen", null)
                .create()
                .show()
        }
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
    @SuppressLint("MissingInflatedId")
    private fun showTaskDialog(existingTask: DynamicTask? = null) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.wochenplan_dialog_add_task, null)

        val datePickerIcon: ImageView = dialogView.findViewById(R.id.datePickerIcon)
        val dateTextView: TextView = dialogView.findViewById(R.id.dateTextView)
        val taskDescription: EditText = dialogView.findViewById(R.id.taskDescription)
        val taskPriority: Spinner = dialogView.findViewById(R.id.taskPriority)
        val taskPointsSpinner: Spinner = dialogView.findViewById(R.id.taskPointsSpinner)
        val taskAssigneeSpinner: Spinner = dialogView.findViewById(R.id.taskAssigneeSpinner)
        val assigneeToggle: Switch = dialogView.findViewById(R.id.asigneeToggle)
        val assigneeLabel: TextView = dialogView.findViewById(R.id.asigneeLabel)
        val ohneAssigneeLabel: TextView = dialogView.findViewById(R.id.ohneAssignee)
        val repeatToggle: Switch = dialogView.findViewById(R.id.repeatingTaskToggle)
        val repeatFrequencyLayout: View = dialogView.findViewById(R.id.repeatingTaskDetails)
        val repeatFrequencySpinner: Spinner = dialogView.findViewById(R.id.repeatFrequencySpinner)
        val repeatUntilContainer: LinearLayout = dialogView.findViewById(R.id.repeatUntilContainer)
        val repeatUntilDate: TextView = dialogView.findViewById(R.id.repeatUntilDate)
        val repeatUntilPickerIcon: ImageView = dialogView.findViewById(R.id.repeatUntilPickerIcon)

        val selectedDate = Calendar.getInstance()
        dateTextView.text = dateFormat.format(selectedDate.time)

        var selectedEndDate: Calendar? = null
        val openDatePicker = {
            val today = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedEndDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    repeatUntilDate.text = dateFormat.format(selectedEndDate!!.time)
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = today.timeInMillis
            datePickerDialog.show()
        }
        repeatUntilDate.setOnClickListener { openDatePicker() }
        repeatUntilPickerIcon.setOnClickListener { openDatePicker() }

        existingTask?.let {
            val parsedDate = dateFormat.parse(it.date)
            parsedDate?.let { date -> selectedDate.time = date }
            taskDescription.setText(it.description)
            taskPriority.setSelection(resources.getStringArray(R.array.prio_liste).indexOf(it.priority))

            val pointsArray = resources.getStringArray(R.array.points_options)
            val index = pointsArray.indexOf(it.points.toString())
            if (index >= 0) taskPointsSpinner.setSelection(index)

            repeatToggle.isChecked = it.repeating
            repeatFrequencyLayout.visibility = if (it.repeating) View.VISIBLE else View.GONE
            repeatFrequencySpinner.setSelection(
                resources.getStringArray(R.array.repeat_frequency_options).indexOf(it.repeatFrequency ?: "")
            )

            if (!it.repeatUntil.isNullOrEmpty()) {
                val parsedEndDate = dateFormat.parse(it.repeatUntil)
                parsedEndDate?.let { date ->
                    selectedEndDate = Calendar.getInstance().apply { time = date }
                    repeatUntilDate.text = dateFormat.format(selectedEndDate!!.time)
                }
            }

            assigneeToggle.isChecked = it.assignee == "Unassigned"
        }

        datePickerIcon.setOnClickListener {
            val today = Calendar.getInstance()
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

            // Setzt das minimale auswählbare Datum auf heute
            datePickerDialog.datePicker.minDate = today.timeInMillis
            datePickerDialog.show()
        }

        wochenplanViewModel.assignees.observe(viewLifecycleOwner) { assignees ->
            val assigneeAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                assignees.map { it.first }
            )
            assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            taskAssigneeSpinner.adapter = assigneeAdapter

            existingTask?.assignee?.let { assignee ->
                val position = assignees.indexOfFirst { it.first == assignee }
                if (position != -1) taskAssigneeSpinner.setSelection(position)
            }
        }

        repeatToggle.setOnCheckedChangeListener { _, isChecked ->
            repeatFrequencyLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            repeatUntilContainer.visibility = if (isChecked) View.VISIBLE else View.GONE

            if (isChecked) {
                assigneeToggle.isChecked = true
                assigneeToggle.isEnabled = false // Sperren des Toggles
            } else {
                assigneeToggle.isEnabled = true // Entsperren des Toggles
            }
        }


        assigneeToggle.setOnCheckedChangeListener { _, isChecked ->
            taskAssigneeSpinner.visibility = if (isChecked) View.GONE else View.VISIBLE
            assigneeLabel.visibility = if (isChecked) View.GONE else View.VISIBLE
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
                val priority = taskPriority.selectedItem.toString().trim()
                val points = taskPointsSpinner.selectedItem.toString().toInt()
                val repeatUntil = selectedEndDate?.let { dateFormat.format(it.time) }
                val selectedAssignee = if (assigneeToggle.isChecked) null else taskAssigneeSpinner.selectedItem.toString()
                val assigneeEmail = selectedAssignee?.let { name ->
                    wochenplanViewModel.assignees.value?.find { it.first == name }?.second
                }

                val newTask = DynamicTask(
                    id = id,
                    date = date,
                    description = description,
                    priority = priority,
                    points = points,
                    assignee = selectedAssignee ?: "Unassigned",
                    assigneeEmail = assigneeEmail ?: "",
                    repeating = repeatToggle.isChecked,
                    repeatFrequency = if (repeatToggle.isChecked) repeatFrequencySpinner.selectedItem.toString() else null,
                    repeatUntil = repeatUntil
                )

                if (existingTask == null) {
                    wochenplanViewModel.addTask(newTask)
                } else {
                    wochenplanViewModel.updateTask(newTask)
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }




    private fun showPointsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.wochenplan_dialog_punkteinsicht, null)
        val dialog = builder.setView(view).create()

        val monthDisplay: TextView = view.findViewById(R.id.monthDisplay)
        val pointsList: LinearLayout = view.findViewById(R.id.pointsList)
        val prevMonthButton: ImageView = view.findViewById(R.id.prevMonthButton)
        val nextMonthButton: ImageView = view.findViewById(R.id.nextMonthButton)
        val closeDialogButton: ImageView = view.findViewById(R.id.closeDialogButton)

        var currentMonth = Calendar.getInstance()

        fun updateMonthDisplay() {
            val dateFormat = SimpleDateFormat("MM-yyyy", Locale.GERMANY)
            monthDisplay.text = dateFormat.format(currentMonth.time)
        }

        fun calculateLevel(points: Int): Pair<Int, Int> {
            val pointsPerLevel = 100 // Alle 100 Punkte ein Levelaufstieg
            val level = (points / pointsPerLevel) + 1 // Level beginnt bei 1
            val progress = (points % pointsPerLevel) // Fortschritt innerhalb des Levels

            return Pair(level, progress)
        }


        fun loadPointsForMonth() {
            val monthIdentifier = SimpleDateFormat("yyyy-MM", Locale.GERMANY).format(currentMonth.time)
            pointsList.removeAllViews()

            wochenplanViewModel.fetchUserToken { token ->
                token?.let { userToken ->
                    val userEmail = userToken.email
                    FirebaseFirestore.getInstance().collection("users").document(userEmail)
                        .get()
                        .addOnSuccessListener { document ->
                            val wgId = document.getString("wgId")
                            if (wgId != null) {
                                FirebaseFirestore.getInstance().collection("WGs")
                                    .document(wgId)
                                    .collection("PunkteHistorie")
                                    .document(monthIdentifier)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val pointsData = doc.get("points") as? Map<String, Long>
                                        if (pointsData != null && pointsData.isNotEmpty()) {
                                            val sortedEntries = pointsData.entries.sortedByDescending { it.value }

                                            for ((email, points) in sortedEntries) {
                                                val (level, progress) = calculateLevel(points.toInt())

                                                val userView = LayoutInflater.from(requireContext())
                                                    .inflate(R.layout.wochenplan_user_points_item, pointsList, false)

                                                val usernameTextView = userView.findViewById<TextView>(R.id.usernameTextView)
                                                val levelTextView = userView.findViewById<TextView>(R.id.levelTextView)
                                                val pointsProgressBar = userView.findViewById<ProgressBar>(R.id.pointsProgressBar)
                                                val pointsTextView = userView.findViewById<TextView>(R.id.pointsTextView)

                                                // Suche den Nickname des Benutzers basierend auf der Email
                                                wochenplanViewModel.loadAssignees { assignees ->
                                                    val nickname = assignees.find { it.second == email }?.first ?: email
                                                    usernameTextView.text = nickname
                                                }

                                                levelTextView.text = "Level $level"
                                                pointsProgressBar.progress = (progress) // Fortschritt innerhalb des Levels
                                                pointsTextView.text = "$points P"

                                                pointsList.addView(userView)
                                            }
                                        } else {
                                            val emptyTextView = TextView(requireContext())
                                            emptyTextView.text = "Keine Punkte für diesen Monat"
                                            emptyTextView.textSize = 16f
                                            emptyTextView.setPadding(16, 8, 16, 8)
                                            pointsList.addView(emptyTextView)
                                        }
                                    }
                            }
                        }
                }
            }
        }


        prevMonthButton.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            loadPointsForMonth()
        }

        nextMonthButton.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            loadPointsForMonth()
        }

        closeDialogButton.setOnClickListener {
            dialog.dismiss()
        }

        updateMonthDisplay()
        loadPointsForMonth()
        dialog.show()
    }




    // stellt sicher dass bei jeder navigation in der app, der zuständiger wird observed und updated
    override fun onResume() {
        super.onResume()
        val layout = binding.taskLayout
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in)
        layout?.startAnimation(animation)

        wochenplanViewModel.loadAssignees {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}