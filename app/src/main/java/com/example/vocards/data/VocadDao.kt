package com.example.vocards.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VocadDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Insert
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    // Vocads
    @Query("SELECT * FROM vocads WHERE projectId = :projectId ORDER BY word ASC")
    fun getVocadsByProject(projectId: Int): Flow<List<Vocad>>

    @Query("SELECT * FROM vocads ORDER BY lastReviewed DESC")
    fun getAllVocads(): Flow<List<Vocad>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocad(vocad: Vocad)

    @Update
    suspend fun updateVocad(vocad: Vocad)

    @Delete
    suspend fun deleteVocad(vocad: Vocad)
    
    @Query("SELECT * FROM vocads WHERE id = :id")
    fun getVocadById(id: Int): Flow<Vocad>
}
