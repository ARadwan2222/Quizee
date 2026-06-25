package com.example.vocards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vocards.ui.theme.QuizeeDarkBg
import com.example.vocards.ui.theme.QuizeeSecondaryText
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    mainViewModel: VocadViewModel,
    onBack: () -> Unit
) {
    val email by viewModel.userEmail.collectAsState()
    val isDark by viewModel.darkMode.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = QuizeeDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Profile Header ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    color = Color(0xFF9C27B0)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            email?.take(1)?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("My Profile", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(email ?: "User@quizee.com", color = QuizeeSecondaryText, fontSize = 14.sp)
                }
            }

            // --- Preferences Section ---
            SettingsSection(title = "Preferences") {
                SettingsToggleItem(
                    title = "Dark Mode",
                    icon = Icons.Default.DarkMode,
                    checked = isDark ?: true,
                    onCheckedChange = { viewModel.saveDarkMode(it) }
                )
                SettingsToggleItem(
                    title = "Push Notifications",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                SettingsToggleItem(
                    title = "Study Sounds",
                    icon = Icons.Default.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
            }

            // --- Goal Section ---
            SettingsSection(title = "Learning Goals") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Daily Word Goal", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (dailyGoal > 1) viewModel.saveDailyGoal(dailyGoal - 1) }) {
                            Icon(Icons.Default.Remove, null, tint = Color.White)
                        }
                        Text("$dailyGoal", color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.saveDailyGoal(dailyGoal + 1) }) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                }
            }

            // --- Account Actions ---
            SettingsSection(title = "Account") {
                SettingsActionItem(title = "Language Settings", icon = Icons.Default.Language) { 
                    /* Navigate to specific lang settings if needed */
                }
                SettingsActionItem(title = "Cloud Sync", icon = Icons.Default.CloudSync) {
                    /* viewModel.syncToCloud() */
                }
                SettingsActionItem(title = "Help & Support", icon = Icons.Default.HelpOutline) { }
                SettingsActionItem(title = "Sign Out", icon = Icons.Default.Logout, color = Color.Red) {
                    FirebaseAuth.getInstance().signOut()
                    onBack() // Or navigate to welcome
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = QuizeeSecondaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Surface(
            color = Color(0xFF161B30),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, color = Color.White)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF9C27B0), checkedTrackColor = Color(0xFFE1BEE7))
        )
    }
}

@Composable
fun SettingsActionItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (color == Color.Red) color else Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, color = color)
        }
        Icon(Icons.Default.ChevronRight, null, tint = QuizeeSecondaryText)
    }
}
