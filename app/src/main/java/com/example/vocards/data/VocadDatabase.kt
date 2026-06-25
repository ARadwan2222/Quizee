package com.example.vocards.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Project::class, Vocad::class], version = 3, exportSchema = false)
abstract class VocadDatabase : RoomDatabase() {
    abstract fun vocadDao(): VocadDao

    companion object {
        @Volatile
        private var Instance: VocadDatabase? = null

        fun getDatabase(context: Context): VocadDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, VocadDatabase::class.java, "vocad_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
