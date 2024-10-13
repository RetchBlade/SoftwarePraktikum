package com.serenitysystems.livable.ui.login.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore-Extension für den Context
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    private val USER_TOKEN_KEY = stringPreferencesKey("user_token")
    private val gson = Gson()

    // Speichert den UserToken als JSON
    suspend fun saveUserToken(userToken: UserToken) {
        val userTokenString = gson.toJson(userToken)
        context.dataStore.edit { preferences ->
            preferences[USER_TOKEN_KEY] = userTokenString
        }
    }

    // Holt den UserToken und konvertiert ihn zurück
    val userToken: Flow<UserToken?> = context.dataStore.data
        .map { preferences ->
            val userTokenString = preferences[USER_TOKEN_KEY]
            userTokenString?.let { gson.fromJson(it, UserToken::class.java) }
        }

    // Löschen des Tokens
    suspend fun clearUserToken() {
        context.dataStore.edit { preferences ->
            preferences.clear() // Löscht alle gespeicherten Daten im DataStore
        }
    }
}
