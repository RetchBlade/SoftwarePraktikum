package com.serenitysystems.livable.ui.register

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.serenitysystems.livable.R
import java.util.*

class RegistrierungFragment : Fragment() {

    private lateinit var editTextBirthdate: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registrierung, container, false)

        editTextBirthdate = view.findViewById(R.id.editTextBirthdate)
        editTextBirthdate.setOnClickListener {
            showDatePickerDialog()
        }

        return view
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Date selected
                val selectedDate = "$selectedDay.${selectedMonth + 1}.$selectedYear"
                editTextBirthdate.text = selectedDate
            },
            year, month, day
        )

        datePickerDialog.show()
    }
}
