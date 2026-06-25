package com.example.vocards.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vocards.data.Vocad
import com.example.vocards.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(viewModel: QuizViewModel, onNavigateBack: () -> Unit, onSpeak: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = QuizeeDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("${uiState.currentMode?.name} Quiz", color = Color.White) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (uiState.showResult) {
                QuizResult(score = uiState.score, total = uiState.vocads.size, onFinish = onNavigateBack)
            } else if (uiState.vocads.isNotEmpty()) {
                val current = uiState.vocads[uiState.currentIndex]
                
                if (uiState.currentMode == QuizMode.BLOCKS) {
                    BlocksQuiz(
                        vocads = uiState.vocads,
                        onFinish = { onNavigateBack() }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LinearProgressIndicator(progress = { (uiState.currentIndex + 1).toFloat() / uiState.vocads.size }, Modifier.fillMaxWidth())
                        Text("Question ${uiState.currentIndex + 1} of ${uiState.vocads.size}", style = MaterialTheme.typography.labelLarge, color = Color.White)

                        when (uiState.currentMode) {
                            QuizMode.FLASHCARDS -> FlashcardQuiz(current, { viewModel.nextQuestion() }, onSpeak)
                            QuizMode.WRITING -> WritingQuiz(current, uiState.isAnswerCorrect, { viewModel.submitWritingAnswer(it) }, { viewModel.nextQuestion() }, onSpeak)
                            QuizMode.MEANING, QuizMode.REVERSE -> ChoiceQuiz(
                                title = if (uiState.currentMode == QuizMode.MEANING) current.word else current.definition,
                                options = uiState.options,
                                isCorrect = uiState.isAnswerCorrect,
                                correctAnswer = if (uiState.currentMode == QuizMode.MEANING) current.definition else current.word,
                                onAnswer = { viewModel.submitChoiceAnswer(it) },
                                onNext = { viewModel.nextQuestion() },
                                onSpeak = { if (uiState.currentMode == QuizMode.MEANING) onSpeak(current.word) }
                            )
                            QuizMode.LEARN -> LearnMode(
                                vocad = current,
                                onLearned = { viewModel.markAsLearned() },
                                onSkip = { viewModel.nextQuestion() },
                                onSpeak = onSpeak
                            )
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlocksQuiz(vocads: List<Vocad>, onFinish: () -> Unit) {
    var selectedWord by remember { mutableStateOf<Vocad?>(null) }
    var selectedDefinition by remember { mutableStateOf<Vocad?>(null) }
    var matchedIds by remember { mutableStateOf(setOf<Int>()) }
    
    val words = remember(vocads) { vocads.shuffled() }
    val definitions = remember(vocads) { vocads.shuffled() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Match the pairs!", style = MaterialTheme.typography.titleLarge, color = Color.White)
        
        Row(Modifier.weight(1f)) {
            // Words Column
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(words) { vocad ->
                    val isMatched = matchedIds.contains(vocad.id)
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isMatched) 0.3f else 1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedWord == vocad) QuizeeAccent else QuizeeCardBg
                        ),
                        onClick = { if (!isMatched) selectedWord = vocad }
                    ) {
                        Text(vocad.word, Modifier.padding(16.dp), color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            // Definitions Column
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(definitions) { vocad ->
                    val isMatched = matchedIds.contains(vocad.id)
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isMatched) 0.3f else 1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDefinition == vocad) QuizeeAccent else QuizeeCardBg
                        ),
                        onClick = { if (!isMatched) selectedDefinition = vocad }
                    ) {
                        Text(vocad.definition, Modifier.padding(16.dp), color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        LaunchedEffect(selectedWord, selectedDefinition) {
            if (selectedWord != null && selectedDefinition != null) {
                if (selectedWord!!.id == selectedDefinition!!.id) {
                    matchedIds = matchedIds + selectedWord!!.id
                }
                delay(300)
                selectedWord = null
                selectedDefinition = null
            }
        }

        if (matchedIds.size == vocads.size) {
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text("Mission Accomplished! Done")
            }
        }
    }
}

@Composable
fun ChoiceQuiz(title: String, options: List<String>, isCorrect: Boolean?, correctAnswer: String, onAnswer: (String) -> Unit, onNext: () -> Unit, onSpeak: () -> Unit) {
    var selected by remember { mutableStateOf("") }
    LaunchedEffect(title) { selected = "" }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White)
            IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = QuizeeAccent) }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                val color = when {
                    isCorrect != null && option == correctAnswer -> Color.Green.copy(0.2f)
                    isCorrect == false && option == selected -> Color.Red.copy(0.2f)
                    else -> QuizeeCardBg
                }
                Card(onClick = { if (isCorrect == null) { selected = option; onAnswer(option) } }, enabled = isCorrect == null, colors = CardDefaults.cardColors(containerColor = color)) {
                    Text(option, Modifier.padding(16.dp).fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }

        if (isCorrect != null) {
            AnswerFeedback(isCorrect, correctAnswer)
            Button(onClick = onNext, Modifier.fillMaxWidth()) { Text("Next") }
        }
    }
}

