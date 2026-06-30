package com.android.fluxcut

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun ProfileScreen(
    projects: List<Project>,
    onSettingsClick: () -> Unit,
    onDocsClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dark = isSystemInDarkTheme()

    val bg             = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface        = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val surface2       = if (dark) Color(0xFF23233A) else Color(0xFFEAE9F2)
    val onSurface      = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle         = if (dark) Color(0xFF666680) else Color(0xFF888888)
    val divider        = if (dark) Color(0xFF232338) else Color(0xFFEEEEF5)
    val accent         = Color(0xFF6C63FF)
    val teal           = Color(0xFF00BFA5)
    val amber          = Color(0xFFFFB300)

    var cacheSizeState by remember { mutableStateOf("...") }
    var userName by remember { mutableStateOf("") }
    var userHandle by remember { mutableStateOf("") }
    var userPhotoUri by remember { mutableStateOf("") }
    var totalExports by remember { mutableIntStateOf(0) }
    var activityData by remember { mutableStateOf(List(7) { 0f }) }

    LaunchedEffect(Unit) {
        cacheSizeState = CacheManager.getCacheSize(context)
        val profileData = UserPreferences.getProfile(context)
        userName = profileData.name
        userHandle = profileData.handle
        userPhotoUri = profileData.photoUri
        totalExports = UserPreferences.getExportCount(context)
        activityData = UserPreferences.calculateWeeklyActivity(projects)
    }

    val buildVersion = "v1.0-α"

    Scaffold(containerColor = bg) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Column {
                        Text(
                            "Profile",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        Text(
                            "Your local workspace",
                            fontSize = 13.sp,
                            color = subtle
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(surface)
                            .border(1.dp, divider, RoundedCornerShape(12.dp))
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(surface)
                        .border(1.dp, divider, RoundedCornerShape(20.dp))
                ) {
                    FilmStripHeader(
                        accent = accent,
                        dark = dark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditProfileClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(accent.copy(0.3f), surface2)
                                            )
                                        )
                                        .border(2.dp, accent.copy(0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (userPhotoUri.isNotEmpty()) {
                                        AsyncImage(
                                            model = userPhotoUri,
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = userName.take(1).uppercase(locale),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = onSurface
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                        .border(2.dp, surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )
                                Text(
                                    text = userHandle,
                                    fontSize = 13.sp,
                                    color = subtle
                                )
                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(teal.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Code,
                                        contentDescription = null,
                                        tint = teal,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        "FOSS Contributor",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = teal
                                    )
                                }
                            }

                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = subtle,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = divider, thickness = 0.5.dp)
                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileStat(
                                value = projects.size.toString(),
                                label = "Projects",
                                color = accent
                            )
                            ProfileStat(
                                value = totalExports.toString(),
                                label = "Exports",
                                color = accent
                            )
                            ProfileStat(
                                value = cacheSizeState,
                                label = "Cache",
                                color = amber
                            )
                            ProfileStat(
                                value = buildVersion,
                                label = "Build",
                                color = teal
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surface)
                        .border(1.dp, divider, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.BarChart,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Editing Activity",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurface
                            )
                        }
                        Text("Last 7 days", fontSize = 11.sp, color = subtle)
                    }

                    Spacer(Modifier.height(12.dp))
                    ActivityHeatmapBar(accent = accent, subtle = subtle, activityData = activityData)
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surface)
                        .border(1.dp, divider, RoundedCornerShape(16.dp))
                ) {
                    ProfileMenuRow(
                        icon = Icons.Outlined.Storage,
                        label = "Manage Storage",
                        value = cacheSizeState,
                        accent = accent,
                        onSurface = onSurface,
                        subtle = subtle,
                        divider = divider,
                        showDivider = true
                    ) {
                        val cleared = CacheManager.clearAllCache(context)
                        if (cleared) {
                            cacheSizeState = CacheManager.getCacheSize(context)
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ProfileMenuRow(
                        icon = Icons.Outlined.CloudUpload,
                        label = "Cloud Backup",
                        value = "Not Connected",
                        accent = accent,
                        onSurface = onSurface,
                        subtle = subtle,
                        divider = divider,
                        showDivider = true
                    ) {
                        Toast.makeText(context, "Cloud sync coming soon", Toast.LENGTH_SHORT).show()
                    }
                    ProfileMenuRow(
                        icon = Icons.Outlined.Hardware,
                        label = "Device Diagnostics",
                        value = android.os.Build.MODEL,
                        accent = teal,
                        onSurface = onSurface,
                        subtle = subtle,
                        divider = divider,
                        showDivider = true
                    ) {
                        Toast.makeText(context, "CPU: ${android.os.Build.HARDWARE}", Toast.LENGTH_SHORT).show()
                    }
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        label = "Documentation",
                        value = null,
                        accent = amber,
                        onSurface = onSurface,
                        subtle = subtle,
                        divider = divider,
                        showDivider = true
                    ) { onDocsClick() }
                    ProfileMenuRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        label = "Contribute on GitHub",
                        value = null,
                        accent = accent,
                        onSurface = onSurface,
                        subtle = subtle,
                        divider = divider,
                        showDivider = false
                    ) {
                        Toast.makeText(context, "github.com/SathiyaSenpai", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("</> ", fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
                        Text(
                            "FluxCut $buildVersion · GNU GPLv3",
                            fontSize = 11.sp,
                            color = subtle,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        "Built by the community, for the community",
                        fontSize = 11.sp,
                        color = subtle.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun FilmStripHeader(
    accent: Color,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (dark) Color(0xFF12122A) else Color(0xFFE8E7FF)

    Canvas(modifier = modifier.background(bgColor)) {
        val perfW  = 10.dp.toPx()
        val perfH  = 16.dp.toPx()
        val gap    = 14.dp.toPx()
        val marginY = (size.height - perfH) / 2f
        val cornerR = 2.dp.toPx()

        var x = gap
        while (x < size.width - perfW) {
            drawRoundRect(
                color = accent.copy(alpha = 0.25f),
                topLeft = Offset(x, marginY * 0.3f),
                size = Size(perfW, perfH * 0.55f),
                cornerRadius = CornerRadius(cornerR)
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.25f),
                topLeft = Offset(x, size.height - marginY * 0.3f - perfH * 0.55f),
                size = Size(perfW, perfH * 0.55f),
                cornerRadius = CornerRadius(cornerR)
            )
            x += perfW + gap
        }
    }
}

@Composable
fun ActivityHeatmapBar(accent: Color, subtle: Color, activityData: List<Float>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEachIndexed { i, day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.08f + (activityData.getOrElse(i) { 0f } * 0.7f)))
                )
                Text(
                    day,
                    fontSize = 9.sp,
                    color = subtle,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.6f))
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    value: String?,
    accent: Color,
    onSurface: Color,
    subtle: Color,
    divider: Color,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }

            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = onSurface,
                modifier = Modifier.weight(1f)
            )

            if (value != null) {
                Text(value, fontSize = 12.sp, color = subtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = subtle.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 62.dp, end = 16.dp)
            )
        }
    }
}
