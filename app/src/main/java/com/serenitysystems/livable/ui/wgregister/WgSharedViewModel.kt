package com.serenitysystems.livable.ui.userprofil

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WgSharedViewModel : ViewModel() {
    private val _wgAddress = MutableLiveData<String>()
    val wgAddress: LiveData<String> get() = _wgAddress

    private val _roomCount = MutableLiveData<String>()
    val roomCount: LiveData<String> get() = _roomCount

    private val _wgSize = MutableLiveData<String>()
    val wgSize: LiveData<String> get() = _wgSize

    fun setWgDetails(address: String, rooms: String, size: String) {
        _wgAddress.value = address
        _roomCount.value = rooms
        _wgSize.value = size
    }
}