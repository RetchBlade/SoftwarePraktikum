import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomePageViewModel(application: Application) : AndroidViewModel(application) {

    private val _userNickname = MutableLiveData<String?>()
    val userNickname: LiveData<String?> = _userNickname
    private val _userPic = MutableLiveData<String?>()
    val userPic: LiveData<String?> = _userPic // Hier auf _userPic korrigiert

    private val userPreferences: UserPreferences = UserPreferences(application)

    init {
        fetchUserNickname()
        fetchUserPicture() // Füge diesen Aufruf hinzu, um das Bild zu laden
    }

    private fun fetchUserNickname() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                _userNickname.postValue(userToken?.nickname)
            }
        }
    }

    private fun fetchUserPicture() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                _userPic.postValue(userToken?.profileImageUrl)
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
                        val updatedToken = userToken.copy(wgId = wgId, wgRole = "Wg-Mitglied")
                        userRef.update("wgId", wgId, "wgRole", "Wg-Mitglied")
                            .addOnSuccessListener {
                                Log.d("HomePageViewModel", "Erfolgreich die Wg beigetreten.")
                            }
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
                    }
                    .addOnFailureListener { exception ->
                        Log.e("HomePageViewModel", "Error leaving the WG: ${exception.message}")
                    }
            }
        }
    }

    fun fetchUserWGInfo(onSuccess: (String?, String?) -> Unit, onError: (String) -> Unit) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userToken.email)

                userRef.get().addOnSuccessListener { wgDocument ->
                    if (wgDocument.exists()) {
                        val wgId = wgDocument.getString("wgId")
                        val wgRole = wgDocument.getString("wgRole")
                        onSuccess(wgId, wgRole)
                    } else {
                        onError("Benutzerdaten nicht gefunden.")
                    }
                }.addOnFailureListener { exception ->
                    onError("Fehler beim Laden der Benutzerdaten: ${exception.message}")
                }
            } ?: onError("Benutzer-Token ist null.")
        }
    }
}
