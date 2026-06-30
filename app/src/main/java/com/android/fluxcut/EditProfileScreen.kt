package com.android.fluxcut

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dark = isSystemInDarkTheme()

    val bg = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle = if (dark) Color(0xFF666680) else Color(0xFF888888)
    val divider = if (dark) Color(0xFF232338) else Color(0xFFEEEEF5)
    val accent = Color(0xFF6C63FF)

    val currentProfile = remember { UserPreferences.getProfile(context) }

    var nameInput by remember { mutableStateOf(currentProfile.name) }
    var handleInput by remember { mutableStateOf(currentProfile.handle) }
    var bioInput by remember { mutableStateOf(currentProfile.bio) }
    var photoUri by remember { mutableStateOf(currentProfile.photoUri) }

    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "profile_avatar.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                photoUri = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = bg,
        modifier = Modifier.imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Cancel",
                        tint = onSurface,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBackClick() }
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurface)
                }
                Text(
                    text = "Save",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.clickable {
                        val finalHandle = if (handleInput.startsWith("@")) handleInput else "@$handleInput"
                        UserPreferences.saveProfile(
                            context,
                            nameInput.trim(),
                            finalHandle.replace(" ", ""),
                            bioInput.trim(),
                            photoUri
                        )
                        onBackClick()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(surface)
                            .border(2.dp, divider, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.isNotEmpty()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = nameInput.take(1).uppercase(locale),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .border(3.dp, bg, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProfileTextField(label = "Display Name", value = nameInput, onValueChange = { nameInput = it }, surface = surface, accent = accent, onSurface = onSurface, subtle = subtle)
                ProfileTextField(label = "Username", value = handleInput, onValueChange = { handleInput = it }, surface = surface, accent = accent, onSurface = onSurface, subtle = subtle)
                ProfileTextField(label = "Bio", value = bioInput, onValueChange = { bioInput = it }, surface = surface, accent = accent, onSurface = onSurface, subtle = subtle, singleLine = false, maxLines = 4)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    surface: Color,
    accent: Color,
    onSurface: Color,
    subtle: Color,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = subtle)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth().then(if (!singleLine) Modifier.heightIn(min = 100.dp) else Modifier),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = surface,
                focusedContainerColor = surface,
                unfocusedContainerColor = surface,
                focusedTextColor = onSurface,
                unfocusedTextColor = onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
