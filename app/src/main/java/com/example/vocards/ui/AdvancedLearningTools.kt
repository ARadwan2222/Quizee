package com.example.vocards.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vocards.fetchDefinition
import com.example.vocards.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val TopPopularLanguages = listOf(
    "English" to "en-US", "Spanish" to "es-ES", "Chinese" to "zh-CN", "Hindi" to "hi-IN",
    "Arabic" to "ar-SA", "Bengali" to "bn-BD", "Portuguese" to "pt-PT", "Russian" to "ru-RU",
    "Japanese" to "ja-JP", "Punjabi" to "pa-IN", "Marathi" to "mr-IN", "Telugu" to "te-IN",
    "Turkish" to "tr-TR", "Korean" to "ko-KR", "French" to "fr-FR", "German" to "de-DE",
    "Vietnamese" to "vi-VN", "Tamil" to "ta-IN", "Urdu" to "ur-PK", "Italian" to "it-IT"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCorrectionScreen(
    viewModel: VocadViewModel,
    onSpeak: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    val vocads by viewModel.allVocadsState.collectAsState()
    val projects by viewModel.projectsState.collectAsState()
    
    var recognizedText by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("en-US") }
    var onlineDefinition by remember { mutableStateOf<String?>(null) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = data?.get(0) ?: ""
        recognizedText = text
        onlineDefinition = null
        if (text.isNotBlank()) {
            onSpeak(text, selectedLang)
            val matchingInDb = vocads.any { it.word.equals(text, ignoreCase = true) || it.definition.equals(text, ignoreCase = true) }
            if (!matchingInDb) {
                scope.launch {
                    isSearchingOnline = true
                    val res = fetchDefinition(text)
                    onlineDefinition = res?.first
                    isSearchingOnline = false
                }
            }
        }
    }

    // Offline search in user's saved vocads
    val matchingVocads = remember(recognizedText, vocads) {
        if (recognizedText.isBlank()) emptyList()
        else vocads.filter { 
            it.word.contains(recognizedText, ignoreCase = true) || 
            it.definition.contains(recognizedText, ignoreCase = true) ||
            recognizedText.contains(it.word, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = QuizeeDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Voice Correction", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Speak to check your pronunciation", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(12.dp))
            
            Text("Target Language:", style = MaterialTheme.typography.labelSmall, color = QuizeeSecondaryText)
            Spacer(Modifier.height(4.dp))
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TopPopularLanguages) { (name, code) ->
                    FilterChip(
                        selected = selectedLang == code,
                        onClick = { selectedLang = code },
                        label = { Text(name, fontSize = 11.sp, color = if (selectedLang == code) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QuizeeAccent,
                            containerColor = Color(0xFF1E2235)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (recognizedText.isNotBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = QuizeeCardBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, QuizeeAccent.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Speech Recognized", style = MaterialTheme.typography.labelSmall, color = Color.Green, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    recognizedText, 
                                    style = MaterialTheme.typography.headlineSmall, 
                                    fontWeight = FontWeight.Bold, 
                                    textAlign = TextAlign.Center, 
                                    color = Color.White
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = { onSpeak(recognizedText, selectedLang) },
                                    colors = ButtonDefaults.buttonColors(containerColor = QuizeeAccent),
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Listen Correct Pronunciation", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (matchingVocads.isNotEmpty()) {
                        item {
                            Text(
                                "Matches in Offline Collection (${matchingVocads.size})", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        items(matchingVocads) { vocad ->
                            val proj = projects.find { it.id == vocad.projectId }
                            val projName = proj?.name ?: "Collection"
                            val wordLang = proj?.fromLanguage ?: selectedLang
                            val defLang = proj?.toLanguage ?: selectedLang

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(vocad.word, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { onSpeak(vocad.word, wordLang) }) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = QuizeeAccent)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text("Meaning: ${vocad.definition}", color = Color.White.copy(0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { onSpeak(vocad.definition, defLang) }) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = QuizeeAccent.copy(alpha = 0.7f))
                                        }
                                    }
                                    vocad.example?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Example: \"$it\"", color = QuizeeSecondaryText, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("Collection: $projName", color = QuizeeAccent, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    if (isSearchingOnline) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(Modifier.size(20.dp), color = QuizeeAccent)
                                            Spacer(Modifier.width(12.dp))
                                            Text("Searching online definition...", color = Color.White)
                                        }
                                    } else if (onlineDefinition != null) {
                                        Text("Meaning (Online):", color = QuizeeSecondaryText, fontSize = 12.sp)
                                        Text(onlineDefinition!!, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    } else {
                                        Text("Meaning:", color = QuizeeSecondaryText, fontSize = 12.sp)
                                        Text("Word not found in offline collections.", color = Color.White.copy(0.7f), fontSize = 14.sp)
                                        Text("💡 Offline lookup searches all saved project words automatically.", color = QuizeeSecondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Press the mic button below and speak a word.", color = QuizeeSecondaryText, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = QuizeeAccent,
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLang)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak clearly...")
                    }
                    speechLauncher.launch(intent)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, null, Modifier.size(36.dp), tint = Color.White)
                }
            }
            Text("Tap Mic to Start Speaking", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp), color = QuizeeSecondaryText)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassiveListeningScreen(
    viewModel: VocadViewModel, 
    onSpeak: (String, String?) -> Unit, 
    onBack: () -> Unit
) {
    val vocads by viewModel.allVocadsState.collectAsState()
    val projects by viewModel.projectsState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }

    val currentVocad = if (vocads.isNotEmpty() && currentIndex < vocads.size) vocads[currentIndex] else null
    val currentProject = remember(currentVocad, projects) {
        projects.find { it.id == currentVocad?.projectId }
    }

    LaunchedEffect(isPlaying, currentIndex, vocads, projects) {
        if (isPlaying && vocads.isNotEmpty()) {
            val vocad = vocads[currentIndex]
            val project = projects.find { it.id == vocad.projectId }
            val wordLang = project?.fromLanguage ?: "English"
            val defLang = project?.toLanguage ?: "Arabic"

            // Speak word in source language
            onSpeak(vocad.word, wordLang)
            delay(2500)
            if (!isPlaying) return@LaunchedEffect
            
            // Speak definition in target language
            onSpeak(vocad.definition, defLang)
            delay(3500)
            if (!isPlaying) return@LaunchedEffect
            
            if (currentIndex < vocads.size - 1) {
                currentIndex++
            } else {
                currentIndex = 0 // Loop back
            }
        }
    }

    Scaffold(
        containerColor = QuizeeDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Passive Listening", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Headset, null, modifier = Modifier.size(70.dp), tint = QuizeeAccent.copy(alpha = 0.8f))
            Spacer(Modifier.height(16.dp))
            Text("Relax and listen to your vocabulary in both languages.", textAlign = TextAlign.Center, color = Color.White)
            
            if (currentProject != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF1E2235),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${currentProject.name} (${currentProject.fromLanguage} \u2192 ${currentProject.toLanguage})",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = QuizeeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            
            if (currentVocad != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = QuizeeCardBg)
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currentVocad.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(currentVocad.definition, style = MaterialTheme.typography.headlineSmall, color = QuizeeSecondaryText, textAlign = TextAlign.Center)
                        currentVocad.example?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("\"$it\"", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Color.White.copy(0.6f), textAlign = TextAlign.Center)
                        }
                    }
                }
                Text("${currentIndex + 1} / ${vocads.size}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp), color = QuizeeSecondaryText)
            } else {
                Text("No words found in your projects.", color = Color.White)
            }
            
            Spacer(Modifier.weight(1f))
            
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- else if (vocads.isNotEmpty()) currentIndex = vocads.size - 1 },
                    enabled = vocads.isNotEmpty()
                ) {
                    Icon(Icons.Default.SkipPrevious, null, Modifier.size(36.dp), tint = Color.White)
                }
                
                Button(
                    onClick = { isPlaying = !isPlaying },
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Red else QuizeeAccent)
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(36.dp), tint = Color.White)
                }
                
                IconButton(
                    onClick = { if (currentIndex < vocads.size - 1) currentIndex++ else currentIndex = 0 },
                    enabled = vocads.isNotEmpty()
                ) {
                    Icon(Icons.Default.SkipNext, null, Modifier.size(36.dp), tint = Color.White)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenGlanceScreen(viewModel: VocadViewModel, onBack: () -> Unit) {
    val vocads by viewModel.allVocadsState.collectAsState()
    val randomVocad = remember(vocads) { if (vocads.isNotEmpty()) vocads.random() else null }

    Scaffold(
        containerColor = QuizeeDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Lock Screen Glance", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Study words every time you open Quizee", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.White)
            
            Spacer(Modifier.height(64.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = QuizeeCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (randomVocad != null) {
                        Text(randomVocad.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(randomVocad.definition, style = MaterialTheme.typography.titleMedium, color = QuizeeSecondaryText)
                    } else {
                        Text("No words yet", color = Color.White)
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            Text("This feature shows your project words in high visibility white text for better focus.", 
                style = MaterialTheme.typography.bodySmall, color = QuizeeSecondaryText, textAlign = TextAlign.Center)

            Spacer(Modifier.height(32.dp))
        }
    }
}
