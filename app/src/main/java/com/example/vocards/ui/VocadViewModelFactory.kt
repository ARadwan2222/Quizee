package com.example.vocards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vocards.data.UserPreferences
import com.example.vocards.data.VocadRepository

class VocadViewModelFactory(
    private val repository: VocadRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VocadViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                VocadViewModel(repository) as T
            }
            modelClass.isAssignableFrom(QuizViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                QuizViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                ProfileViewModel(userPreferences) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
