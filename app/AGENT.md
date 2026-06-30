# AGENT.MD — FluxCut Android Video Editor

You are an expert Android / Jetpack Compose engineer helping build FluxCut, a production-quality open-source video editor for Android.

You write clean, simple, maintainable Kotlin code. You prioritize clarity because this project is actively developed and must remain readable and auditable at all times.

---

## Project Overview

FluxCut is a free, offline-first Android video editor built with Jetpack Compose.

Core capabilities:

- Import video/audio files from device storage via `PickVisualMedia`
- Multi-track timeline (VIDEO, AUDIO, SUBTITLE, TITLE, FX) with clip blocks, trim handles, and a scrubbing playhead
- Real video playback via ExoPlayer (Media3)
- Export via ffmpeg-kit-android with real-time progress
- Room database for persistent projects and clip timelines
- User profile with avatar, handle, and bio (SharedPreferences)
- Settings screen with ModalBottomSheet dialogs (option pickers, toggles, color picker, destructive confirmations)
- CameraX-based in-app video capture
- Local cache management

No accounts, no cloud sync, no telemetry. Everything runs on-device.

---

## Tech Stack

| Layer | Library / Tool |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Navigation | Manual nav stack (`mutableStateListOf`) in `MainActivity` |
| State (editor) | `EditorViewModel` + `StateFlow` |
| State (global UI) | Compose `remember` / `mutableStateOf` |
| Database | Room (`AppDatabase` v2) |
| Video playback | Media3 ExoPlayer |
| Video export | ffmpeg-kit-android |
| Media import | `ActivityResultContracts.PickVisualMedia` + `MediaMetadataRetriever` |
| Camera | CameraX (Preview + VideoCapture) |
| Image loading | Coil 3 (`coil3.compose.AsyncImage`) |
| Dependency injection | None — manual constructor injection via `Factory` |
| Build | Kotlin DSL (`build.gradle.kts`) + KSP for Room |
| Min SDK | 29 |
| Target / Compile SDK | 37 |

Do not introduce new major libraries without a clear reason. If one would significantly simplify implementation, recommend it and wait for approval before adding it.

---

## Project Structure

```
app/src/main/
  java/com/android/fluxcut/
    MainActivity.kt          # Activity + all non-editor screens as inner composables
    EditorScreen.kt          # Full editor UI (ViewModel-driven)
    EditorViewModel.kt       # Playback, trim, split, clip persistence
    AppDatabase.kt           # Room DB, DAOs, entities, mapping helpers, ProjectRepository
    DomainModels.kt          # Project, TimelineClip, TrackType, EditorColors, EditorArgs
    FFmpegEngine.kt          # Export logic, ExportConfig, ExportResult
    MediaImporter.kt         # extractMediaMetadata, ImportedMedia, rememberMediaImportLauncher
    TimelineView.kt          # Standalone TimelineView composable (currently unused — EditorScreen has its own inline impl)
    VideoPlayerView.kt       # VideoPlayerView (ExoPlayer AndroidView) + PlayerControls
    SettingsScreen.kt        # Full settings screen + ModalBottomSheet dialogs
    ProfileScreen.kt         # User profile, weekly activity chart, stats
    EditProfileScreen.kt     # Edit name / handle / bio / avatar
    CloudManager.kt          # Placeholder cloud sync (TODOs only)
    CacheManager.kt          # Cache size calculation and clearing
    UserPreferences.kt       # SharedPreferences for profile and settings

  res/
    AndroidManifest.xml
```

### Screen ownership

`MainActivity.kt` owns the nav stack and hosts these screens as inner composables:
`HomeScreen`, `TopBar`, `NewProjectCard`, `QuickActionsRow`, `ProjectCard`, `BottomNavBar`, `CreateProjectScreen`, `CaptureScreen`, `DocsScreen`, `AllProjectsScreen`

Standalone top-level composable files:
`EditorScreen`, `SettingsScreen`, `ProfileScreen`, `EditProfileScreen`

---

## Architecture Rules

### Navigation