@Composable
fun LearnMode(vocad: Vocad, onLearned: () -> Unit, onSkip: () -> Unit, onSpeak: (String) -> Unit) {
    var isSwapped by remember { mutableStateOf(false) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (!isSwapped) vocad.word else vocad.definition, 
                style = MaterialTheme.typography.displayMedium, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(onClick = { onSpeak(if (!isSwapped) vocad.word else vocad.definition) }) { 
                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(48.dp), tint = QuizeeAccent) 
            }
        }
        
        // Direction Switcher
        IconButton(
            onClick = { isSwapped = !isSwapped },
            modifier = Modifier.background(QuizeeCardBg, CircleShape)
        ) {
            Icon(Icons.Default.SyncAlt, "Switch Language Direction", Modifier.size(24.dp), tint = Color.White)
        }

        Text(
            if (!isSwapped) vocad.definition else vocad.word, 
            style = MaterialTheme.typography.headlineSmall, 
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        vocad.example?.let { 
            Text("\"$it\"", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f)) 
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onLearned, Modifier.fillMaxWidth()) { Text("I learned this word") }
        TextButton(onClick = onSkip) { Text("Skip for now", color = Color.White) }
    }
}

@Composable
fun FlashcardQuiz(vocad: Vocad, onNext: () -> Unit, onSpeak: (String) -> Unit) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardRotation"
    )

    LaunchedEffect(vocad) { flipped = false }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = QuizeeCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (rotation <= 90f) {
                    // Front
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = vocad.word,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.padding(24.dp)
                        )
                        IconButton(onClick = { onSpeak(vocad.word) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(32.dp), tint = QuizeeAccent)
                        }
                        Text(
                            "Tap to reveal definition",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    // Back
                    Column(
                        modifier = Modifier
                            .graphicsLayer { rotationY = 180f }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = vocad.definition,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        vocad.example?.let {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "\"$it\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Next Word")
        }
    }
}

@Composable
fun WritingQuiz(vocad: Vocad, isCorrect: Boolean?, onAnswer: (String) -> Unit, onNext: () -> Unit, onSpeak: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    LaunchedEffect(vocad) { input = "" }
    Text(vocad.definition, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color.White)
    OutlinedTextField(
        value = input, onValueChange = { if (isCorrect == null) input = it }, 
        label = { Text("Word", color = Color.White) }, 
        modifier = Modifier.fillMaxWidth(), enabled = isCorrect == null,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White)
    )
    if (isCorrect == null) Button(onClick = { onAnswer(input) }, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Check") }
    else {
        AnswerFeedback(isCorrect, vocad.word)
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
    }
}

@Composable
fun AnswerFeedback(isCorrect: Boolean, correct: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (isCorrect) Color.Green else Color.Red)
        Spacer(Modifier.width(8.dp))
        Text(if (isCorrect) "Correct!" else "Wrong! Correct: $correct", color = if (isCorrect) Color.Green else Color.Red, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuizResult(score: Int, total: Int, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Quiz Finished!", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("Score: $score / $total", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Button(onClick = onFinish, modifier = Modifier.padding(top = 32.dp)) { Text("Done") }
    }
}
