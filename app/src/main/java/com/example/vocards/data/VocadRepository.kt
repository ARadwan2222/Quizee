package com.example.vocards.data

import kotlinx.coroutines.flow.Flow

class VocadRepository(private val vocadDao: VocadDao) {
    // Projects
    fun getAllProjectsStream(): Flow<List<Project>> = vocadDao.getAllProjects()
    suspend fun insertProject(project: Project): Long = vocadDao.insertProject(project)
    suspend fun updateProject(project: Project) = vocadDao.updateProject(project)
    suspend fun deleteProject(project: Project) = vocadDao.deleteProject(project)
    suspend fun getProjectById(id: Int): Project? = vocadDao.getProjectById(id)

    // Vocads
    fun getVocadsByProjectStream(projectId: Int): Flow<List<Vocad>> = vocadDao.getVocadsByProject(projectId)
    fun getAllVocadsStream(): Flow<List<Vocad>> = vocadDao.getAllVocads()
    suspend fun insertVocad(vocad: Vocad) = vocadDao.insertVocad(vocad)
    suspend fun deleteVocad(vocad: Vocad) = vocadDao.deleteVocad(vocad)
    suspend fun updateVocad(vocad: Vocad) = vocadDao.updateVocad(vocad)
    fun getVocadStream(id: Int): Flow<Vocad?> = vocadDao.getVocadById(id)
}
