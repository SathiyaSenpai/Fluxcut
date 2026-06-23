package com.android.fluxcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fluxcut.ui.theme.FluxcutTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import kotlin.random.Random
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

// ── Data ──────────────────────────────────────────────────────────────────────

data class Project(
    val id: Int,
    val title: String,
    val date: String,
    val duration: String,
    val resolution: String,
    val thumbnailColor: Color          // placeholder until real thumbnails
)

val sampleProjects = listOf(
    Project(1, "Wanderlust",    "May 20, 2024", "00:45", "1080p", Color(0xFF4A5568)),
    Project(2, "Architecture",  "May 18, 2024", "01:12", "4K",    Color(0xFF2D3748)),
    Project(3, "Ocean Drive",   "May 15, 2024", "00:32", "1080p", Color(0xFF1A365D)),
    Project(4, "City Lights",   "May 10, 2024", "00:19", "720p",  Color(0xFF1C1C2E)),
)

val Inter = FontFamily.SansSerif

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluxcutTheme {
                // Track the active layout screen state
                var currentScreen by remember { mutableStateOf("home") }

                when (currentScreen) {
                    "home" -> HomeScreen(onSettingsClick = { currentScreen = "settings" })
                    "settings" -> SettingsScreen(onBackClick = { currentScreen = "home" })
                }
            }
        }
    }

// ── Home Screen ───────────────────────────────────────────────────────────────

    @Composable
    fun HomeScreen(onSettingsClick: () -> Unit) {
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

                // Top bar
                item {
                    Spacer(Modifier.height(12.dp))
                    TopBar(
                        onSurface = onSurface,
                        subtle = subtle,
                        accent = accent,
                        onSettingsClick = onSettingsClick,
                    )
                }

                // New Project hero card
                item {
                    NewProjectCard(accent = accent, dark = dark)
                }

                // Quick actions
                item {
                    QuickActionsRow(
                        surface = surface,
                        subtle = subtle,
                        accent = accent,
                        onSurface = onSurface
                    )
                }

                // Projects header
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

                // Project cards
                items(sampleProjects) { project ->
                    ProjectCard(
                        project = project,
                        surface = surface,
                        onSurface = onSurface,
                        subtle = subtle,
                        accent = accent
                    )
                }

                // Swipe hint
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

                // Footer
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

// ── Top Bar ───────────────────────────────────────────────────────────────────

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
                // flu✗cut logo with long-press detection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            Toast.makeText(context,
                                "Stop poking me, I am just the logo.",
                                Toast.LENGTH_SHORT
                            ).show()
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

    // ── New Project Hero Card ─────────────────────────────────────────────────────
    data class AmbientParticle(
        val xFraction: Float,
        val yFraction: Float,
        val radius: Float,
        val alpha: Float
    )

    @Composable
    fun FloatingParticles(particleColor: Color) {
        // Reduced count to 7 particles for a strict, clean look
        val particles = remember {
            List(7) {
                AmbientParticle(
                    xFraction = Random.nextFloat(),
                    yFraction = Random.nextFloat(),
                    radius = Random.nextFloat() * 0.8f + 0.6f, // Tiny core dimensions (0.6dp to 1.4dp)
                    alpha = Random.nextFloat() * 0.3f + 0.3f
                )
            }
        }

        // Extended timeline to 12 seconds for an ultra-slow, ambient drift
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
                // Calculate raw vertical progress
                var currentYFraction = particle.yFraction - progress
                if (currentYFraction < 0f) {
                    currentYFraction += 1f
                }

                // Edge Fading Math: Force alpha to 0 when near the top (0.0) or bottom (1.0) boundaries
                val edgeAlpha = when {
                    currentYFraction < 0.2f -> currentYFraction / 0.2f
                    currentYFraction > 0.8f -> (1f - currentYFraction) / 0.2f
                    else -> 1f
                }

                val finalAlpha = particle.alpha * edgeAlpha
                val x = particle.xFraction * width
                val y = currentYFraction * height

                // 1. Outer Glow Layer (Wide and faint)
                drawCircle(
                    color = particleColor,
                    radius = (particle.radius * 5.0f).dp.toPx(),
                    center = Offset(x, y),
                    alpha = finalAlpha * 0.12f
                )

                // 2. Medium Halo Layer (Bridges core and outer glow)
                drawCircle(
                    color = particleColor,
                    radius = (particle.radius * 2.5f).dp.toPx(),
                    center = Offset(x, y),
                    alpha = finalAlpha * 0.35f
                )

                // 3. Sharp Solid Core
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
    fun NewProjectCard(accent: Color, dark: Boolean) {
        val backgroundImage =
            if (dark) R.drawable.new_project_bg_dark else R.drawable.new_project_bg
        val particleColor = if (dark) Color(0xFFA855F7) else Color.White

        // Define the color scheme for glow
        val glowColor = if (dark) Color(0xFFA855F7) else Color.White
        val iconBgColor = if (dark) Color(0xFF1C1C28) else Color(0xFFFFFFFF)

        // Dynamic horizontal gradient
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
                    Color(0xFF0F0F1A).copy(alpha = 0.75f), // Dark deep slate on the left protects white text
                    Color(0xFF6C63FF).copy(alpha = 0.25f)  // Translucent accent tint on the right lets the light asset pop
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { }
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


// ── Quick Actions ─────────────────────────────────────────────────────────────

    data class QuickAction(val icon: ImageVector, val label: String, val badge: String? = null)

    @Composable
    fun QuickActionsRow(surface: Color, onSurface: Color, subtle: Color, accent: Color) {
        val actions = listOf(
            QuickAction(Icons.Outlined.EmergencyRecording, "Capture"),
            QuickAction(Icons.Outlined.MusicNote, "Extract Audio"),
            QuickAction(
                Icons.Outlined.DeleteSweep,
                "Clear Cache",
                "3.2G"
            ), // High utility for storage management
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
                            .height(76.dp) // <-- Fixes the height layout bug by forcing strict uniformity
                            .clip(RoundedCornerShape(14.dp))
                            .background(surface)
                            .clickable { }
                            .padding(horizontal = 4.dp), // Prevents text from hitting the borders
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center // Centers elements vertically inside the fixed height
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
                            maxLines = 1, // Ensures text never wraps to a second line
                            lineHeight = 14.sp
                        )
                    }

                    // Badge
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

// ── Project Card ──────────────────────────────────────────────────────────────

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
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(project.thumbnailColor)
            )

            // Info
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

            // Menu
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "More",
                tint = subtle,
                modifier = Modifier.size(20.dp)
            )
        }
    }

// ── Bottom Nav ────────────────────────────────────────────────────────────────

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
                // Home
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

                // Center lightning bolt (elevated pill)
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

                // Profile
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
}
// ── Settings Screen ───────────────────────────────────────────────────────────

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

            // Top Navigation Bar
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

            // Settings Preferences List Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surface)
                    .padding(vertical = 8.dp)
            ) {
                // Preference Option 1: Video Export Quality
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

                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = bg, thickness = 1.dp)

                // Preference Option 2: Storage Location
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