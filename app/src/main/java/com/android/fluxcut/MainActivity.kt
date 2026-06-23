package com.android.fluxcut

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fluxcut.ui.theme.FluxcutTheme
import kotlin.random.Random
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import android.Manifest
import androidx.activity.compose.BackHandler

// Data
data class Project(
    val id: Int,
    val title: String,
    val date: String,
    val duration: String,
    val resolution: String,
    val aspectRatio: String,
    val fps: Int,
    val thumbnailColor: Color
)

data class EditorArgs(val project: Project)

// MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluxcutTheme {
                var currentScreen by remember { mutableStateOf("home") }
                var editorProject by remember { mutableStateOf<Project?>(null) }

                BackHandler(enabled = currentScreen != "home") {
                    currentScreen = "home"
                }

                val projectList = remember {
                    mutableStateListOf(
                        Project(1, "Wanderlust", "May 20, 2026", "00:45", "1080p", "9:16", 30, Color(0xFF4A5568)),
                        Project(2, "Architecture", "May 18, 2026", "01:12", "4K", "16:9", 24, Color(0xFF2D3748))
                    )
                }

                when (currentScreen) {
                    "home" -> HomeScreen(
                        projects = projectList,
                        onSettingsClick = { currentScreen = "settings" },
                        onCreateProjectClick = { currentScreen = "create_project" },
                        onCaptureClick = { currentScreen = "capture" },
                        onDocsClick = { currentScreen = "docs" },
                        onProjectClick = { project ->
                            editorProject = project
                            currentScreen = "editor"
                        },
                        onEditorClick = {
                            if (projectList.isNotEmpty()) {
                                editorProject = projectList.first()
                                currentScreen = "editor"
                            } else {
                                currentScreen = "create_project"
                            }
                        }
                    )
                    "capture" -> CaptureScreen(
                        onBackClick = { currentScreen = "home" }
                    )
                    "docs" -> DocsScreen(
                        onBackClick = { currentScreen = "home" }
                    )
                    "create_project" -> CreateProjectScreen(
                        onBackClick = { currentScreen = "home" },
                        onProjectCreated = { newProject ->
                            projectList.add(0, newProject)
                            editorProject = newProject
                            currentScreen = "editor"
                        }
                    )
                    "settings" -> SettingsScreen(
                        onBackClick = { currentScreen = "home" }
                    )
                    "editor" -> editorProject?.let { project ->
                        EditorScreen(
                            project = project,
                            onBackClick = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}

// Home Screen
@Composable
fun HomeScreen(
    projects: List<Project>,
    onSettingsClick: () -> Unit,
    onCreateProjectClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onDocsClick: () -> Unit,
    onProjectClick: (Project) -> Unit,
    onEditorClick: () -> Unit
) {

    val context = LocalContext.current

    // live storage size state
    var cacheSizeState by remember { mutableStateOf("Calculating...") }

    // Read the size on screen launch
    LaunchedEffect(Unit) {
        cacheSizeState = CacheManager.getCacheSize(context)
    }

    val dark = isSystemInDarkTheme()

    val bg = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle = if (dark) Color(0xFF888888) else Color(0xFF888888)
    val accent = Color(0xFF6C63FF)
    val navBg = if (dark) Color(0xFF111118) else Color(0xFFFFFFFF)

    Scaffold(
        containerColor = bg,
        bottomBar = {
            BottomNavBar(
                navBg = navBg,
                onSurface = onSurface,
                accent = accent,
                onEditorClick = onEditorClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                TopBar(
                    onSurface = onSurface,
                    subtle = subtle,
                    accent = accent,
                    onSettingsClick = onSettingsClick,
                )
            }

            item {
                NewProjectCard(accent = accent, dark = dark, onCreateClick = onCreateProjectClick)
            }

            item {
                QuickActionsRow(
                    surface = surface,
                    accent = accent,
                    onSurface = onSurface,
                    subtle = subtle,
                    onCaptureClick = onCaptureClick,
                    onClearCacheClick = {
                        val cleared = CacheManager.clearAllCache(context)
                        if (cleared) {
                            cacheSizeState = CacheManager.getCacheSize(context)
                            Toast.makeText(context, "Storage cleared", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to clear some files", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDocsClick = onDocsClick
                )
            }

            item {
                Text(
                    text = "Temporary Cache: $cacheSizeState",
                    fontSize = 12.sp,
                    color = subtle,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Projects",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View all", fontSize = 13.sp, color = accent)
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            items(projects) { project ->
                ProjectCard(
                    project = project,
                    surface = surface,
                    onSurface = onSurface,
                    subtle = subtle,
                    accent = accent,
                    onClick = { onProjectClick(project) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.SwipeLeft,
                        contentDescription = null,
                        tint = subtle,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Swipe left to delete", fontSize = 12.sp, color = subtle)
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Made with passion ♥", fontSize = 11.sp, color = subtle)
                }
            }
        }
    }
}

@Composable
fun TopBar(
    onSurface: Color,
    subtle: Color,
    accent: Color,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        Toast.makeText(context, "Stop poking me, I am just the logo.", Toast.LENGTH_SHORT).show()
                    })
                }
            ) {
                Text("flu", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = onSurface)
                Text("✗", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accent)
                Text("cut", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("</> ", fontSize = 11.sp, color = accent, fontWeight = FontWeight.SemiBold)
                Text("open source video editor", fontSize = 11.sp, color = subtle)
            }
        }
        Icon(
            Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = onSurface,
            modifier = Modifier.size(22.dp).clickable { onSettingsClick() }
        )
    }
}

data class AmbientParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
fun FloatingParticles(particleColor: Color) {
    val particles = remember {
        List(7) {
            AmbientParticle(
                xFraction = Random.nextFloat(),
                yFraction = Random.nextFloat(),
                radius = Random.nextFloat() * 0.8f + 0.6f,
                alpha = Random.nextFloat() * 0.3f + 0.3f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            var currentYFraction = particle.yFraction - progress
            if (currentYFraction < 0f) {
                currentYFraction += 1f
            }

            val edgeAlpha = when {
                currentYFraction < 0.2f -> currentYFraction / 0.2f
                currentYFraction > 0.8f -> (1f - currentYFraction) / 0.2f
                else -> 1f
            }

            val finalAlpha = particle.alpha * edgeAlpha
            val x = particle.xFraction * width
            val y = currentYFraction * height
            
            drawCircle(
                color = particleColor,
                radius = (particle.radius * 5.0f).dp.toPx(),
                center = Offset(x, y),
                alpha = finalAlpha * 0.12f
            )
            
            drawCircle(
                color = particleColor,
                radius = (particle.radius * 2.5f).dp.toPx(),
                center = Offset(x, y),
                alpha = finalAlpha * 0.35f
            )
            
            drawCircle(
                color = particleColor,
                radius = particle.radius.dp.toPx(),
                center = Offset(x, y),
                alpha = finalAlpha
            )
        }
    }
}

@Composable
fun NewProjectCard(accent: Color, dark: Boolean, onCreateClick: () -> Unit) {
    val backgroundImage = if (dark) R.drawable.new_project_bg_dark else R.drawable.new_project_bg
    val particleColor = if (dark) Color(0xFFA855F7) else Color.White
    val glowColor = if (dark) Color(0xFFA855F7) else Color.White
    val iconBgColor = if (dark) Color(0xFF1C1C28) else Color(0xFFFFFFFF)

    val overlayBrush = if (dark) {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.6f),
                Color.Black.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF0F0F1A).copy(alpha = 0.75f),
                Color(0xFF6C63FF).copy(alpha = 0.25f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCreateClick() }
    ) {
        Image(
            painter = painterResource(id = backgroundImage),
            contentDescription = "New Project Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )

        FloatingParticles(particleColor = particleColor)

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Text(
                text = "New Project",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Start creating",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Project",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

data class QuickAction(val icon: ImageVector, val label: String, val badge: String? = null)

@Composable
fun QuickActionsRow(
    surface: Color,
    onSurface: Color,
    subtle: Color,
    accent: Color,
    onCaptureClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onDocsClick: () -> Unit
) {
    val context = LocalContext.current
    val cacheSize = remember { mutableStateOf(CacheManager.getCacheSize(context)) }

    val actions = listOf(
        QuickAction(Icons.Outlined.EmergencyRecording, "Capture"),
        QuickAction(Icons.Outlined.MusicNote, "Extract Audio"),
        QuickAction(Icons.Outlined.DeleteSweep, "Clear Cache", cacheSize.value),
        QuickAction(Icons.AutoMirrored.Outlined.MenuBook, "Docs"),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        actions.forEach { action ->
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(surface)
                        .clickable {
                            when (action.label) {
                                "Capture" -> onCaptureClick()
                                "Clear Cache" -> {
                                    onClearCacheClick()
                                    cacheSize.value = CacheManager.getCacheSize(context)
                                }
                                "Docs" -> onDocsClick()
                            }
                        }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = action.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        lineHeight = 14.sp
                    )
                }

                action.badge?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            it,
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    surface: Color,
    onSurface: Color,
    subtle: Color,
    accent: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(project.thumbnailColor)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                project.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )
            Text("${project.date} · ${project.duration}", fontSize = 12.sp, color = subtle)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    project.resolution,
                    fontSize = 10.sp,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Icon(
            Icons.Outlined.MoreVert,
            contentDescription = "More",
            tint = subtle,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BottomNavBar(navBg: Color, onSurface: Color, accent: Color, onEditorClick: () -> Unit = {}) {
    Surface(
        color = navBg,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Home,
                    contentDescription = "Home",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text("Home", fontSize = 10.sp, color = accent)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable { onEditorClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FlashOn,
                    contentDescription = "Editor",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text("Profile", fontSize = 10.sp, color = onSurface)
            }
        }
    }
}

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val dark = isSystemInDarkTheme()

    val bg        = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface   = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle    = if (dark) Color(0xFF888888) else Color(0xFF888888)
    val accent    = Color(0xFF6C63FF)

    Scaffold(
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Go Back",
                    tint = onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
            }

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surface)
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Export Resolution", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = onSurface)
                        Text("Default render target for new timelines", fontSize = 12.sp, color = subtle)
                    }
                    Text("1080p", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = bg, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cache Directory", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = onSurface)
                        Text("Where proxy video segments are saved", fontSize = 12.sp, color = subtle)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = subtle, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

data class AspectRatioPreset(val ratio: String, val label: String, val width: Float, val height: Float)

val ratioPresets = listOf(
    AspectRatioPreset("16:9", "YouTube", 16f, 9f),
    AspectRatioPreset("9:16", "Shorts", 9f, 16f),
    AspectRatioPreset("1:1", "Square", 1f, 1f),
    AspectRatioPreset("4:5", "Insta Feed", 4f, 5f),
    AspectRatioPreset("21:9", "Cinema", 21f, 9f),
    AspectRatioPreset("4:3", "Standard", 4f, 3f),
    AspectRatioPreset("Custom", "Free", 0f, 0f) // 0f triggers the custom icon logic
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(onBackClick: () -> Unit, onProjectCreated: (Project) -> Unit) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle = if (dark) Color(0xFF888888) else Color(0xFF888888)
    val accent = Color(0xFF6C63FF)

    val funnyPlaceholders = remember {
        listOf(
            "Untitled Masterpiece",
            "Delete Me Later",
            "Low Budget Blockbuster",
            "Oscar Winner 2026",
            "Cat Video #42",
            "Epic Montage",
            "Client Wants Changes",
            "Hollywood Budget $0",
            "Coffee Powered Edit",
            "Clickbait Thumbnail",
            "Final_Final_v2_Real",
            "This One Is Final"
        )
    }
    val defaultPlaceholder = remember { funnyPlaceholders.random() }

    var nameInput by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf("9:16") }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedResolution by remember { mutableStateOf("1080p") }

    Scaffold(
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Cancel",
                    tint = onSurface,
                    modifier = Modifier.size(24.dp).clickable { onBackClick() }
                )
                Spacer(Modifier.width(16.dp))
                Text("New Project Configuration", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurface)
            }

            Spacer(Modifier.height(24.dp))

            Text("Project Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = subtle)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = { Text(defaultPlaceholder, color = subtle) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = surface,
                    focusedContainerColor = surface,
                    unfocusedContainerColor = surface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text("Aspect Ratio", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = subtle)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ratioPresets) { preset ->
                    val isSelected = selectedRatio == preset.ratio
                    val cardBg = if (isSelected) accent.copy(alpha = 0.15f) else surface
                    val borderColor = if (isSelected) accent else Color.Transparent

                    Column(
                        modifier = Modifier
                            .width(88.dp) // Fixed width keeps the list perfectly uniform
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { selectedRatio = preset.ratio }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // The visual representation container
                        Box(
                            modifier = Modifier
                                .height(28.dp) // Locks the height so tall and wide boxes align on center
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (preset.width == 0f) {
                                // Draw an edit icon for the "Custom" option
                                Icon(Icons.Outlined.Edit, contentDescription = "Custom", tint = if (isSelected) accent else subtle, modifier = Modifier.size(20.dp))
                            } else {
                                // Draw the exact aspect ratio mathematically
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(preset.width / preset.height, matchHeightConstraintsFirst = true)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.5.dp, if (isSelected) accent else subtle, RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(preset.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) accent else onSurface, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(preset.ratio, fontSize = 10.sp, color = subtle)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Input: Frame Rate Selection Controls
            Text("Target Frame Rate", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = subtle)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val fpsOptions = listOf(24, 30, 60, 120) // 120 FPS integrated
                items(fpsOptions) { fps ->
                    val isSelected = selectedFps == fps
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent else surface)
                            .clickable { selectedFps = fps },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${fps} FPS", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else onSurface)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalName = if (nameInput.trim().isEmpty()) defaultPlaceholder else nameInput
                    val newProjectInstance = Project(
                        id = Random.nextInt(100, 100000),
                        title = finalName,
                        date = "Jun 23, 2026",
                        duration = "00:00",
                        resolution = selectedResolution,
                        aspectRatio = selectedRatio,
                        fps = selectedFps,
                        thumbnailColor = Color(0xFF3B82F6)
                    )
                    onProjectCreated(newProjectInstance)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Initialize Timeline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Capture Screen (CameraX Integration) ──────────────────────────────────────

@Composable
fun CaptureScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var hasPermissions by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    var recordedTime by remember { mutableStateOf("00:00") }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    val videoCaptureState = remember { mutableStateOf<androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?>(null) }
    val activeRecordingState = remember { mutableStateOf<androidx.camera.video.Recording?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermissions = permissions[android.Manifest.permission.CAMERA] == true &&
                    permissions[android.Manifest.permission.RECORD_AUDIO] == true
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermissions) {
            AndroidView(
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx)
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val recorder = androidx.camera.video.Recorder.Builder()
                            .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HIGHEST))
                            .build()
                        val videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)
                        videoCaptureState.value = videoCapture

                        val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)

                            cameraControl = camera.cameraControl
                        } catch (exc: Exception) {
                            android.util.Log.e("FluxCutCamera", "Use case binding failed", exc)
                        }
                    }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomMultiplier, _ ->
                            zoomRatio = (zoomRatio * zoomMultiplier).coerceIn(1f, 5f)
                            cameraControl?.setZoomRatio(zoomRatio)
                        }
                    }
            )

            // Top UI Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).clickable { onBackClick() }
                )

                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                        Spacer(Modifier.width(8.dp))
                        Text(recordedTime, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Empty spacer to balance the row layout
                Spacer(modifier = Modifier.size(28.dp))
            }

            // Record Button Logic
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.DarkGray else Color.Red)
                    .clickable {
                        val videoCapture = videoCaptureState.value ?: return@clickable
                        val currentRecording = activeRecordingState.value

                        if (currentRecording != null) {
                            currentRecording.stop()
                            activeRecordingState.value = null
                            isRecording = false
                            recordedTime = "00:00" // Reset timer
                        } else {
                            val name = "FluxCut_${System.currentTimeMillis()}.mp4"
                            val file = java.io.File(context.filesDir, name)
                            val outputOptions = androidx.camera.video.FileOutputOptions.Builder(file).build()

                            if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                activeRecordingState.value = videoCapture.output
                                    .prepareRecording(context, outputOptions)
                                    .withAudioEnabled()
                                    .start(androidx.core.content.ContextCompat.getMainExecutor(context)) { event ->
                                        when (event) {
                                            is androidx.camera.video.VideoRecordEvent.Start -> isRecording = true

                                            is androidx.camera.video.VideoRecordEvent.Status -> {
                                                val durationNanos = event.recordingStats.recordedDurationNanos
                                                val seconds = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(durationNanos)
                                                val mins = seconds / 60
                                                val secs = seconds % 66
                                                recordedTime = String.format("%02d:%02d", mins, secs)
                                            }

                                            is androidx.camera.video.VideoRecordEvent.Finalize -> {
                                                isRecording = false
                                                recordedTime = "00:00"
                                                if (!event.hasError()) {
                                                    android.widget.Toast.makeText(context, "Saved to App Files", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    activeRecordingState.value?.close()
                                                    activeRecordingState.value = null
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
            )
        } else {
            // Permission fallback UI
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.VideocamOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera & Mic permissions required", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onBackClick() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))) {
                    Text("Go Back")
                }
            }
        }
    }
}

// ── Documentation Screen ──────────────────────────────────────────────────────

@Composable
fun DocsScreen(onBackClick: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle = if (dark) Color(0xFF888888) else Color(0xFF888888)
    val accent = Color(0xFF6C63FF)

    Scaffold(
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(12.dp))

            // Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Go Back",
                    tint = onSurface,
                    modifier = Modifier.size(24.dp).clickable { onBackClick() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "FluxCut Manual",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
            }

            Spacer(Modifier.height(24.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Section 1: Core Engine
                item {
                    DocSectionCard(surface = surface) {
                        Text("1. The Timeline Engine", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "FluxCut uses a strict rendering pipeline. When you initialize a project, the target Frame Rate (FPS) and Aspect Ratio are locked. Media imported into the timeline will automatically scale and resample to match these project constraints to prevent dropped frames during export.",
                            fontSize = 13.sp,
                            color = onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Section 2: Quick Actions
                item {
                    DocSectionCard(surface = surface) {
                        Text("2. Quick Action Utilities", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                        Spacer(Modifier.height(12.dp))

                        Text("Capture", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
                        Text(
                            "Bypasses the system camera and records hardware-encoded .mp4 files directly into the app's internal storage block. This prevents gallery clutter and speeds up import times.",
                            fontSize = 13.sp, color = subtle, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Text("Extract Audio", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
                        Text(
                            "A fast demuxing tool. It separates the audio track from a selected video container without re-encoding, saving it as a standalone asset for voiceovers or background tracks.",
                            fontSize = 13.sp, color = subtle, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Text("Clear Cache", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
                        Text(
                            "Deletes temporary preview frames and proxy files generated during editing. Running this will not delete your project files or saved timeline configurations.",
                            fontSize = 13.sp, color = subtle, lineHeight = 18.sp
                        )
                    }
                }

                // Section 3: Privacy & Storage
                item {
                    DocSectionCard(surface = surface) {
                        Text("3. Storage & Privacy", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "FluxCut operates as an offline-first tool. All project databases, media files, and rendering processes happen locally on your device hardware. No telemetry or video data is transmitted to remote servers.",
                            fontSize = 13.sp,
                            color = onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Section 4: FOSS License
                item {
                    DocSectionCard(surface = surface) {
                        Text("4. Open Source Licensing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This software is distributed under the GNU General Public License v3.0. You are free to inspect, modify, and distribute the source code. Pull requests for bug fixes and feature expansions are handled via the official GitHub repository.",
                            fontSize = 13.sp,
                            color = onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Bottom padding
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// Helper composable to maintain clean layout formatting for the text blocks
@Composable
fun DocSectionCard(surface: Color, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .padding(16.dp)
    ) {
        content()
    }
}

