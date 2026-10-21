package com.kongjjj.obscontroller.ui

import android.graphics.BitmapFactory
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kongjjj.obscontroller.OBSFilter
import com.kongjjj.obscontroller.OBSScene
import com.kongjjj.obscontroller.OBSSceneItem

private const val TAG = "ScenesScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesScreen(
    scenes: List<OBSScene>,
    currentScene: String,
    previewScene: String,
    studioModeEnabled: Boolean,
    streamActive: Boolean,
    recordActive: Boolean,
    recordingTimeSec: Long = 0,
    programScreenshot: String?,
    previewScreenshot: String?,
    onSceneClick: (String) -> Unit,
    onTransition: () -> Unit,
    onCut: () -> Unit,
    onToggleStudioMode: () -> Unit,
    onToggleStream: () -> Unit,
    onToggleRecord: () -> Unit,
    onCreateScene: (String) -> Unit = {},
    onAddSource: (inputName: String, inputKind: String) -> Unit,
    onPreviewVisibilityChange: (Boolean) -> Unit = {},
    sceneItems: Map<String, List<OBSSceneItem>> = emptyMap(),
    groupItems: Map<String, List<OBSSceneItem>> = emptyMap(),
    onLoadSceneItems: (String) -> Unit = {},
    onToggleSceneItemVisibility: (sceneItemId: Int, currentEnabled: Boolean) -> Unit = { _, _ -> },
    onDeleteSceneItem: (sceneItemId: Int) -> Unit = {},
    onLoadGroupItems: (groupName: String) -> Unit = {},
    onToggleGroupItemVisibility: (groupName: String, sceneItemId: Int, currentEnabled: Boolean) -> Unit = { _, _, _ -> },
    onDeleteGroupItem: (groupName: String, sceneItemId: Int) -> Unit = { _, _ -> },
    filters: List<OBSFilter> = emptyList(),
    onLoadFilters: (String) -> Unit = {},
    onAddFilter: (filterName: String, filterKind: String) -> Unit = { _, _ -> },
    onRemoveFilter: (filterName: String) -> Unit = {},
    onToggleFilter: (filterName: String, currentEnabled: Boolean) -> Unit = { _, _ -> },
    onSetFilterSettings: (filterName: String, partialSettings: Map<String, Any>) -> Unit = { _, _ -> },
    inputSettings: Map<String, Any>? = null,
    onLoadInputSettings: (sourceName: String) -> Unit = {},
    onSetInputSettings: (sourceName: String, partialSettings: Map<String, Any>) -> Unit = { _, _ -> },
    onReorderSceneItem: (sceneItemId: Int, newIndex: Int) -> Unit = { _, _ -> },
    sceneCollections: List<String> = emptyList(),
    currentSceneCollection: String = "",
    onSetSceneCollection: (String) -> Unit = {},
    showCollectionChip: Boolean = true
) {
    val view = LocalView.current

    var showAddScene by remember { mutableStateOf(false) }
    var showAddSource by remember { mutableStateOf(false) }
    var sourcesSheetScene by remember { mutableStateOf<String?>(null) }
    var filterForSource by remember { mutableStateOf<String?>(null) }
    var settingsForSource by remember { mutableStateOf<Pair<String, String>?>(null) }
    var confirmStream by remember { mutableStateOf(false) }
    var confirmRecord by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }

    if (confirmStream) {
        AlertDialog(
            onDismissRequest = { confirmStream = false },
            title = { Text(if (streamActive) "終止直播?" else "開始直播?") },
            text = {
                Text(if (streamActive) "是否終止現在的直播?"
                     else "是否開始直播?")
            },
            confirmButton = {
                Button(
                    onClick = { onToggleStream(); confirmStream = false },
                    colors = if (streamActive) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) { Text(if (streamActive) "終止直播" else "開始直播") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStream = false }) { Text("取消") }
            }
        )
    }

    if (confirmRecord) {
        AlertDialog(
            onDismissRequest = { confirmRecord = false },
            title = { Text(if (recordActive) "終止錄影?" else "開始錄影?") },
            text = {
                Text(if (recordActive) "是否終止現在的錄影?"
                     else "是否開始錄影?")
            },
            confirmButton = {
                Button(
                    onClick = { onToggleRecord(); confirmRecord = false },
                    colors = if (recordActive) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) { Text(if (recordActive) "終止錄影" else "開始錄影") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRecord = false }) { Text("取消") }
            }
        )
    }

    if (showCollectionPicker && sceneCollections.size > 1) {
        AlertDialog(
            onDismissRequest = { showCollectionPicker = false },
            title = { Text("Scene Collection") },
            text = {
                Column {
                    sceneCollections.forEach { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetSceneCollection(collection)
                                    showCollectionPicker = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = collection == currentSceneCollection,
                                onClick = {
                                    onSetSceneCollection(collection)
                                    showCollectionPicker = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(collection, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCollectionPicker = false }) { Text("關閉") }
            }
        )
    }

    LaunchedEffect(filterForSource) {
        if (filterForSource != null) onLoadFilters(filterForSource!!)
    }

    LaunchedEffect(settingsForSource) {
        if (settingsForSource != null) onLoadInputSettings(settingsForSource!!.first)
    }

    if (showAddScene) {
        AddSceneDialog(
            onAdd = { name ->
                onCreateScene(name)
                showAddScene = false
            },
            onDismiss = { showAddScene = false }
        )
    }

    if (showAddSource) {
        AddSourceDialog(
            currentSceneName = currentScene,
            onAdd = { name, kind ->
                onAddSource(name, kind)
                showAddSource = false
            },
            onDismiss = { showAddSource = false }
        )
    }

    if (sourcesSheetScene != null) {
        SceneItemsSheet(
            sceneName = sourcesSheetScene!!,
            sceneItems = sceneItems[sourcesSheetScene!!] ?: emptyList(),
            groupItems = groupItems,
            onToggleVisibility = onToggleSceneItemVisibility,
            onDelete = onDeleteSceneItem,
            onToggleGroupItemVisibility = onToggleGroupItemVisibility,
            onDeleteGroupItem = onDeleteGroupItem,
            onLoadGroupItems = onLoadGroupItems,
            onOpenFilters = { sourceName -> filterForSource = sourceName },
            onOpenSettings = { sourceName, sourceKind -> settingsForSource = sourceName to sourceKind },
            onReorderSceneItem = onReorderSceneItem,
            onDismiss = { sourcesSheetScene = null }
        )
    }

    if (settingsForSource != null) {
        SourceSettingsSheet(
            sourceName = settingsForSource!!.first,
            sourceKind = settingsForSource!!.second,
            settings = inputSettings,
            onSetSettings = { partial -> onSetInputSettings(settingsForSource!!.first, partial) },
            onDismiss = { settingsForSource = null }
        )
    }

    if (filterForSource != null) {
        FiltersSheet(
            sourceName = filterForSource!!,
            filters = filters,
            filterTypes = VIDEO_FILTER_TYPES,
            onAddFilter = onAddFilter,
            onRemoveFilter = onRemoveFilter,
            onToggleFilter = onToggleFilter,
            onSetFilterSettings = onSetFilterSettings,
            onDismiss = { filterForSource = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(onClick = { showAddScene = true }) {
                    Icon(Icons.Default.Movie, contentDescription = "新增場景")
                }
                FloatingActionButton(onClick = { showAddSource = true }) {
                    Icon(Icons.Default.Add, contentDescription = "加入來源")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Stream + Studio mode control bar ──────────────────────────────
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stream + Record buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (streamActive) {
                            Button(
                                onClick = { confirmStream = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop stream", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("終止直播")
                            }
                        } else {
                            Button(onClick = { confirmStream = true }) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = "Go live",
                                    modifier = Modifier.size(16.dp), tint = Color.Red)
                                Spacer(Modifier.width(4.dp))
                                Text("開始直播")
                            }
                        }

                        if (recordActive) {
                            Button(
                                onClick = { confirmRecord = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop recording", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("終止錄影")
                                    if (recordingTimeSec > 0) {
                                        Text(
                                            text = "● %02d:%02d".format(recordingTimeSec / 60, recordingTimeSec % 60),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { confirmRecord = true }) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = "Start recording",
                                    modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(4.dp))
                                Text("錄影")
                            }
                        }
                    }

                    // Studio mode toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("工作室", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = studioModeEnabled,
                            onCheckedChange = { onToggleStudioMode() }
                        )
                    }
                }
            }

            // ── Studio mode split view ────────────────────────────────────────
            if (studioModeEnabled) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SceneBox(
                            label = "預覽",
                            sceneName = previewScene,
                            screenshot = previewScreenshot,
                            labelColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        SceneBox(
                            label = "節目",
                            sceneName = currentScene,
                            screenshot = programScreenshot,
                            labelColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTransition,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("轉場")
                        }
                        OutlinedButton(
                            onClick = onCut,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("直接切換")
                        }
                    }
                }

                HorizontalDivider()
            }

            // ── Scene cards ───────────────────────────────────────────────────
            if (scenes.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val activeScene = if (studioModeEnabled) previewScene else currentScene

                // Scene collection chip (shown when multiple collections exist)
                if (showCollectionChip && sceneCollections.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        AssistChip(
                            onClick = { showCollectionPicker = true },
                            label = {
                                Text(
                                    currentSceneCollection.ifEmpty { "Collection" },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                // Program thumbnail when studio mode OFF
                if (!studioModeEnabled) {
                    var previewVisible by remember { mutableStateOf(true) }
                    var previewPaused by remember { mutableStateOf(false) }
                    LaunchedEffect(previewVisible, previewPaused) {
                        onPreviewVisibilityChange(previewVisible && !previewPaused)
                    }

                    if (previewVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            ScreenshotImage(
                                base64 = programScreenshot,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            // Overlay bar at the bottom of the preview image
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable { previewVisible = false }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "LIVE: $currentScene",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (streamActive) MaterialTheme.colorScheme.error
                                            else Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { previewPaused = !previewPaused },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (previewPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (previewPaused) "Resume preview" else "Pause preview",
                                        tint = if (previewPaused) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Hide preview",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    } else {
                        // Collapsed: slim row to re-expand
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { previewVisible = true }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "LIVE: $currentScene",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (streamActive) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Show preview",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Scene grid ───────────────────────────────────────────────
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(scenes) { scene ->
                            val isActive = scene.name == activeScene
                            val isProgram = scene.name == currentScene
                            SceneCard(
                                scene = scene,
                                isActive = isActive,
                                isProgram = isProgram,
                                studioModeEnabled = studioModeEnabled,
                                onClick = {
                                    onSceneClick(scene.name)
                                    view.performHapticFeedback(
                                        HapticFeedbackConstants.CLOCK_TICK,
                                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                                    )
                                },
                                onShowSources = {
                                    sourcesSheetScene = scene.name
                                    onLoadSceneItems(scene.name)
                                }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun SceneCard(
    scene: OBSScene,
    isActive: Boolean,
    isProgram: Boolean,
    studioModeEnabled: Boolean,
    onClick: () -> Unit,
    onShowSources: () -> Unit
) {
    val borderColor = when {
        isProgram && studioModeEnabled -> MaterialTheme.colorScheme.error
        isActive -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val label = when {
        isProgram && studioModeEnabled -> "On Air"
        isActive && studioModeEnabled -> "Preview"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive || isProgram) 2.dp else 0.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label ?: "",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (label != null) borderColor else Color.Transparent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = scene.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                color = if (isActive)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onShowSources,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "View sources",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneBox(
    label: String,
    sceneName: String,
    screenshot: String?,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
        Spacer(Modifier.height(4.dp))
        ScreenshotImage(
            base64 = screenshot,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = sceneName.ifEmpty { "—" },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ScreenshotImage(base64: String?, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = remember(base64) {
        base64 ?: return@remember null
        try {
            val clean = if (',' in base64) base64.substringAfter(",") else base64
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode screenshot bitmap", e)
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (base64 == null) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}
