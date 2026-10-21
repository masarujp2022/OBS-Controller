package com.kongjjj.obscontroller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kongjjj.obscontroller.OBSFilter
import com.kongjjj.obscontroller.OBSInput
import kotlin.math.log10
import kotlin.math.roundToInt

private fun Float.toDbMeterProgress(): Float {
    if (this <= 0f) return 0f
    return ((20f * log10(this) + 60f) / 60f).coerceIn(0f, 1f)
}

// Volume range shown in the UI: -60 dB (silence) to +6 dB (slight gain)
private const val VOL_MIN = -60f
private const val VOL_MAX = 6f

@Composable
fun AudioScreen(
    inputs: List<OBSInput>,
    filters: List<OBSFilter>,
    volumeMeters: Map<String, List<Float>> = emptyMap(),
    filterMainMixerByScene: Boolean = false,
    currentScene: String = "",
    onToggleMute: (String) -> Unit,
    onVolumeChange: (String, Float) -> Unit,
    onLoadFilters: (sourceName: String) -> Unit,
    onAddFilter: (filterName: String, filterKind: String) -> Unit,
    onRemoveFilter: (filterName: String) -> Unit,
    onToggleFilter: (filterName: String, currentEnabled: Boolean) -> Unit,
    onSetFilterSettings: (filterName: String, partialSettings: Map<String, Any>) -> Unit
) {
    val audioInputs = inputs.filter { it.isAudio && it.active }

    // Which input's filters are being shown (null = sheet closed)
    var filterFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filterFor) {
        if (filterFor != null) onLoadFilters(filterFor!!)
    }

    if (filterFor != null) {
        FiltersSheet(
            sourceName = filterFor!!,
            filters = filters,
            onAddFilter = onAddFilter,
            onRemoveFilter = onRemoveFilter,
            onToggleFilter = onToggleFilter,
            onSetFilterSettings = onSetFilterSettings,
            onDismiss = { filterFor = null }
        )
    }

    if (inputs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (audioInputs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No audio sources found in OBS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val title = if (filterMainMixerByScene && currentScene.isNotEmpty()) "Audio Mixer - $currentScene" else "Audio Mixer"
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(audioInputs, key = { it.name }) { input ->
            AudioInputCard(
                input = input,
                peaks = volumeMeters[input.name] ?: emptyList(),
                onToggleMute = { onToggleMute(input.name) },
                onVolumeChange = { db -> onVolumeChange(input.name, db) },
                onOpenFilters = { filterFor = input.name }
            )
        }
    }
}

@Composable
private fun AudioInputCard(
    input: OBSInput,
    peaks: List<Float> = emptyList(),
    onToggleMute: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenFilters: () -> Unit
) {
    // Local slider state to avoid jumpy UI while dragging
    var sliderValue by remember(input.name) {
        mutableFloatStateOf(input.volumeDb.coerceIn(VOL_MIN, VOL_MAX))
    }

    // Sync when OBS pushes an update from elsewhere
    LaunchedEffect(input.volumeDb) {
        sliderValue = input.volumeDb.coerceIn(VOL_MIN, VOL_MAX)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (input.muted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row: name + filter button + mute button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = input.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = input.kind,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onOpenFilters) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (input.muted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (input.muted) "Unmute" else "Mute",
                        tint = if (input.muted)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Volume slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${sliderValue.roundToInt()} dB",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(52.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onVolumeChange(sliderValue) },
                    valueRange = VOL_MIN..VOL_MAX,
                    enabled = !input.muted,
                    modifier = Modifier.weight(1f)
                )
            }

            // Peak meters (one bar per channel)
            if (peaks.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    peaks.forEach { peak ->
                        LinearProgressIndicator(
                            progress = { peak.toDbMeterProgress() },
                            modifier = Modifier.weight(1f).height(4.dp),
                            color = when {
                                peak > 0.5f -> MaterialTheme.colorScheme.error   // > -6 dBFS
                                peak > 0.1f -> Color(0xFFFFB300)                 // > -20 dBFS
                                else        -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