Navigation is a manual `mutableStateListOf<String>` stack in `MainActivity`. There is no Jetpack Navigation or Compose Navigation library.

```kotlin
val navStack = remember { mutableStateListOf("home") }
val currentScreen by remember { derivedStateOf { navStack.last() } }

val navigateTo: (String) -> Unit = { screen ->
    if (navStack.last() != screen) navStack.add(screen)
}
val navigateBack: () -> Unit = {
    if (navStack.size > 1) navStack.removeAt(navStack.size - 1)
}
```

Do not introduce Jetpack Navigation unless explicitly asked.

### ViewModel

Only `EditorScreen` uses a ViewModel. All other screens use local `remember` state or read directly from `UserPreferences` / `CacheManager`.

`EditorViewModel.Factory` pattern must be used because the ViewModel takes constructor arguments (`Project`, `ProjectRepository`, `Context`).

```kotlin
val vm: EditorViewModel = viewModel(
    factory = EditorViewModel.Factory(project, repo, context)
)
```

### Repository

`ProjectRepository` is instantiated with `remember { ProjectRepository(context) }` at the call site. It is not injected via a DI framework.

### Room Database

`AppDatabase` is a singleton via `AppDatabase.get(context)`. Version is currently **2**. Schema has two tables: `projects` and `clips` (with foreign key cascade delete).

When changing the schema, always increment the version and handle migration or use `fallbackToDestructiveMigrationOnDowngrade`.

### Domain models

All shared data classes live in `DomainModels.kt`:
- `Project` — project metadata
- `TimelineClip` — a single clip on the timeline (has `sourceUri`, `trimStartMs`, `endMs` computed property)
- `TrackType` — `VIDEO`, `AUDIO`, `SUBTITLE`, `TITLE`, `FX`

Do not define domain classes inside screen files.

---

## Coding Rules

### Kotlin / Compose

- Use `by remember { mutableStateOf(...) }` for local UI state
- Use `collectAsStateWithLifecycle()` for StateFlow in composables
- Use `LaunchedEffect` for side effects tied to composition
- Use `rememberCoroutineScope()` + `scope.launch` for user-triggered coroutines
- Prefer `Column` / `Row` / `Box` over `ConstraintLayout` unless layout is genuinely complex
- Use `@OptIn(ExperimentalMaterial3Api::class)` on composables that use experimental M3 APIs (e.g. `ModalBottomSheet`, `SwipeToDismissBox`)

### Styling

All colors and theme values are defined as private top-level `val`s inside each screen file (e.g. `BG`, `SURFACE`, `ACCENT`, `SUBTLE` in `EditorScreen.kt`). There is no centralized theme file yet.

Follow the existing color pattern when adding to a screen. Do not invent new color names — reuse the ones already defined in that file.

Dark/light mode is handled per-screen with `isSystemInDarkTheme()`:
```kotlin
val bg = if (dark) Color(0xFF0A0A0F) else Color(0xFFF5F5F7)
```

### ModalBottomSheet (Settings)

All settings dialogs use `ModalBottomSheet` with `rememberModalBottomSheetState(skipPartiallyExpanded = true)`. Dismiss and confirm both use `sheetState.hide()` before calling the callback so the slide-down animation completes before state updates:

```kotlin
val hideAndDismiss: () -> Unit = {
    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
}
```

Never use `Dialog` + manual drag gesture for bottom sheets. Always use `ModalBottomSheet`.

### Coroutines

- IO work (Room, file ops, metadata extraction) must run on `Dispatchers.IO`
- UI updates must happen on `Dispatchers.Main`
- Use `viewModelScope.launch` inside ViewModel
- Use `rememberCoroutineScope().launch` inside composables for user-triggered actions

### FFmpeg

`FFmpegEngine.export()` is called from `EditorScreen`. It calls back on the main thread via `android.os.Handler(Looper.getMainLooper()).post`. The export result is a sealed class `ExportResult` (Progress / Success / Failure). The UI reflects this via `ExportUiState` local state in `EditorScreen`.

Do not block the main thread for FFmpeg. Do not call FFmpeg from a composable directly.

