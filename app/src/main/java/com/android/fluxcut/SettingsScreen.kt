package com.android.fluxcut

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class SettingDialogType {
    data class OptionPicker(
        val title: String,
        val subtitle: String,
        val options: List<String>,
        val current: String,
        val accentColor: Color
    ) : SettingDialogType()

    data class Toggle(
        val title: String,
        val subtitle: String,
        val description: String,
        val current: Boolean,
        val accentColor: Color
    ) : SettingDialogType()

    data class Info(
        val title: String,
        val body: String,
        val accentColor: Color
    ) : SettingDialogType()

    data class Destructive(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val accentColor: Color
    ) : SettingDialogType()

    data class ColorPicker(
        val title: String,
        val subtitle: String,
        val colors: List<Pair<String, Color>>,
        val current: String,
        val accentColor: Color
    ) : SettingDialogType()
}

data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val trailingValue: String? = null,
    val isDestructive: Boolean = false,
    val dialog: SettingDialogType? = null
)

data class SettingsSection(
    val sectionTitle: String,
    val accentColor: Color,
    val items: List<SettingItem>
)

@Composable
fun SettingsScreen(navigateTo: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val locale = LocalConfiguration.current.locales[0]

    val bg        = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
    val surface   = if (dark) Color(0xFF1A1A2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle    = if (dark) Color(0xFF666680) else Color(0xFF888888)
    val divider   = if (dark) Color(0xFF232338) else Color(0xFFEEEEF5)

    val videoAccent  = Color(0xFF6C63FF)
    val audioAccent  = Color(0xFF00BFA5)
    val systemAccent = Color(0xFFFFB300)
    val dangerAccent = Color(0xFFFF4D6D)

    val stateMap = remember {
        mutableStateMapOf(
            "Default Frame Rate"        to "30 fps",
            "Default Aspect Ratio"      to "9:16",
            "Ripple Edit"               to "On",
            "Snapping"                  to "On",
            "Default Clip Duration"     to "5s",
            "Preview Quality"           to "720p",
            "Background Decode"         to "On",
            "Proxy Mode"                to "Off",
            "Proxy Cache Limit"         to "2 GB",
            "Export Codec"              to "H.264",
            "Default Export Resolution" to "1080p",
            "Color Space"               to "sRGB",
            "HDR Output"                to "Off",
            "Bitrate Mode"              to "VBR",
            "Video Stabilization"       to "Off",
            "Master Volume"             to "100%",
            "Sample Rate"               to "44.1 kHz",
            "Default Fade Length"       to "0.3s",
            "Audio Ducking"             to "Off",
            "Default EQ Preset"         to "Flat",
            "Noise Reduction"           to "Off",
            "Export Destination"        to "Gallery",
            "Auto-Import on Capture"    to "On",
            "Auto Backup Projects"      to "On",
            "Appearance"                to "System",
            "Accent Color"              to "Purple",
            "Haptic Feedback"           to "On",
            "Language"                  to "English",
            "Hardware Acceleration"     to "On",
            "RAM Limit"                 to "512 MB",
            "Battery Saver Mode"        to "On",
            "Thermal Throttle"          to "On",
        )
    }

    fun s(key: String) = stateMap[key] ?: ""

    val sections = listOf(
        SettingsSection("Timeline", videoAccent, listOf(
            SettingItem(Icons.Outlined.Speed, "Default Frame Rate", "Target FPS for new projects", s("Default Frame Rate"),
                dialog = SettingDialogType.OptionPicker("Default Frame Rate", "Target FPS for new projects",
                    listOf("24 fps", "30 fps", "60 fps", "120 fps"), s("Default Frame Rate"), videoAccent)),
            SettingItem(Icons.Outlined.AspectRatio, "Default Aspect Ratio", "Canvas ratio for new timelines", s("Default Aspect Ratio"),
                dialog = SettingDialogType.OptionPicker("Default Aspect Ratio", "Canvas ratio for new timelines",
                    listOf("16:9", "9:16", "1:1", "4:5", "21:9", "4:3"), s("Default Aspect Ratio"), videoAccent)),
            SettingItem(Icons.Outlined.LinearScale, "Ripple Edit", "Shift downstream clips on trim", s("Ripple Edit"),
                dialog = SettingDialogType.Toggle("Ripple Edit", "Shift downstream clips on trim",
                    "Automatically shift downstream clips when trimming.", s("Ripple Edit") == "On", videoAccent)),
            SettingItem(Icons.Outlined.GridOn, "Snapping", "Snap clips to playhead and markers", s("Snapping"),
                dialog = SettingDialogType.Toggle("Snapping", "Snap clips to playhead and markers",
                    "Magnetically snap to edges and markers.", s("Snapping") == "On", videoAccent)),
            SettingItem(Icons.Outlined.Timer, "Default Clip Duration", "For image and text clips", s("Default Clip Duration"),
                dialog = SettingDialogType.OptionPicker("Default Clip Duration", "Duration for new image/text clips",
                    listOf("2s", "3s", "5s", "8s", "10s"), s("Default Clip Duration"), videoAccent)),
        )),
        SettingsSection("Preview", videoAccent, listOf(
            SettingItem(Icons.Outlined.Hd, "Preview Quality", "Resolution used during scrubbing", s("Preview Quality"),
                dialog = SettingDialogType.OptionPicker("Preview Quality", "Resolution during scrubbing",
                    listOf("360p", "480p", "720p", "1080p", "Native"), s("Preview Quality"), videoAccent)),
            SettingItem(Icons.Outlined.Cached, "Background Decode", "Pre-render frames while idle", s("Background Decode"),
                dialog = SettingDialogType.Toggle("Background Decode", "Pre-render frames while idle",
                    "Pre-decode frames in the background for smoother playback.", s("Background Decode") == "On", videoAccent)),
            SettingItem(Icons.Outlined.Layers, "Proxy Mode", "Use low-res proxy files", s("Proxy Mode"),
                dialog = SettingDialogType.Toggle("Proxy Mode", "Use low-res proxy files",
                    "Generate proxy files for heavy 4K timelines.", s("Proxy Mode") == "On", videoAccent)),
            SettingItem(Icons.Outlined.Memory, "Proxy Cache Limit", "Max disk space for proxy files", s("Proxy Cache Limit"),
                dialog = SettingDialogType.OptionPicker("Proxy Cache Limit", "Max space for proxy files",
                    listOf("512 MB", "1 GB", "2 GB", "4 GB", "8 GB"), s("Proxy Cache Limit"), videoAccent)),
        )),
        SettingsSection("Video", videoAccent, listOf(
            SettingItem(Icons.Outlined.Tune, "Export Codec", "Encoder used for final render", s("Export Codec"),
                dialog = SettingDialogType.OptionPicker("Export Codec", "Encoder used for final render",
                    listOf("H.264", "H.265 (HEVC)", "VP9", "AV1"), s("Export Codec"), videoAccent)),
            SettingItem(Icons.Outlined.HighQuality, "Default Export Resolution", "Output resolution for exports", s("Default Export Resolution"),
                dialog = SettingDialogType.OptionPicker("Default Export Resolution", "Default export resolution",
                    listOf("720p", "1080p", "1440p", "4K"), s("Default Export Resolution"), videoAccent)),
            SettingItem(Icons.Outlined.PhotoFilter, "Color Space", "Working color profile", s("Color Space"),
                dialog = SettingDialogType.OptionPicker("Color Space", "Project color space",
                    listOf("sRGB", "Display P3", "Rec. 709", "Rec. 2020"), s("Color Space"), videoAccent)),
            SettingItem(Icons.Outlined.BrightnessMedium, "HDR Output", "Encode in HDR if supported", s("HDR Output"),
                dialog = SettingDialogType.Toggle("HDR Output", "Encode in HDR if supported",
                    "Export using HDR10 when supported.", s("HDR Output") == "On", videoAccent)),
            SettingItem(Icons.Outlined.Compress, "Bitrate Mode", "CBR or VBR", s("Bitrate Mode"),
                dialog = SettingDialogType.OptionPicker("Bitrate Mode", "Bitrate control mode",
                    listOf("CBR", "VBR", "CRF"), s("Bitrate Mode"), videoAccent)),
            SettingItem(Icons.Outlined.CameraRoll, "Video Stabilization", "Apply stabilization on import", s("Video Stabilization"),
                dialog = SettingDialogType.Toggle("Video Stabilization", "Apply stabilization on import",
                    "Uses gyro data for stabilization.", s("Video Stabilization") == "On", videoAccent)),
        )),
        SettingsSection("Audio", audioAccent, listOf(
            SettingItem(Icons.Outlined.VolumeUp, "Master Volume", "Default output gain", s("Master Volume"),
                dialog = SettingDialogType.OptionPicker("Master Volume", "Default project gain",
                    listOf("60%", "75%", "90%", "100%", "110%", "125%"), s("Master Volume"), audioAccent)),
            SettingItem(Icons.Outlined.GraphicEq, "Sample Rate", "Processing sample rate", s("Sample Rate"),
                dialog = SettingDialogType.OptionPicker("Sample Rate", "Audio sample rate",
                    listOf("22.05 kHz", "44.1 kHz", "48 kHz", "96 kHz"), s("Sample Rate"), audioAccent)),
            SettingItem(Icons.Outlined.MusicNote, "Default Fade Length", "Auto fade on clip edges", s("Default Fade Length"),
                dialog = SettingDialogType.OptionPicker("Default Fade Length", "Automatic fade length",
                    listOf("0s", "0.1s", "0.3s", "0.5s", "1.0s", "2.0s"), s("Default Fade Length"), audioAccent)),
            SettingItem(Icons.Outlined.HeadsetMic, "Audio Ducking", "Lower music when voice is active", s("Audio Ducking"),
                dialog = SettingDialogType.Toggle("Audio Ducking", "Lower music when voice is active",
                    "Automatically reduces music volume during narration.", s("Audio Ducking") == "On", audioAccent)),
            SettingItem(Icons.Outlined.Equalizer, "Default EQ Preset", "Applied to new clips", s("Default EQ Preset"),
                dialog = SettingDialogType.OptionPicker("Default EQ Preset", "Default equalizer preset",
                    listOf("Flat", "Voice Boost", "Bass Boost", "Podcast", "Cinema"), s("Default EQ Preset"), audioAccent)),
            SettingItem(Icons.Outlined.RecordVoiceOver, "Noise Reduction", "Reduce background noise", s("Noise Reduction"),
                dialog = SettingDialogType.Toggle("Noise Reduction", "Reduce background noise",
                    "Adaptive noise reduction for voice tracks.", s("Noise Reduction") == "On", audioAccent)),
        )),
        SettingsSection("Import & Export", systemAccent, listOf(
            SettingItem(Icons.Outlined.FolderOpen, "Media Locations", "Source file search paths",
                dialog = SettingDialogType.Info("Media Locations",
                    "Scanned locations:\n• /Movies\n• /DCIM\n• /Downloads", systemAccent)),
            SettingItem(Icons.Outlined.SaveAlt, "Export Destination", "Default save folder", s("Export Destination"),
                dialog = SettingDialogType.OptionPicker("Export Destination", "Where rendered videos are saved",
                    listOf("Gallery", "Downloads", "App Folder", "Custom"), s("Export Destination"), systemAccent)),
            SettingItem(Icons.Outlined.ImageSearch, "Auto-Import on Capture", "Add captured clips to project", s("Auto-Import on Capture"),
                dialog = SettingDialogType.Toggle("Auto-Import on Capture", "Add captured clips to project",
                    "Clips recorded in-app are added to timeline automatically.", s("Auto-Import on Capture") == "On", systemAccent)),
            SettingItem(Icons.Outlined.Archive, "Auto Backup Projects", "Periodic project snapshots", s("Auto Backup Projects"),
                dialog = SettingDialogType.Toggle("Auto Backup Projects", "Periodic project snapshots",
                    "Creates snapshots every 5 minutes.", s("Auto Backup Projects") == "On", systemAccent)),
        )),
        SettingsSection("Interface", systemAccent, listOf(
            SettingItem(Icons.Outlined.DarkMode, "Appearance", "System or forced theme", s("Appearance"),
                dialog = SettingDialogType.OptionPicker("Appearance", "App color scheme",
                    listOf("System", "Light", "Dark", "AMOLED Black"), s("Appearance"), systemAccent)),
            SettingItem(Icons.Outlined.Palette, "Accent Color", "UI highlight color", s("Accent Color"),
                dialog = SettingDialogType.ColorPicker("Accent Color", "App accent color",
                    listOf(
                        "Purple"    to Color(0xFF6C63FF),
                        "Teal"      to Color(0xFF00BFA5),
                        "Amber"     to Color(0xFFFFB300),
                        "Rose"      to Color(0xFFFF4D6D),
                        "Sky"       to Color(0xFF0EA5E9),
                        "Lime"      to Color(0xFF84CC16),
                    ), s("Accent Color"), systemAccent)),
            SettingItem(Icons.Outlined.Vibration, "Haptic Feedback", "Vibrate on events", s("Haptic Feedback"),
                dialog = SettingDialogType.Toggle("Haptic Feedback", "Vibrate on events",
                    "Vibration on snapping and cuts.", s("Haptic Feedback") == "On", systemAccent)),
            SettingItem(Icons.Outlined.Language, "Language", "App display language", s("Language"),
                dialog = SettingDialogType.OptionPicker("Language", "Display language",
                    listOf("English", "Tamil", "Hindi", "Spanish", "French", "German", "Japanese"), s("Language"), systemAccent)),
        )),
        SettingsSection("Performance", systemAccent, listOf(
            SettingItem(Icons.Outlined.Hardware, "Hardware Acceleration", "Use GPU for rendering", s("Hardware Acceleration"),
                dialog = SettingDialogType.Toggle("Hardware Acceleration", "Use GPU for rendering",
                    "Uses MediaCodec hardware paths.", s("Hardware Acceleration") == "On", systemAccent)),
            SettingItem(Icons.Outlined.Memory, "RAM Limit", "Timeline buffer size", s("RAM Limit"),
                dialog = SettingDialogType.OptionPicker("RAM Limit", "Memory for frame buffer",
                    listOf("256 MB", "512 MB", "1 GB", "2 GB"), s("RAM Limit"), systemAccent)),
            SettingItem(Icons.Outlined.BatteryFull, "Battery Saver Mode", "Conserve power", s("Battery Saver Mode"),
                dialog = SettingDialogType.Toggle("Battery Saver Mode", "Conserve power",
                    "Reduces preview quality when battery is low.", s("Battery Saver Mode") == "On", systemAccent)),
        )),
        SettingsSection("Advanced", dangerAccent, listOf(
            SettingItem(Icons.Outlined.Refresh, "Reset All Settings", "Restore factory defaults", null, isDestructive = true,
                dialog = SettingDialogType.Destructive("Reset All Settings",
                    "This will restore every setting to factory default.",
                    "Reset Settings", dangerAccent)),
            SettingItem(Icons.Outlined.DeleteForever, "Clear All Cache", "Delete all temporary files", null, isDestructive = true,
                dialog = SettingDialogType.Destructive("Clear All Cache",
                    "Permanently delete proxy and preview caches.",
                    "Clear Cache", dangerAccent)),
        )),
        SettingsSection("About", videoAccent, listOf(
            SettingItem(Icons.Outlined.Info, "Version", "Build info", "v1.0.0-alpha",
                dialog = SettingDialogType.Info("About FluxCut",
                    "FluxCut v1.0.0-alpha\nBuild: 2026.06.23", videoAccent)),
            SettingItem(Icons.Outlined.FavoriteBorder, "Contribute on GitHub", "Open source development",
                dialog = SettingDialogType.Info("Contribute on GitHub",
                    "github.com/android/fluxcut", videoAccent)),
        )),
    )

    var activeDialog by remember { mutableStateOf<SettingDialogType?>(null) }

    Scaffold(containerColor = bg) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = onSurface,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { navigateTo() }
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurface)
                            Text("Customize your editing environment", fontSize = 12.sp, color = subtle)
                        }
                    }
                }
            }

            sections.forEach { section ->
                item {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(14.dp)
                                .background(section.accentColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            section.sectionTitle.uppercase(locale),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = section.accentColor,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(surface)
                    ) {
                        section.items.forEachIndexed { index, item ->
                            SettingRow(
                                item      = item,
                                accent    = section.accentColor,
                                onSurface = onSurface,
                                subtle    = subtle,
                                currentValue = stateMap[item.title] ?: item.trailingValue,
                                onRowClick = { activeDialog = item.dialog }
                            )
                            if (index < section.items.lastIndex) {
                                HorizontalDivider(
                                    color = divider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 56.dp, end = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    activeDialog?.let { dialog ->
        PremiumSettingDialog(
            dialog = dialog,
            onDismiss = { activeDialog = null },
            onConfirm = { newValue ->
                when (dialog) {
                    is SettingDialogType.OptionPicker -> stateMap[dialog.title] = newValue
                    is SettingDialogType.Toggle       -> stateMap[dialog.title] = if (newValue == "true") "On" else "Off"
                    is SettingDialogType.ColorPicker  -> stateMap[dialog.title] = newValue
                    else                              -> { }
                }
                activeDialog = null
            }
        )
    }
}

@Composable
fun SettingRow(
    item: SettingItem,
    accent: Color,
    onSurface: Color,
    subtle: Color,
    currentValue: String?,
    onRowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.isDestructive) Color(0xFFFF4D6D) else accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.isDestructive) Color(0xFFFF4D6D) else onSurface
            )
            Text(
                text = item.subtitle,
                fontSize = 11.sp,
                color = subtle,
                lineHeight = 15.sp
            )
        }

        if (currentValue != null) {
            Text(
                text = currentValue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.isDestructive) Color(0xFFFF4D6D) else accent
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = subtle.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PremiumSettingDialog(
    dialog: SettingDialogType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val surface   = if (dark) Color(0xFF1C1C2E) else Color(0xFFFFFFFF)
    val onSurface = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111)
    val subtle    = if (dark) Color(0xFF888899) else Color(0xFF888888)
    val divider   = if (dark) Color(0xFF2A2A3E) else Color(0xFFEEEEF5)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val hideAndDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    val hideAndConfirm: (String) -> Unit = { value ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { onConfirm(value) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = subtle.copy(alpha = 0.4f)
            )
        }
    ) {
        when (dialog) {
            is SettingDialogType.OptionPicker -> OptionPickerContent(
                dialog, surface, onSurface, subtle, divider, hideAndDismiss, hideAndConfirm
            )
            is SettingDialogType.Toggle -> ToggleContent(
                dialog, surface, onSurface, subtle, divider, hideAndDismiss, hideAndConfirm
            )
            is SettingDialogType.Info -> InfoContent(
                dialog, onSurface, subtle, hideAndDismiss
            )
            is SettingDialogType.Destructive -> DestructiveContent(
                dialog, onSurface, subtle, divider, hideAndDismiss, hideAndConfirm
            )
            is SettingDialogType.ColorPicker -> ColorPickerContent(
                dialog, onSurface, subtle, divider, hideAndDismiss, hideAndConfirm
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun OptionPickerContent(
    dialog: SettingDialogType.OptionPicker,
    surface: Color,
    onSurface: Color,
    subtle: Color,
    divider: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(dialog.current) }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dialog.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = dialog.accentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(dialog.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text(dialog.subtitle, fontSize = 13.sp, color = subtle, lineHeight = 18.sp)
    }

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = divider, thickness = 0.5.dp)

    dialog.options.forEachIndexed { index, option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected == option, onClick = { selected = option })
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(option, fontSize = 15.sp, color = onSurface, modifier = Modifier.weight(1f))
            if (selected == option) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Selected",
                    tint = dialog.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.5.dp, subtle.copy(alpha = 0.3f), CircleShape)
                )
            }
        }
        if (index < dialog.options.lastIndex) {
            HorizontalDivider(color = divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 24.dp))
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, subtle.copy(alpha = 0.3f))
        ) {
            Text("Cancel", color = subtle, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = { onConfirm(selected) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = dialog.accentColor)
        ) {
            Text("Apply", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ToggleContent(
    dialog: SettingDialogType.Toggle,
    surface: Color,
    onSurface: Color,
    subtle: Color,
    divider: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var checked by remember { mutableStateOf(dialog.current) }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dialog.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ToggleOn, contentDescription = null, tint = dialog.accentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(dialog.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text(dialog.subtitle, fontSize = 13.sp, color = subtle, lineHeight = 18.sp)
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(dialog.accentColor.copy(alpha = 0.07f))
            .clickable { checked = !checked }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (checked) "Enabled" else "Disabled",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) dialog.accentColor else subtle
            )
            Text(
                if (checked) "Tap to turn off" else "Tap to turn on",
                fontSize = 12.sp,
                color = subtle
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = dialog.accentColor,
                uncheckedThumbColor = subtle,
                uncheckedTrackColor = subtle.copy(alpha = 0.2f)
            )
        )
    }

    Spacer(Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(subtle.copy(alpha = 0.08f))
            .padding(16.dp)
    ) {
        Text(dialog.description, fontSize = 13.sp, color = subtle, lineHeight = 19.sp)
    }

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, subtle.copy(alpha = 0.3f))
        ) {
            Text("Cancel", color = subtle, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = { onConfirm(if (checked) "true" else "false") },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = dialog.accentColor)
        ) {
            Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun InfoContent(
    dialog: SettingDialogType.Info,
    onSurface: Color,
    subtle: Color,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dialog.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = dialog.accentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(dialog.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(subtle.copy(alpha = 0.08f))
                .padding(16.dp)
        ) {
            Text(dialog.body, fontSize = 13.sp, color = onSurface, lineHeight = 20.sp)
        }
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = dialog.accentColor)
    ) {
        Text("Got it", fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun DestructiveContent(
    dialog: SettingDialogType.Destructive,
    onSurface: Color,
    subtle: Color,
    divider: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(dialog.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = dialog.accentColor, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(dialog.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = dialog.accentColor, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(dialog.message, fontSize = 13.sp, color = subtle, lineHeight = 19.sp, textAlign = TextAlign.Center)
    }

    Spacer(Modifier.height(24.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { onConfirm("confirmed") },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = dialog.accentColor)
        ) {
            Text(dialog.confirmLabel, fontWeight = FontWeight.Bold, color = Color.White)
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, subtle.copy(alpha = 0.3f))
        ) {
            Text("Keep Current Settings", color = subtle, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ColorPickerContent(
    dialog: SettingDialogType.ColorPicker,
    onSurface: Color,
    subtle: Color,
    divider: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(dialog.current) }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dialog.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Palette, contentDescription = null, tint = dialog.accentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(dialog.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text(dialog.subtitle, fontSize = 13.sp, color = subtle)
    }

    Spacer(Modifier.height(20.dp))

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dialog.colors.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (name, color) ->
                    val isSelected = selected == name
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = if (isSelected) 0.18f else 0.08f))
                            .border(
                                if (isSelected) 2.dp else 0.dp,
                                if (isSelected) color else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selected = name }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) color else subtle)
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, subtle.copy(alpha = 0.3f))
        ) {
            Text("Cancel", color = subtle, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = { onConfirm(selected) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = dialog.colors.find { it.first == selected }?.second ?: dialog.accentColor
            )
        ) {
            Text("Apply", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
