package com.example.vocards.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vocards.data.Vocad

@Composable
fun FlashcardScreen(
    vocads: List<Vocad>,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentIndex + 1} / ${vocads.size}",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onClose) {
                Text("Exit Study")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (vocads.isNotEmpty()) {
            Flashcard(
                vocad = vocads[currentIndex],
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            )
        } else {
            Text("No cards to study!")
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { if (currentIndex > 0) currentIndex-- },
                enabled = currentIndex > 0
            ) {
                Text("Previous")
            }
            Button(
                onClick = { if (currentIndex < vocads.size - 1) currentIndex++ },
                enabled = currentIndex < vocads.size - 1
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun Flashcard(vocad: Vocad, modifier: Modifier = Modifier) {
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardRotation"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { rotated = !rotated },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front
                Text(
                    text = vocad.word,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                // Back
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = vocad.definition,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    vocad.example?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Example: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    
    // Reset rotation when the card changes
    LaunchedEffect(vocad) {
        rotated = false
    }
}
