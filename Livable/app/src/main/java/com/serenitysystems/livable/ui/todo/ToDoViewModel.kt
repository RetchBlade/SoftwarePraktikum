package com.serenitysystems.livable.ui.todo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ToDoViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Hier wird die ToDo list implementiert"
    }
    val text: LiveData<String> = _text
}