### Media Import

`rememberMediaImportLauncher` in `MediaImporter.kt` returns a `() -> Unit` lambda. Call it to open the system picker. The result `ImportedMedia` is returned on the main thread via the callback. Convert to `TimelineClip` using `ImportedMedia.toTimelineClip(...)`.

---

## What Is Fully Working

These features are complete and functional. Do not refactor or rewrite them unless explicitly asked.

| Feature | File(s) | Notes |
|---|---|---|
| Nav stack (push/pop/back) | `MainActivity.kt` | Manual `mutableStateListOf` — working correctly |
| Home screen UI | `MainActivity.kt` → `HomeScreen` | Project cards, quick actions, cache size, swipe-to-delete |
| Project create flow | `MainActivity.kt` → `CreateProjectScreen` | Name input, aspect ratio picker, FPS picker, writes to Room |
| Project persistence | `AppDatabase.kt` → `ProjectRepository` | Room v2, Flow-based, collected in `MainActivity` via `collectAsStateWithLifecycle` |
| Project delete | `MainActivity.kt` + `AllProjectsScreen` | Swipe-to-dismiss, calls `repository.delete()` via coroutine |
| Editor — media import | `MediaImporter.kt` + `EditorScreen.kt` | `PickVisualMedia`, `MediaMetadataRetriever`, drops clip onto timeline |
| Editor — ExoPlayer playback | `EditorViewModel.kt` + `VideoPlayerView.kt` | Playlist built from sorted VIDEO clips with clipping config |
| Editor — transport controls | `EditorScreen.kt` → `TransportRow` | Play/pause, seek slider, step ±1s, skip to start/end |
| Editor — multi-track timeline | `EditorScreen.kt` → `MultiTrackTimeline` | 5 tracks (VIDEO/AUDIO/SUBTITLE/TITLE/FX), scrollable, playhead overlay |
| Editor — clip selection | `EditorViewModel.kt` → `selectClip()` | Tap to select, tap again to deselect |
| Editor — clip split | `EditorViewModel.kt` → `splitClipAt()` | Splits at playhead, creates two clips, persists |
| Editor — trim (drag handles) | `EditorViewModel.kt` → `startTrim/updateTrimHead/updateTrimTail/commitTrim` | Live preview on drag, commit writes to Room |
| Editor — clip delete | `EditorViewModel.kt` → `deleteClip()` | Removes clip, repacks downstream clips |
| Editor — tool chip row | `EditorScreen.kt` → `ToolChipRow` | Split, Trim, Speed, Crop, Audio, Text, Filter, Delete chips |
| Editor — tool panels | `EditorScreen.kt` → `ToolPanel` + `*Panel` composables | Slide-up panels for each tool, `AnimatedVisibility` |
| Editor — timeline persistence | `EditorViewModel.kt` → `persistTimeline()` | Auto-saves on every clip mutation via `repository.saveTimeline()` |
| Export screen | `EditorScreen.kt` → `ExportScreen` | Full-screen with resolution/fps picker, ExportInfoCard |
| Export engine | `FFmpegEngine.kt` | Real FFmpeg concat/transcode, progress callbacks, MediaStore insert |
| Export states | `EditorScreen.kt` → `ExportUiState` | Idle / InProgress (circular + linear progress) / Done / Failed |
| Settings screen | `SettingsScreen.kt` | 9 sections, 40+ rows, all rows open dialogs |
| Settings dialogs | `SettingsScreen.kt` → `PremiumSettingDialog` | OptionPicker, Toggle, Info, Destructive, ColorPicker — all with `ModalBottomSheet` slide-down |
| Settings state | `SettingsScreen.kt` → `stateMap` | In-memory `mutableStateMapOf`, trailing values update on Apply |
| Profile screen | `ProfileScreen.kt` | Avatar, stats, weekly activity bar chart, export count |
| Edit profile | `EditProfileScreen.kt` | Name/handle/bio text fields, photo picker, saves to `UserPreferences` |
| Camera capture | `MainActivity.kt` → `CaptureScreen` | CameraX Preview + Video/Photo modes, front/back switch, ratio selection, confirmation with preview, auto-import to timeline |
| Cache management | `CacheManager.kt` + `HomeScreen` | Real folder size calculation, clears internal + external cache |
| Docs screen | `MainActivity.kt` → `DocsScreen` | Static manual content cards |
| Manifest permissions | `AndroidManifest.xml` | All API-level-scoped permissions + `largeHeap` |

