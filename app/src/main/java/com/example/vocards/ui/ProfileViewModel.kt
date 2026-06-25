package com.example.vocards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocards.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    
    val userEmail: StateFlow<String?> = userPreferences.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiApiKey: StateFlow<String?> = userPreferences.aiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appLanguage: StateFlow<String> = userPreferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val darkMode: StateFlow<Boolean?> = userPreferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyGoal: StateFlow<Int> = userPreferences.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val defaultTaskSize: StateFlow<Int> = userPreferences.defaultTaskSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val isFirstTime: StateFlow<Boolean> = userPreferences.firstTimeUse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsEnabled: StateFlow<Boolean> = userPreferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = userPreferences.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun completeWelcome() {
        viewModelScope.launch { userPreferences.setFirstTimeUse(false) }
    }

    fun saveDarkMode(enabled: Boolean) {
        viewModelScope.launch { userPreferences.saveDarkMode(enabled) }
    }

    fun saveNotifications(enabled: Boolean) {
        viewModelScope.launch { userPreferences.saveNotificationsEnabled(enabled) }
    }

    fun saveSound(enabled: Boolean) {
        viewModelScope.launch { userPreferences.saveSoundEnabled(enabled) }
    }

    fun saveDailyGoal(goal: Int) {
        viewModelScope.launch { userPreferences.saveDailyGoal(goal) }
    }

    fun saveDefaultTaskSize(size: Int) {
        viewModelScope.launch { userPreferences.saveDefaultTaskSize(size) }
    }

    fun saveLanguage(lang: String) {
        viewModelScope.launch { userPreferences.saveLanguage(lang) }
    }

    fun saveEmail(email: String) {
        viewModelScope.launch { userPreferences.saveUserEmail(email) }
    }

    fun saveAiApiKey(key: String) {
        viewModelScope.launch { userPreferences.saveAiApiKey(key) }
    }
}
