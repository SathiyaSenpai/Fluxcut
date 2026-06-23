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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fluxcut.ui.theme.FluxcutTheme
import kotlin.random.Random

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

val sampleProjects = listOf(
    Project(1, "Raw B-Roll: Mettupalayam", "May 20, 2026", "08:00", "1080p", "9:16", 30, Color(0xFF4A5568)),
    Project(2, "Vlog_Ep.21: The Trip", "May 18, 2026", "12:30", "4K", "16:9", 24, Color(0xFF2D3748)),
    Project(3, "Drone Cinematic", "May 15, 2026", "04:15", "4K", "16:9", 30, Color(0xFF1A365D)),
    Project(4, "Brand Promo 2024", "May 10, 2026", "01:45", "4K", "16:9", 30, Color(0xFF1C1C2E))
)

val Inter = FontFamily.SansSerif

// MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluxcutTheme {
                var currentScreen by remember { mutableStateOf("home") }

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
                        onCreateProjectClick = { currentScreen = "create_project" }
                    )
                    "settings" -> SettingsScreen(
                        onBackClick = { currentScreen = "home" }
                    )
                    "create_project" -> CreateProjectScreen(
                        onBackClick = { currentScreen = "home" },
                        onProjectCreated = { newProject ->
                            projectList.add(0, newProject)
                            currentScreen = "home"
                        }
                    )
                }
            }
        }
    }
}

// Home Screen
@Composable
fun HomeScreen(projects: List<Project>, onSettingsClick: () -> Unit, onCreateProjectClick: () -> Unit) {
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
            BottomNavBar(navBg = navBg, onSurface = onSurface, accent = accent)
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
                    subtle = subtle,
                    accent = accent,
                    onSurface = onSurface
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
                ProjectCard(project = project, surface = surface, onSurface = onSurface, subtle = subtle, accent = accent)
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
fun QuickActionsRow(surface: Color, onSurface: Color, subtle: Color, accent: Color) {
    val actions = listOf(
        QuickAction(Icons.Outlined.EmergencyRecording, "Capture"),
        QuickAction(Icons.Outlined.MusicNote, "Extract Audio"),
        QuickAction(Icons.Outlined.DeleteSweep, "Clear Cache", "3.2G"),
        QuickAction(Icons.Outlined.MenuBook, "Docs"),
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
                        .clickable { }
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
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .clickable { }
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
fun BottomNavBar(navBg: Color, onSurface: Color, accent: Color) {
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
                    .background(accent),
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
                    imageVector = Icons.Outlined.ArrowBack,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(onBackClick: () -> Unit, onProjectCreated: (Project) -> Unit) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle = if (dark) Color(0xFF888888) else Color(0xFF888888)
    val accent = Color(0xFF6C63FF)

    var nameInput by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf("9:16") }
    var selectedFps by remember { mutableStateOf(30) }
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
                    imageVector = Icons.Outlined.ArrowBack,
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
                placeholder = { Text("Project_${System.currentTimeMillis() % 100000}", color = subtle) },
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

            Text("Canvas Aspect Ratio", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = subtle)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val ratios = listOf("9:16", "16:9", "1:1")
                ratios.forEach { ratio ->
                    val isSelected = selectedRatio == ratio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent else surface)
                            .clickable { selectedRatio = ratio },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Target Frame Rate", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = subtle)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val fpsOptions = listOf(24, 30, 60)
                fpsOptions.forEach { fps ->
                    val isSelected = selectedFps == fps
                    Box(
                        modifier = Modifier
                            .weight(1f)
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
                    val finalName = if (nameInput.trim().isEmpty()) "Project_${System.currentTimeMillis() % 100000}" else nameInput
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
