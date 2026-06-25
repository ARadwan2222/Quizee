package com.example.vocards.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

val PopularLanguagesList = listOf(
    "Arabic" to "ar", "Bengali" to "bn", "Chinese" to "zh", "English" to "en",
    "French" to "fr", "German" to "de", "Hindi" to "hi", "Italian" to "it",
    "Japanese" to "ja", "Korean" to "ko", "Marathi" to "mr", "Portuguese" to "pt",
    "Punjabi" to "pa", "Russian" to "ru", "Spanish" to "es", "Tamil" to "ta",
    "Telugu" to "te", "Turkish" to "tr", "Urdu" to "ur", "Vietnamese" to "vi",
    "Dutch" to "nl", "Greek" to "el", "Hebrew" to "he", "Indonesian" to "id",
    "Malay" to "ms", "Persian" to "fa", "Polish" to "pl", "Swedish" to "sv",
    "Thai" to "th", "Ukrainian" to "uk"
).sortedBy { it.first }

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    val fromLanguage: String = "en",
    val toLanguage: String = "en",
    val createdAt: Long = System.currentTimeMillis(),
    val currentQuizMode: String? = null,
    val currentQuizWordIndex: Int = 0,
    val currentQuizWordIds: String? = null,
    val streak: Int = 0,
    val lastStudyDate: Long = 0,
    val dailyGoal: Int = 10
)

@Entity(
    tableName = "vocads",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Vocad(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val word: String,
    val definition: String,
    val example: String? = null,
    val isLearned: Boolean = false,
    val level: Int = 0,
    val mistakeCount: Int = 0,
    val successCount: Int = 0,
    val lastReviewed: Long = 0
)