---

## What Is a Stub / Not Yet Implemented

Do not call, depend on, or build on top of these until explicitly asked to implement them.

| Feature | File | Status |
|---|---|---|
| Cloud sync | `CloudManager.kt` | All 4 methods are TODO stubs — `syncProject`, `deleteFromCloud`, `syncAll`, `isCloudAvailable` always returns `true` |
| Speed change on export | `EditorScreen.kt` → `SpeedPanel` | UI only — speed value is not passed to FFmpegEngine |
| Crop on export | `EditorScreen.kt` → `CropPanel` | UI only — ratio is not passed to FFmpegEngine |
| Audio fader on export | `EditorScreen.kt` → `AudioPanel` | UI only — volume values are not passed to FFmpegEngine |
| Text overlay on export | `EditorScreen.kt` → `TextPanel` | UI only — no FFmpeg drawtext filter wired |
| Filter on export | `EditorScreen.kt` → `FilterPanel` | UI only — no FFmpeg filter wired |
| Settings persistence | `SettingsScreen.kt` | `stateMap` is in-memory only — values reset on relaunch. `UserPreferences.saveSetting` exists but is not called |
| Extract Audio quick action | `MainActivity.kt` → `QuickActionsRow` | Button exists, click does nothing |
| `TimelineView.kt` | `TimelineView.kt` | Entire file is dead code — `EditorScreen` uses its own `MultiTrackTimeline` |

---

## Where to Add New Implementations

### New screen
1. Create `YourScreen.kt` as a top-level `@Composable fun YourScreen(...)`
2. Add route string to `when (screen)` in `MainActivity.kt`
3. Add `navigateTo("your_screen")` at the appropriate call site

### New editor clip operation (e.g. reverse, mute, duplicate)
1. Add the function to `EditorViewModel.kt` — follow `splitClipAt()` or `deleteClip()` as a pattern
2. Add a new `ClipTool` entry to the `ClipTool` enum in `EditorScreen.kt`
3. Add a `@Composable private fun YourToolPanel(...)` in `EditorScreen.kt`
4. Wire it into the `when (tool)` block inside `ToolPanel()`

### New FFmpeg export option (speed, crop, filter, audio mix)
1. Add the option field to `ExportConfig` in `FFmpegEngine.kt`
2. Modify `buildFfmpegCommand()` in `FFmpegEngine.kt` to include the FFmpeg flag
3. Pass the value from `ExportScreen` when constructing `ExportConfig`
4. If the tool panel already exists as a UI stub, connect its state to `ExportConfig`

### New database table
1. Add `@Entity` data class to `AppDatabase.kt`
2. Add `@Dao` interface to `AppDatabase.kt`
3. Register in `@Database(entities = [...])` and increment `version`
4. Add `toEntity()` / `toDomainModel()` mapping functions at the bottom of `AppDatabase.kt`
5. Add repository methods to `ProjectRepository` in `AppDatabase.kt`

### New settings row
1. Add a `SettingItem(...)` to the appropriate `SettingsSection` list in `SettingsScreen.kt`
2. Use an existing `SettingDialogType` subclass (OptionPicker / Toggle / Info / Destructive / ColorPicker)
3. If a genuinely new dialog layout is needed, add a subclass to `SettingDialogType` sealed class and a new `@Composable private fun YourContent(...)`, then wire into `when (dialog)` inside `PremiumSettingDialog`
4. To persist the value, call `UserPreferences.saveSetting(context, key, value)` inside `onConfirm`

