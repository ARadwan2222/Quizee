package com.example.vocards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocards.data.Project
import com.example.vocards.data.Vocad
import com.example.vocards.data.VocadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class VocadViewModel(
    private val repository: VocadRepository
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Int?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    val projectsState: StateFlow<List<Project>> =
        repository.getAllProjectsStream()
            .map { list -> list.sortedBy { it.name } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val allVocadsState: StateFlow<List<Vocad>> =
        repository.getAllVocadsStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentVocadsState: StateFlow<List<Vocad>> =
        _selectedProjectId.flatMapLatest { projectId ->
            if (projectId == null) flowOf(emptyList())
            else combine(repository.getVocadsByProjectStream(projectId), _searchQuery) { vocads, query ->
                if (query.isBlank()) vocads
                else vocads.filter { 
                    it.word.contains(query, ignoreCase = true) || 
                    it.definition.contains(query, ignoreCase = true) 
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val projectProgress: StateFlow<Map<Int, Float>> =
        repository.getAllProjectsStream().flatMapLatest { projects ->
            val flows = projects.map { project ->
                repository.getVocadsByProjectStream(project.id).map { vocads ->
                    project.id to if (vocads.isEmpty()) 0f else vocads.count { it.isLearned }.toFloat() / vocads.size
                }
            }
            if (flows.isEmpty()) flowOf(emptyMap())
            else combine(flows) { it.toMap() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectProject(projectId: Int?) {
        _selectedProjectId.value = projectId
    }

    fun addProject(name: String, description: String? = null, fromLang: String = "English", toLang: String = "Arabic") {
        viewModelScope.launch {
            repository.insertProject(Project(name = name, description = description, fromLanguage = fromLang, toLanguage = toLang))
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addVocad(word: String, definition: String, example: String?) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            repository.insertVocad(Vocad(projectId = projectId, word = word, definition = definition, example = example))
        }
    }

    fun deleteVocad(vocad: Vocad) {
        viewModelScope.launch {
            repository.deleteVocad(vocad)
        }
    }

    fun importAsNewProject(rawText: String, projectName: String, fromLang: String = "English", toLang: String = "Arabic") {
        viewModelScope.launch {
            val projectId = repository.insertProject(Project(name = projectName, fromLanguage = fromLang, toLanguage = toLang)).toInt()
            importVocads(rawText, projectId)
        }
    }

    fun importVocads(rawText: String, projectId: Int) {
        viewModelScope.launch {
            val targetId = if (projectId == -1) {
                repository.insertProject(Project(name = "New Import")).toInt()
            } else projectId

            if (rawText.trim().startsWith("{") || rawText.trim().startsWith("[")) {
                try {
                    val array = if (rawText.trim().startsWith("{")) {
                        JSONObject(rawText).optJSONArray("words") ?: JSONArray()
                    } else {
                        JSONArray(rawText)
                    }
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        repository.insertVocad(Vocad(
                            projectId = targetId,
                            word = obj.optString("word", obj.optString("term")),
                            definition = obj.optString("definition", obj.optString("meaning")),
                            example = obj.optString("example").ifBlank { null }
                        ))
                    }
                    return@launch
                } catch (ignored: Exception) { }
            }

            val lines = rawText.lines().filter { it.isNotBlank() }
            for (line in lines) {
                val parts = when {
                    line.contains(" - ") -> line.split(" - ")
                    line.contains("\t") -> line.split("\t")
                    line.contains(",") -> line.split(",")
                    line.contains(":") -> line.split(":")
                    else -> listOf(line)
                }
                
                if (parts.size >= 2) {
                    repository.insertVocad(Vocad(
                        projectId = targetId, 
                        word = parts[0].trim(), 
                        definition = parts[1].trim(),
                        example = if (parts.size > 2) parts[2].trim() else null
                    ))
                }
            }
        }
    }

    fun registerUser(email: String, status: String) {
        viewModelScope.launch {
            // Mock registration call
            try {
                withContext(Dispatchers.IO) {
                    val url = URL("https://mock-registration-service.com/register?email=$email&status=$status")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.responseCode
                }
            } catch (e: Exception) { }
        }
    }

    fun importFromUrl(url: String, projectName: String, fromLang: String = "English", toLang: String = "Arabic") {
        viewModelScope.launch {
            try {
                _isGenerating.value = true
                val content = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }
                importAsNewProject(content, projectName, fromLang, toLang)
            } catch (e: Exception) {
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
