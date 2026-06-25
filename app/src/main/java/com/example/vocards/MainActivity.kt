package com.example.vocards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.vocards.data.Project
import com.example.vocards.data.Vocad
import com.example.vocards.data.PopularLanguagesList
import com.example.vocards.ui.*
import com.example.vocards.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

// --- Custom Colors for Exact Match ---
val QuizeeDarkBg = Color(0xFF06091E)
val QuizeeCardBg = Color(0xFF161B30)
val QuizeeSecondaryText = Color(0xFF94A3B8)
val QuizeeAccent = Color(0xFF9C27B0)

// --- Activity Class ---
class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        ttsManager = TtsManager(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as VocadsApplication
            val factory = VocadViewModelFactory(application.repository, application.userPreferences)
            
            val vocadViewModel: VocadViewModel = viewModel(factory = factory)
            val quizViewModel: QuizViewModel = viewModel(factory = factory)
            val profileViewModel: ProfileViewModel = viewModel(factory = factory)
            
            val lang by profileViewModel.appLanguage.collectAsState()
            val isDarkModePref by profileViewModel.darkMode.collectAsState()
            
            // Apply Dark Theme preference, default to true
            val darkTheme = isDarkModePref ?: true

            LaunchedEffect(lang) { ttsManager.setLanguage(lang) }

            val currentUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }
            val startDestination = if (currentUser == null || !currentUser.isEmailVerified) "welcome" else "projects"

            VocadsTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = startDestination) {
                    composable("welcome") {
                        WelcomeScreen(
                            onContinue = { email ->
                                profileViewModel.saveEmail(email)
                                vocadViewModel.registerUser(email, "Verified")
                                profileViewModel.completeWelcome()
                                navController.navigate("projects") { popUpTo("welcome") { inclusive = true } } 
                            }
                        )
                    }
                    composable("projects") {
                        ProjectListScreen(
                            viewModel = vocadViewModel,
                            profileViewModel = profileViewModel,
                            quizViewModel = quizViewModel,
                            navController = navController,
                            onProjectClick = { id -> vocadViewModel.selectProject(id); navController.navigate("vocads") },
                            onProfileClick = { navController.navigate("profile") },
                            onStartQuiz = { mode, id, limit, mistakes, resume -> 
                                quizViewModel.startQuiz(mode, id, limit, mistakes, resume)
                                navController.navigate("quiz") 
                            }
                        )
                    }
                    composable("vocads") {
                        VocadApp(
                            viewModel = vocadViewModel,
                            profileViewModel = profileViewModel,
                            onStartQuiz = { mode, id, limit, mistakes, resume -> 
                                quizViewModel.startQuiz(mode, id, limit, mistakes, resume)
                                navController.navigate("quiz") 
                            },
                            onSpeak = { ttsManager.speak(it) },
                            onBack = { navController.popBackStack() },
                            onNavigateToProfile = { navController.navigate("profile") }
                        )
                    }
                    composable("quiz") {
                        QuizScreen(
                            viewModel = quizViewModel,
                            onNavigateBack = { quizViewModel.resetQuiz(); navController.popBackStack() },
                            onSpeak = { ttsManager.speak(it) }
                        )
                    }
                    composable("voice_correction") {
                        VoiceCorrectionScreen(onBack = { navController.popBackStack() })
                    }
                    composable("passive_listening") {
                        PassiveListeningScreen(viewModel = vocadViewModel, onSpeak = { ttsManager.speak(it) }, onBack = { navController.popBackStack() })
                    }
                    composable("lock_screen_preview") {
                        LockScreenGlanceScreen(viewModel = vocadViewModel, onBack = { navController.popBackStack() })
                    }
                    composable("profile") {
                        ProfileScreen(
                            viewModel = profileViewModel, 
                            mainViewModel = vocadViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutDown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: VocadViewModel, 
    profileViewModel: ProfileViewModel,
    quizViewModel: QuizViewModel,
    navController: NavHostController,
    onProjectClick: (Int) -> Unit, 
    onProfileClick: () -> Unit,
    onStartQuiz: (QuizMode, Int, Int?, Boolean, Boolean) -> Unit
) {
    val projects by viewModel.projectsState.collectAsState()
    val allVocads by viewModel.allVocadsState.collectAsState()
    val userEmail by profileViewModel.userEmail.collectAsState()
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDonePopup by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Library
    val focusManager = LocalFocusManager.current

    // Global search: search in project names OR project words
    val filteredProjects = if (searchQuery.isBlank()) projects else projects.filter { project ->
        val projectWords = allVocads.filter { it.projectId == project.id }
        project.name.contains(searchQuery, ignoreCase = true) || 
        (project.description?.contains(searchQuery, ignoreCase = true) == true) ||
        projectWords.any { it.word.contains(searchQuery, ignoreCase = true) || it.definition.contains(searchQuery, ignoreCase = true) }
    }

    val dailyMissionWords = remember(allVocads) { allVocads.shuffled().take(3).map { it.word } }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val text = FileImportHelper.extractText(context, it)
            if (text != null) {
                val name = context.contentResolver.query(it, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst()) c.getString(idx).substringBeforeLast(".") else "Imported"
                } ?: "New Project"
                viewModel.importAsNewProject(text, name, "English", "Arabic") // Default
                showDonePopup = true
            }
        }
    }

    Scaffold(
        containerColor = QuizeeDarkBg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E2235), contentColor = Color.White) {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = { showAddDialog = true }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Create") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Library") })
                NavigationBarItem(selected = false, onClick = onProfileClick, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top Bar: Search and Profile
            item {
                Spacer(Modifier.height(48.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = CircleShape,
                        color = Color(0xFF1E2235)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Icon(Icons.Default.Search, null, tint = QuizeeSecondaryText)
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) Text("Search words or projects...", color = QuizeeSecondaryText)
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = QuizeeSecondaryText)
                                }
                            } else {
                                IconButton(onClick = { focusManager.clearFocus() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Search, null, tint = QuizeeAccent)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = Color(0xFF9C27B0),
                        onClick = onProfileClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                userEmail?.take(1)?.uppercase() ?: "U", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }

            if (currentTab == 0) {
                // --- Your Collections (Recents) ---
                item {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Recents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showImportDialog = true }) {
                            Icon(Icons.Default.FileUpload, "Import", tint = QuizeeAccent)
                        }
                    }
                }

                if (filteredProjects.isEmpty()) {
                    item {
                        Text(if (searchQuery.isEmpty()) "No collections yet. Tap + below to start!" else "No matches found for '$searchQuery'.", color = QuizeeSecondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(filteredProjects) { project ->
                        RecentsItem(project, onDelete = { projectToDelete = project }, onClick = { onProjectClick(project.id) })
                    }
                }

                // --- Featured Card (Daily Challenges) ---
                item {
                    Text("Personalise your content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    FeaturedMissionCard(
                        title = "Daily Challenge Mission",
                        subtitle = if (dailyMissionWords.isEmpty()) "Review words from your projects to keep your streak" else "Words to review: ${dailyMissionWords.joinToString(", ")}",
                        buttonText = "Start Daily Session",
                        onClick = {
                            if (projects.isNotEmpty()) {
                                val randomProject = projects.random()
                                onStartQuiz(QuizMode.MEANING, randomProject.id, 5, false, false)
                            } else {
                                showAddDialog = true
                            }
                        }
                    )
                }

                // --- Power-Up Tools (Study Session) ---
                item {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("For your next study session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            StudyToolCard("Voice Correction", Icons.Default.Mic, Color(0xFFE91E63)) {
                                navController.navigate("voice_correction")
                            }
                        }
                        item {
                            StudyToolCard("Passive Listening", Icons.Default.Headset, Color(0xFF2196F3)) {
                                navController.navigate("passive_listening")
                            }
                        }
                        item {
                            StudyToolCard("Lock Screen", Icons.Default.PhonelinkLock, Color(0xFF795548)) {
                                navController.navigate("lock_screen_preview")
                            }
                        }
                    }
                }
            } else {
                // --- Library Tab (All Projects) ---
                item {
                    Text("Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                if (filteredProjects.isEmpty()) {
                    item {
                        Text(if (searchQuery.isEmpty()) "Library is empty." else "No matches found.", color = QuizeeSecondaryText)
                    }
                } else {
                    items(filteredProjects) { project ->
                        RecentsItem(project, onDelete = { projectToDelete = project }, onClick = { onProjectClick(project.id) })
                    }
                }
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }

        if (showAddDialog) AddProjectDialog(onDismiss = { showAddDialog = false }, onConfirm = { n, d, f, t -> viewModel.addProject(n, d, f, t); showAddDialog = false })
        
        if (showImportDialog) {
            ImportDialog(
                onDismiss = { showImportDialog = false }, 
                onPaste = { text, from, to -> 
                    viewModel.importAsNewProject(text, "Imported Collection", from, to)
                    showImportDialog = false
                    showDonePopup = true
                }, 
                onPickFile = { filePickerLauncher.launch(arrayOf("text/plain", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword", "application/json")) },
                onUrlImport = { url, from, to ->
                    viewModel.importFromUrl(url, "Web Mission") // Note: should update VM to take languages
                    showImportDialog = false
                    showDonePopup = true
                }
            )
        }

        if (showDonePopup) {
            AlertDialog(
                onDismissRequest = { showDonePopup = false },
                title = { Text("Done!") },
                text = { Text("Your mission collection has been imported successfully.") },
                confirmButton = { Button(onClick = { showDonePopup = false }) { Text("Great") } }
            )
        }

        if (projectToDelete != null) {
            AlertDialog(
                onDismissRequest = { projectToDelete = null },
                title = { Text("Delete Collection?") },
                text = { Text("Are you sure you want to delete '${projectToDelete?.name}'? This cannot be undone.", color = Color.White) },
                confirmButton = {
                    Button(onClick = { projectToDelete?.let { viewModel.deleteProject(it) }; projectToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = { TextButton(onClick = { projectToDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun RecentsItem(project: Project, onDelete: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E2235)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Style, null, tint = QuizeeAccent, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${project.fromLanguage} \u2192 ${project.toLanguage} \u2022 by you", color = QuizeeSecondaryText, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun FeaturedMissionCard(title: String, subtitle: String, buttonText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B30)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = QuizeeAccent, 
                    modifier = Modifier.size(48.dp)
                )
                Icon(Icons.Default.MoreVert, null, tint = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = QuizeeSecondaryText, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D334D))
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StudyToolCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B30))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(32.dp), tint = color)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatChip(label: String, count: Int, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), fontWeight = FontWeight.Bold, color = color, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun DashboardChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(modifier = Modifier.size(100.dp), color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(20.dp), color)
            Text(value, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VocadApp(
    viewModel: VocadViewModel, 
    profileViewModel: ProfileViewModel,
    onStartQuiz: (QuizMode, Int, Int?, Boolean, Boolean) -> Unit, 
    onSpeak: (String) -> Unit, 
    onBack: () -> Unit, 
    onNavigateToProfile: () -> Unit
) {
    val vocads by viewModel.currentVocadsState.collectAsState()
    val projects by viewModel.projectsState.collectAsState()
    val projectId by viewModel.selectedProjectId.collectAsState()
    val userEmail by profileViewModel.userEmail.collectAsState()
    
    var showImportDialog by remember { mutableStateOf(false) }
    var showDonePopup by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val text = FileImportHelper.extractText(context, it)
            if (text != null && projectId != null) { 
                viewModel.importVocads(text, projectId!!)
                showDonePopup = true
            }
        }
    }

    val currentProject = projects.find { it.id == projectId }

    Scaffold(
        containerColor = QuizeeDarkBg,
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) { Icon(Icons.Default.FileUpload, null, tint = Color.White) }
                    IconButton(onClick = { /* Add to bookmarks */ }) { Icon(Icons.Default.BookmarkBorder, null, tint = Color.White) }
                    IconButton(onClick = { /* More options */ }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            var showAddWordDialog by remember { mutableStateOf(false) }
            FloatingActionButton(
                onClick = { showAddWordDialog = true },
                containerColor = QuizeeAccent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Word")
            }
            if (showAddWordDialog) {
                AddVocadDialog(
                    onDismiss = { showAddWordDialog = false },
                    onConfirm = { w, d, e ->
                        viewModel.addVocad(w, d, e)
                        showAddWordDialog = false
                    }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Pager / Flashcard Area (Flippable) ---
            item {
                val pagerState = rememberPagerState(pageCount = { vocads.size.coerceAtLeast(1) })
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = QuizeeCardBg)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        var isFlipped by remember(page) { mutableStateOf(false) }
                        var isDirectionSwapped by remember { mutableStateOf(false) }
                        
                        Box(
                            Modifier.fillMaxSize().clickable { isFlipped = !isFlipped },
                            contentAlignment = Alignment.Center
                        ) {
                            if (vocads.isEmpty()) {
                                Text("No words here", color = Color.White)
                            } else {
                                val currentVocad = vocads[page]
                                // Logical swap: if swapped, word and definition switch places
                                val showDefinition = if (isDirectionSwapped) !isFlipped else isFlipped
                                val text = if (showDefinition) currentVocad.definition else currentVocad.word
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                                    
                                    // Small toggle for direction
                                    IconButton(onClick = { isDirectionSwapped = !isDirectionSwapped }) {
                                        Icon(Icons.Default.SwapVert, "Swap Direction", tint = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                        IconButton(onClick = {}, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                            Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        
                        // Dots indicator
                        Row(
                            Modifier.height(50.dp).fillMaxWidth().align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(vocads.size.coerceAtMost(3)) { iteration ->
                                val color = if (pagerState.currentPage % 3 == iteration) Color.White else Color.Gray
                                Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(6.dp))
                            }
                        }
                    }
                }
            }

            // --- Project Header ---
            item {
                Text(currentProject?.name ?: "Mission", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(24.dp), shape = CircleShape, color = Color(0xFF9C27B0)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(userEmail?.take(1)?.uppercase() ?: "U", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${currentProject?.fromLanguage} \u2192 ${currentProject?.toLanguage}", color = QuizeeSecondaryText, fontSize = 14.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("${vocads.size} terms", color = QuizeeSecondaryText, fontSize = 14.sp)
                }
            }

            // --- Action List ---
            item { ActionItem(Icons.Default.Style, "Flashcards") { onStartQuiz(QuizMode.FLASHCARDS, projectId!!, null, false, false) } }
            item { ActionItem(Icons.Default.Autorenew, "Learn") { onStartQuiz(QuizMode.LEARN, projectId!!, null, false, false) } }
            item { ActionItem(Icons.Default.Description, "Test") { onStartQuiz(QuizMode.MEANING, projectId!!, null, false, false) } }
            item { ActionItem(Icons.Default.Dns, "Match") { onStartQuiz(QuizMode.REVERSE, projectId!!, null, false, false) } }
            item { ActionItem(Icons.Default.RocketLaunch, "Blast") { onStartQuiz(QuizMode.WRITING, projectId!!, null, false, false) } }
            item { ActionItem(Icons.Default.GridView, "Blocks") { onStartQuiz(QuizMode.BLOCKS, projectId!!, null, false, false) } }
            
            item { Spacer(Modifier.height(32.dp)) }
        }

        if (showImportDialog) {
            ImportDialog(
                onDismiss = { showImportDialog = false }, 
                onPaste = { text, from, to -> 
                    viewModel.importAsNewProject(text, "Imported Collection", from, to)
                    showImportDialog = false
                    showDonePopup = true
                }, 
                onPickFile = { filePickerLauncher.launch(arrayOf("text/plain", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword", "application/json")) },
                onUrlImport = { url, from, to ->
                    viewModel.importFromUrl(url, "Web Mission", from, to)
                    showImportDialog = false
                    showDonePopup = true
                }
            )
        }

        if (showDonePopup) {
            AlertDialog(
                onDismissRequest = { showDonePopup = false },
                title = { Text("Done!") },
                text = { Text("Your mission collection has been updated.") },
                confirmButton = { Button(onClick = { showDonePopup = false }) { Text("Great") } }
            )
        }
    }
}

@Composable
fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QuizeeCardBg)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = QuizeeAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String, String?, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var fromLang by remember { mutableStateOf("English") }
    var toLang by remember { mutableStateOf("Arabic") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Mission Collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                
                Text("Direction:", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageSelector(label = "From", selected = fromLang, onSelect = { fromLang = it }, modifier = Modifier.weight(1f))
                    IconButton(onClick = { val temp = fromLang; fromLang = toLang; toLang = temp }) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = QuizeeAccent)
                    }
                    LanguageSelector(label = "To", selected = toLang, onSelect = { toLang = it }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name, desc, fromLang, toLang) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LanguageSelector(label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedCard(onClick = { expanded = true }) {
            Column(Modifier.padding(8.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(selected, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PopularLanguagesList.forEach { (name, _) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(name); expanded = false })
            }
        }
    }
}

@Composable
fun ImportDialog(onDismiss: () -> Unit, onPaste: (String, String, String) -> Unit, onPickFile: () -> Unit, onUrlImport: (String, String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(0) } // 0: Paste, 1: URL
    
    var fromLang by remember { mutableStateOf("English") }
    var toLang by remember { mutableStateOf("Arabic") }

    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Bulk Import") }, 
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabRow(selectedTabIndex = mode, containerColor = Color.Transparent, contentColor = QuizeeAccent) {
                    Tab(selected = mode == 0, onClick = { mode = 0 }, text = { Text("Paste") })
                    Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text("URL") })
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text("Direction:", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageSelector(label = "From", selected = fromLang, onSelect = { fromLang = it }, modifier = Modifier.weight(1f))
                    IconButton(onClick = { val temp = fromLang; fromLang = toLang; toLang = temp }) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = QuizeeAccent)
                    }
                    LanguageSelector(label = "To", selected = toLang, onSelect = { toLang = it }, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))

                if (mode == 0) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Paste text here") }, modifier = Modifier.fillMaxWidth().height(150.dp))
                    Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Select File") }
                } else {
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("GitHub Raw / Direct Link") }, placeholder = { Text("https://...") }, modifier = Modifier.fillMaxWidth())
                }
            } 
        }, 
        confirmButton = { 
            Button(onClick = { if (mode == 0) onPaste(text, fromLang, toLang) else onUrlImport(url, fromLang, toLang) }, enabled = if (mode == 0) text.isNotBlank() else url.isNotBlank()) {
                Text("Import") 
            } 
        }, 
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } }
    )
}

