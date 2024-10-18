package com.serenitysystems.livable.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.serenitysystems.livable.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomePageViewModel(application: Application) : AndroidViewModel(application) {

    private val _userNickname = MutableLiveData<String?>()
    val userNickname: LiveData<String?> = _userNickname

    private val userPreferences: UserPreferences = UserPreferences(application)

    init {
        fetchUserNickname()
    }

    private fun fetchUserNickname() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                _userNickname.postValue(userToken?.nickname)
            }
        }
    }

    fun joinWG(wgId: String) {
        // Logic to join WG
    }

    fun leaveWG() {
        // Logic to leave WG
    }
}
