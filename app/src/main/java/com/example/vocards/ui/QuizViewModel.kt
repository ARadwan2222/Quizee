package com.example.vocards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocards.data.Vocad
import com.example.vocards.data.VocadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class QuizMode {
    FLASHCARDS, WRITING, MEANING, REVERSE, LEARN, BLOCKS
}

data class QuizUiState(
    val projectId: Int? = null,
    val currentMode: QuizMode? = null,
    val vocads: List<Vocad> = emptyList(),
    val currentIndex: Int = 0,
    val options: List<String> = emptyList(),
    val isAnswerCorrect: Boolean? = null,
    val showResult: Boolean = false,
    val score: Int = 0,
    val wrongAnswers: MutableList<Vocad> = mutableListOf()
)

class QuizViewModel(private val repository: VocadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun startQuizDirect(mode: QuizMode, vocads: List<Vocad>) {
        if (vocads.isNotEmpty()) {
            _uiState.update { 
                QuizUiState(
                    projectId = -1,
                    currentMode = mode,
                    vocads = vocads,
                    currentIndex = 0,
                    score = 0,
                    showResult = false
                )
            }
            if (mode == QuizMode.MEANING || mode == QuizMode.REVERSE) generateOptions()
        }
    }

    fun startQuiz(mode: QuizMode, projectId: Int, limit: Int? = null, onlyMistakes: Boolean = false, resume: Boolean = false) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            
            if (resume && project.currentQuizMode != null && project.currentQuizMode == mode.name) {
                // Restore session
                val allVocads = repository.getVocadsByProjectStream(projectId).first()
                val idList = project.currentQuizWordIds?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                val sessionVocads = idList.mapNotNull { id -> allVocads.find { it.id == id } }
                
                if (sessionVocads.isNotEmpty()) {
                    _uiState.update { 
                        QuizUiState(
                            projectId = projectId,
                            currentMode = mode,
                            vocads = sessionVocads,
                            currentIndex = project.currentQuizWordIndex,
                            showResult = false
                        )
                    }
                    if (mode == QuizMode.MEANING || mode == QuizMode.REVERSE) generateOptions()
                    return@launch
                }
            }

            // Start new session
            var projectVocads = repository.getVocadsByProjectStream(projectId).first().shuffled()
            if (onlyMistakes) projectVocads = projectVocads.filter { it.mistakeCount > 0 }
            val finalVocads = if (limit != null && limit > 0) projectVocads.take(limit) else projectVocads

            if (finalVocads.isNotEmpty()) {
                val vocadIds = finalVocads.joinToString(",") { it.id.toString() }
                repository.updateProject(project.copy(
                    currentQuizMode = mode.name,
                    currentQuizWordIndex = 0,
                    currentQuizWordIds = vocadIds
                ))

                _uiState.update { 
                    QuizUiState(
                        projectId = projectId,
                        currentMode = mode,
                        vocads = finalVocads,
                        currentIndex = 0,
                        score = 0,
                        showResult = false
                    )
                }
                if (mode == QuizMode.MEANING || mode == QuizMode.REVERSE) generateOptions()
            }
        }
    }

    private fun generateOptions() {
        val currentState = _uiState.value
        val currentVocad = currentState.vocads.getOrNull(currentState.currentIndex) ?: return
        
        if (currentState.currentMode == QuizMode.MEANING) {
            val otherMeanings = currentState.vocads
                .filter { it.id != currentVocad.id }
                .map { it.definition }
                .distinct()
                .shuffled()
                .take(3)
            val options = (otherMeanings + currentVocad.definition).shuffled()
            _uiState.update { it.copy(options = options, isAnswerCorrect = null) }
        } else if (currentState.currentMode == QuizMode.REVERSE) {
            val otherWords = currentState.vocads
                .filter { it.id != currentVocad.id }
                .map { it.word }
                .distinct()
                .shuffled()
                .take(3)
            val options = (otherWords + currentVocad.word).shuffled()
            _uiState.update { it.copy(options = options, isAnswerCorrect = null) }
        }
    }

    fun submitWritingAnswer(answer: String) {
        val currentState = _uiState.value
        val currentVocad = currentState.vocads[currentState.currentIndex]
        val isCorrect = answer.trim().equals(currentVocad.word, ignoreCase = true)
        updateWordStats(currentVocad, isCorrect)
        
        _uiState.update { it.copy(
            isAnswerCorrect = isCorrect,
            score = if (isCorrect) it.score + 1 else it.score,
            wrongAnswers = if (!isCorrect) (it.wrongAnswers + currentVocad).toMutableList() else it.wrongAnswers
        ) }
    }

    fun submitChoiceAnswer(selected: String) {
        val currentState = _uiState.value
        val currentVocad = currentState.vocads[currentState.currentIndex]
        val isCorrect = if (currentState.currentMode == QuizMode.MEANING) {
            selected == currentVocad.definition
        } else {
            selected == currentVocad.word
        }
        updateWordStats(currentVocad, isCorrect)
        
        _uiState.update { it.copy(
            isAnswerCorrect = isCorrect,
            score = if (isCorrect) it.score + 1 else it.score,
            wrongAnswers = if (!isCorrect) (it.wrongAnswers + currentVocad).toMutableList() else it.wrongAnswers
        ) }
    }

    private fun updateWordStats(vocad: Vocad, isCorrect: Boolean) {
        viewModelScope.launch {
            val updated = if (isCorrect) {
                vocad.copy(successCount = vocad.successCount + 1, lastReviewed = System.currentTimeMillis())
            } else {
                vocad.copy(mistakeCount = vocad.mistakeCount + 1, lastReviewed = System.currentTimeMillis())
            }
            repository.updateVocad(updated)
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentIndex + 1
        
        viewModelScope.launch {
            currentState.projectId?.let { id ->
                repository.getProjectById(id)?.let { project ->
                    repository.updateProject(project.copy(currentQuizWordIndex = nextIndex))
                }
            }
        }

        if (nextIndex < currentState.vocads.size) {
            _uiState.update { it.copy(
                currentIndex = nextIndex,
                isAnswerCorrect = null
            ) }
            if (currentState.currentMode == QuizMode.MEANING || currentState.currentMode == QuizMode.REVERSE) {
                generateOptions()
            }
        } else {
            // Quiz finished, update project stats
            viewModelScope.launch {
                currentState.projectId?.let { id ->
                    repository.getProjectById(id)?.let { project ->
                        val now = System.currentTimeMillis()
                        val lastDate = project.lastStudyDate
                        
                        val isSameDay = android.text.format.DateUtils.isToday(lastDate)
                        val isYesterday = android.text.format.DateUtils.isToday(lastDate + 24 * 60 * 60 * 1000)
                        
                        val newStreak = when {
                            isSameDay -> project.streak
                            isYesterday -> project.streak + 1
                            else -> 1
                        }
                        
                        repository.updateProject(project.copy(
                            currentQuizMode = null, 
                            currentQuizWordIndex = 0, 
                            currentQuizWordIds = null,
                            streak = newStreak,
                            lastStudyDate = now
                        ))
                    }
                }
            }
            _uiState.update { it.copy(showResult = true) }
        }
    }
    
    fun markAsLearned() {
        val currentState = _uiState.value
        val currentVocad = currentState.vocads[currentState.currentIndex]
        viewModelScope.launch {
            repository.updateVocad(currentVocad.copy(isLearned = true))
        }
        nextQuestion()
    }
    
    fun resetQuiz() {
        _uiState.value = QuizUiState()
    }
}
