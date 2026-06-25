package com.example.vocards

import android.app.Application
import com.example.vocards.data.VocadDatabase
import com.example.vocards.data.VocadRepository
import com.example.vocards.data.UserPreferences

class VocadsApplication : Application() {
    val database: VocadDatabase by lazy { VocadDatabase.getDatabase(this) }
    val repository: VocadRepository by lazy { VocadRepository(database.vocadDao()) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase initialization failed
        }
    }
}
