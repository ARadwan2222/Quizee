package com.example.vocards.ui

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Surface(modifier = Modifier.size(70.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(email?.take(1)?.uppercase() ?: "U", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("My Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(email ?: "User@quizee.com", color = QuizeeSecondaryText, fontSize = 14.sp)
                }
            }

            SettingsSection(title = "Preferences") {
                SettingsToggleItem(
                    title = "Dark Mode",
                    icon = Icons.Default.DarkMode,
                    checked = isDark ?: true,
                    onCheckedChange = { viewModel.saveDarkMode(it) }
                )
            }

            SettingsSection(title = "Learning Goals") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Daily Word Goal")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (dailyGoal > 1) viewModel.saveDailyGoal(dailyGoal - 1) }) { Icon(Icons.Default.Remove, null) }
                        Text("$dailyGoal", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.saveDailyGoal(dailyGoal + 1) }) { Icon(Icons.Default.Add, null) }
                    }
                }
            }

            SettingsSection(title = "Account") {
                SettingsActionItem(title = "Language Settings", icon = Icons.Default.Language) { }
                SettingsActionItem(title = "Sign Out", icon = Icons.Default.Logout, color = Color.Red) {
                    FirebaseAuth.getInstance().signOut()
                    onBack()
                }
            }

            Text(
                text = "Quizee Version 1.4",
                color = QuizeeSecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = QuizeeSecondaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) { content() }
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
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (color == Color.Red) color else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, color = if (color == Color.Red) color else Color.Unspecified)
        }
        Icon(Icons.Default.ChevronRight, null, tint = QuizeeSecondaryText)
    }
}
