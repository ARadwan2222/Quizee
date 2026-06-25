package com.example.vocards.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onContinue: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Safely get Firebase Instance
    val auth: FirebaseAuth? = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) } // 0: Intro, 1: Auth, 2: Verification Wait
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                errorMessage = "Google Login Error: Missing ID Token. Ensure SHA-1 is added."
                isLoading = false
            } else {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth?.signInWithCredential(credential)?.addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        onContinue(account.email ?: "User")
                    } else {
                        errorMessage = authTask.exception?.message
                        isLoading = false
                    }
                }
            }
        } catch (e: ApiException) {
            errorMessage = "Google sign in failed: ${e.message}"
            isLoading = false
        }
    }

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFFE1BEE7), Color(0xFF9C27B0)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            when (step) {
                0 -> {
                    Box(
                        modifier = Modifier.size(130.dp).scale(pulseScale).clip(RoundedCornerShape(40.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Q", fontSize = 85.sp, fontWeight = FontWeight.Black, color = Color(0xFF9C27B0))
                    }
                    Spacer(Modifier.height(40.dp))
                    Text("Quizee", fontSize = 60.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Your Smart Learning Companion", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(48.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Fast & Effective", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Mastering new topics quickly is key to success. Quizee uses AI to make learning 10x more effective.", color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                    Spacer(Modifier.height(60.dp))
                    Button(
                        onClick = { step = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF9C27B0)),
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Start Your Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                1 -> {
                    Text("Welcome aboard!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Sign in to continue your mission.", color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(48.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestIdToken("402469927230-aieqlsflrca43p0q0p7i1014asl6ta2r.apps.googleusercontent.com")
                                    .build()
                                val client = GoogleSignIn.getClient(context, gso)
                                client.signOut().addOnCompleteListener { googleSignInLauncher.launch(client.signInIntent) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("OR", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("Email Address", color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f)),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Password", color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f)),
                            singleLine = true
                        )
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                if (auth == null) { onContinue(email); return@Button }
                                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        if (auth.currentUser?.isEmailVerified == true) onContinue(email) else { step = 2; isLoading = false }
                                    } else {
                                        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { regTask ->
                                            if (regTask.isSuccessful) {
                                                regTask.result.user?.sendEmailVerification()
                                                step = 2
                                                isLoading = false
                                            } else {
                                                errorMessage = regTask.exception?.message
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = email.isNotBlank() && password.length >= 6,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF9C27B0)),
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Login / Register", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                2 -> {
                    Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.White)
                    Spacer(Modifier.height(24.dp))
                    Text("Verify your Email", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("We sent a link to:\n$email", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(48.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            auth?.currentUser?.reload()?.addOnCompleteListener {
                                isLoading = false
                                if (auth.currentUser?.isEmailVerified == true) onContinue(email)
                                else errorMessage = "Not verified yet. Check your inbox."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF9C27B0)),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("I've Verified", fontWeight = FontWeight.Bold)
                    }
                    if (errorMessage != null) Text(text = errorMessage!!, color = Color.White, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
