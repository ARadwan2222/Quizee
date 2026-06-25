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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vocards.data.Vocad
import com.example.vocards.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

val TopPopularLanguages = listOf(
    "English" to "en-US", "Spanish" to "es-ES", "Chinese" to "zh-CN", "Hindi" to "hi-IN",
    "Arabic" to "ar-SA", "Bengali" to "bn-BD", "Portuguese" to "pt-PT", "Russian" to "ru-RU",
    "Japanese" to "ja-JP", "Punjabi" to "pa-IN", "Marathi" to "mr-IN", "Telugu" to "te-IN",
    "Turkish" to "tr-TR", "Korean" to "ko-KR", "French" to "fr-FR", "German" to "de-DE",
    "Vietnamese" to "vi-VN", "Tamil" to "ta-IN", "Urdu" to "ur-PK", "Italian" to "it-IT"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCorrectionScreen(onBack: () -> Unit) {
    var recognizedText by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("en-US") }
    
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        recognizedText = data?.get(0) ?: "Could not recognize"
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
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Speak to check your pronunciation", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))
            
            Text("Target Language:", style = MaterialTheme.typography.labelSmall, color = QuizeeSecondaryText)
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(TopPopularLanguages) { (name, code) ->
                    FilterChip(
                        selected = selectedLang == code,
                        onClick = { selectedLang = code },
                        label = { Text(name, fontSize = 10.sp, color = if (selectedLang == code) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QuizeeAccent,
                            containerColor = Color(0xFF1E2235)
                        )
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            
            if (recognizedText.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = QuizeeCardBg)) {
                    Text(recognizedText, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))
            
            Surface(
                modifier = Modifier.size(100.dp),
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
                    Icon(Icons.Default.Mic, null, Modifier.size(40.dp), tint = Color.White)
                }
            }
            Text("Tap to Start", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp), color = QuizeeSecondaryText)
            Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassiveListeningScreen(viewModel: VocadViewModel, onSpeak: (String) -> Unit, onBack: () -> Unit) {
    val vocads by viewModel.allVocadsState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (isPlaying && vocads.isNotEmpty()) {
            val vocad = vocads[currentIndex]
            onSpeak(vocad.word)
            delay(2500)
            onSpeak(vocad.definition)
            delay(3500)
            
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
            Icon(Icons.Default.Headset, null, modifier = Modifier.size(80.dp), tint = QuizeeAccent.copy(alpha = 0.5f))
            Spacer(Modifier.height(24.dp))
            Text("Relax and listen to your vocabulary mission collection.", textAlign = TextAlign.Center, color = Color.White)
            
            Spacer(Modifier.weight(1f))
            
            if (vocads.isNotEmpty()) {
                val current = vocads[currentIndex]
                Text(current.word, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(current.definition, style = MaterialTheme.typography.headlineSmall, color = QuizeeSecondaryText, textAlign = TextAlign.Center)
                Text("${currentIndex + 1} / ${vocads.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 16.dp), color = QuizeeSecondaryText)
            } else {
                Text("No words found in your projects.", color = Color.White)
            }
            
            Spacer(Modifier.weight(1f))
            
            Button(
                onClick = { isPlaying = !isPlaying },
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Red else QuizeeAccent)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(40.dp), tint = Color.White)
            }
            Spacer(Modifier.height(48.dp))
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
