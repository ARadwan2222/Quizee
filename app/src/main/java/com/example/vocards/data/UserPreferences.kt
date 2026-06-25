package com.example.vocards.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    companion object {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_MOBILE = stringPreferencesKey("user_mobile")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DARK_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("dark_mode")
        val DAILY_GOAL = androidx.datastore.preferences.core.intPreferencesKey("daily_goal")
        val DEFAULT_TASK_SIZE = androidx.datastore.preferences.core.intPreferencesKey("default_task_size")
        val FIRST_TIME_USE = androidx.datastore.preferences.core.booleanPreferencesKey("first_time_use")
        val USER_INTEREST = stringPreferencesKey("user_interest")
    }

    val userInterest: Flow<String?> = context.dataStore.data.map { it[USER_INTEREST] }

    suspend fun saveUserInterest(interest: String) {
        context.dataStore.edit { it[USER_INTEREST] = interest }
    }

    val firstTimeUse: Flow<Boolean> = context.dataStore.data.map { it[FIRST_TIME_USE] ?: true }

    suspend fun setFirstTimeUse(isFirst: Boolean) {
        context.dataStore.edit { it[FIRST_TIME_USE] = isFirst }
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map { it[DARK_MODE] }
    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[DAILY_GOAL] ?: 10 }
    val defaultTaskSize: Flow<Int> = context.dataStore.data.map { it[DEFAULT_TASK_SIZE] ?: 5 }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun saveDailyGoal(goal: Int) {
        context.dataStore.edit { it[DAILY_GOAL] = goal }
    }

    suspend fun saveDefaultTaskSize(size: Int) {
        context.dataStore.edit { it[DEFAULT_TASK_SIZE] = size }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE] ?: "en"
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = lang
        }
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    val userMobile: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_MOBILE]
    }

    val aiApiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AI_API_KEY]
    }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
        }
    }

    suspend fun saveUserMobile(mobile: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_MOBILE] = mobile
        }
    }

    suspend fun saveAiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_API_KEY] = key
        }
    }
}