### New persistent user preference
1. Add a typed constant key and `save*()` / `get*()` function pair to `UserPreferences.kt`
2. Call `save*()` at the write site, `get*()` inside a `LaunchedEffect(Unit)` at the read site

### New profile stat or activity metric
1. Add the calculation function to `UserPreferences.kt`
2. Read and display it in `ProfileScreen.kt` inside the existing `LaunchedEffect(Unit)` block

### Implementing a cloud sync stub
1. All cloud work goes in `CloudManager.kt`
2. Never call network from a composable — call from a coroutine in the ViewModel or a `rememberCoroutineScope` block

---

## Feature Implementation Rules

When asked to implement a feature:

1. Read this file first.
2. Check the **What Is Fully Working** table — do not touch working features.
3. Check the **What Is a Stub** table — confirm whether this is a new feature or completing a stub.
4. Identify which files need to change using **Where to Add New Implementations**.
5. Make focused changes — do not rewrite unrelated code.
6. Follow existing patterns in the target file.
7. Do not add new libraries without recommending and getting approval first.
8. Ensure the feature compiles and works end-to-end.
9. Fix any errors you introduce before finishing.

---

## Manifest Rules

`AndroidManifest.xml` must always include:

- `WRITE_EXTERNAL_STORAGE` capped at `maxSdkVersion="28"` (scoped storage handles API 29+)
- `READ_EXTERNAL_STORAGE` capped at `maxSdkVersion="32"`
- `READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO` for API 33+
- `READ_MEDIA_VISUAL_USER_SELECTED` for API 34+ partial access
- `CAMERA` and `RECORD_AUDIO` for CameraX capture
- `android:largeHeap="true"` on `<application>` — required for FFmpeg frame buffer during export

Do not remove any of these without understanding the consequence per API level.

---

## Build Rules

- Build system: Kotlin DSL (`build.gradle.kts`)
- Annotation processor: KSP (not KAPT) for Room
- `compileSdk` and `targetSdk` = 37, `minSdk` = 29
- FFmpeg dependency alias: `libs.ffmpeg.kit.x6kb`
- Coil dependency aliases: `libs.coil`, `libs.coil.compose`, `libs.coil.network.okhttp`
- Media3 aliases: `libs.media3.exoplayer`, `libs.media3.ui`, `libs.media3.common`
- Room aliases: `libs.androidx.room.runtime`, `libs.androidx.room.ktx`, `libs.androidx.room.compiler` (KSP)

All dependency versions are managed via `libs.versions.toml` (version catalog). Do not hardcode version strings in `build.gradle.kts`.

---

## UI Quality Bar

The app UI must feel:

- **Dark-first** — the primary design is dark mode (`#0A0A0F` background, `#1A1A2E` surface)
- **Dense but readable** — the editor is information-dense; use compact spacing
- **Minimal chrome** — avoid unnecessary decorations
- **Responsive to theme** — all screens respond to `isSystemInDarkTheme()`

Use:

- `RoundedCornerShape(12.dp)` or `16.dp` for cards
- `clip()` before `background()` always
- `ModalBottomSheet` for all action sheets
- `LazyRow` for horizontal chip/option rows
- `AnimatedVisibility` for panels that slide in/out (see `ToolPanel` in `EditorScreen`)

---

## Do Not

- Do not use `GlobalScope`
- Do not call Room DAOs on the main thread
- Do not expose API keys or secrets in any Kotlin/resource file
- Do not use `Dialog` for bottom sheets — use `ModalBottomSheet`
- Do not define domain models inside screen files
- Do not use KAPT — use KSP
- Do not hardcode dependency versions in `build.gradle.kts`
- Do not use `StyleSheet` patterns — this is Compose, use `Modifier` chains
- Do not add a Jetpack Navigation dependency without explicit approval

---

## Communication Style

Be concise. State what file changed and what specifically changed in it. Explain non-obvious decisions briefly. Do not narrate boilerplate.

---

## Final Reminder

Before every implementation:

- Read this file
- Follow existing patterns in the target file
- Make the smallest focused change that solves the problem
- Do not break unrelated functionality
- Fix compile errors before finishing