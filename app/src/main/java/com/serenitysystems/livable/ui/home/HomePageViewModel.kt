import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
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

    private fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                action(userToken)
            }
        }
    }

    fun joinWG(wgId: String, onError: (String) -> Unit) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userToken.email)

                userRef.get().addOnSuccessListener { wgDocument ->
                    if (wgDocument.exists()) {
                       // val updatedToken = userToken.copy(wgId = wgId, wgRole = "Wg-Mitglied")
                        userRef.update("wgId", wgId, "wgRole", "Wg-Mitglied")
                            /*

                            .addOnSuccessListener {
                                viewModelScope.launch(Dispatchers.IO) {
                                    userPreferences.saveUserToken(updatedToken)
                                }
                            }
                             */
                            .addOnFailureListener { exception ->
                                Log.e("HomePageViewModel", "Fehler beim Aktualisieren des UserToken: ${exception.message}")
                            }
                    } else {
                        onError("Die WG-ID existiert nicht.")
                    }
                }.addOnFailureListener { exception ->
                    Log.e("HomePageViewModel", "Fehler beim Laden der WG-DokumentationWG ID: ${exception.message}")
                }
            }
        }
    }


    fun leaveWG() {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userToken.email)
                userRef.update("wgId", "", "wgRole", "")
                    .addOnSuccessListener {
                        Log.d("HomePageViewModel", "Erfolgreich aus der WG verlassen.")
                        /*
                        val updatedToken = userToken.copy(wgId = "", wgRole = "")
                        viewModelScope.launch(Dispatchers.IO) {
                            userPreferences.saveUserToken(updatedToken)
                        }
                         */
                    }
                    .addOnFailureListener { exception ->
                        Log.e("HomePageViewModel", "Error leaving the WG: ${exception.message}")
                    }
            }
        }
    }

}