@Composable
fun AddVocadDialog(onDismiss: () -> Unit, onConfirm: (String, String, String?) -> Unit) {
    var w by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    var e by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Word") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("Word") }, modifier = Modifier.weight(1f)); IconButton(onClick = { scope.launch { loading = true; fetchDefinition(w)?.let { d = it.first; e = it.second ?: "" }; loading = false } }, enabled = w.isNotBlank()) { if (loading) CircularProgressIndicator(Modifier.size(24.dp)) else Icon(Icons.Default.Language, null) } }; OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("Definition") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(value = e, onValueChange = { e = it }, label = { Text("Example") }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { Button(onClick = { if (w.isNotBlank() && d.isNotBlank()) onConfirm(w, d, e.ifBlank { null }) }) { Text("Add") } }, dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } })
}

suspend fun fetchDefinition(word: String): Pair<String, String?>? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
        val conn = url.openConnection() as HttpURLConnection
        if (conn.responseCode == 200) {
            val res = conn.inputStream.bufferedReader().readText()
            val entry = JSONArray(res).getJSONObject(0)
            val def = entry.getJSONArray("meanings").getJSONObject(0).getJSONArray("definitions").getJSONObject(0)
            Pair(def.getString("definition"), def.optString("example", null))
        } else null
    } catch (e: Exception) { null }
}
