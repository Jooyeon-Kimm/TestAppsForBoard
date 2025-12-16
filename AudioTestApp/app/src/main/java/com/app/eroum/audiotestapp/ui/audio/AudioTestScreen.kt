package com.app.eroum.audiotestapp.ui.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.eroum.audiotestapp.data.model.BrowserItem
import com.app.eroum.audiotestapp.data.model.FolderItem
import com.app.eroum.audiotestapp.data.model.TrackItem
import com.app.eroum.audiotestapp.data.model.UpItem
import kotlin.math.max

// seekTo (ms) 인자 넘겨주기
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar
@Composable
fun AudioTestScreen(viewModel: AudioViewModel) {
    /** Field */
    val ui by viewModel.ui.collectAsState()
    val scrollState = rememberScrollState()

    /** UI */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Test App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isLandScape = maxHeight <= maxWidth
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPane(ui)
                Divider()

                if (isLandScape) {
                    var leftHeightPx by remember { mutableIntStateOf(0) }
                    val density = LocalDensity.current
                    val leftHeightDp = with(density) { leftHeightPx.toDp() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Pane
                        PlayerPane(
                            ui = ui,
                            onPrev = { viewModel.playPrev() },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() },
                            onSeek = { ms -> viewModel.seekTo() },
                            onToggleRepeat = { viewModel.toggleRepeatMode() },
                            modifier = Modifier
                                .weight(0.42f)
                                .onSizeChanged { leftHeightPx = it.height }
                        )

                        // Right Pane
                        BrowserPane(
                            ui = ui,
                            onClick = { item -> viewModel.onBrowserClick(item) },
                            modifier = Modifier
                                .weight(0.58f)
                                .then(
                                    if (leftHeightPx > 0) Modifier.height(leftHeightDp)
                                    else Modifier.heightIn(min = 320.dp)
                                )
                        )
                    }
                } else {
                    // 세로모드: 위(플레이어) + 아래(리스트)
                    PlayerPane(
                        ui = ui,
                        onPrev = { viewModel.playPrev() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() },
                        onSeek = { ms -> viewModel.seekTo() },
                        onToggleRepeat = { viewModel.toggleRepeatMode() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    BrowserPane(
                        ui = ui,
                        onClick = { item -> viewModel.onBrowserClick(item) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPane(ui: AudioUiState) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val textSize = 16.sp

            Text(text = "USB Mounted: ${ui.isUsbMounted}", fontSize = textSize)
            Text(text ="USB State: ${ui.usbState}", fontSize = textSize)
            Text(
                text = "USB Root: ${ui.usbRootPath}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = textSize
            )
            if (ui.isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning...")
                }
            }
            Text("Found: ${ui.musicList.size}", fontSize = textSize)
        }
    }
}

/**
 * [Left Pane] AlbumPane + ControlPane
 */
@Composable
private fun PlayerPane(
    ui: AudioUiState,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumPane(ui = ui, modifier = Modifier.fillMaxWidth())
        ControlPane(
            ui = ui,
            onPrev = onPrev,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onSeek = onSeek,
            onToggleRepeat = onToggleRepeat,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AlbumPane(ui: AudioUiState, modifier: Modifier = Modifier) {
    val current = ui.musicList.getOrNull(ui.selectedIndex)

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 앨범 이미지(placeholder) - 정사각형
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Text(
                text = current?.title ?: "No track selected",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = current?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 음악 재생 제어 Pane
 */
@Composable
private fun ControlPane(
    ui: AudioUiState,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = max(1L, ui.durationMs)
    val pos = ui.positionMs.coerceIn(0L, duration)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Slider(
            value = pos.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.toFloat()
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(pos), style = MaterialTheme.typography.labelMedium)
            Text(formatMs(duration), style = MaterialTheme.typography.labelMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev")
            }
            FilledIconButton(onClick = onPlayPause) {
                Icon(
                    if (ui.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "PlayPause"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = onToggleRepeat) {
                Icon(Icons.Default.Repeat, contentDescription = "Repeat")
            }
        }
    }
}

/**
 * 음악 파일 시스템 구조
 */
@Composable
private fun BrowserPane(
    ui: AudioUiState,
    onClick: (BrowserItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dirLabel = if (ui.currentDir.isBlank()) "/" else "/${ui.currentDir}"
                Text("USB Browser  $dirLabel", style = MaterialTheme.typography.titleMedium)
                Text("${ui.browserItems.size}", style = MaterialTheme.typography.labelLarge)
            }
            Divider()

            if (ui.browserItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items", style = MaterialTheme.typography.bodyMedium)
                }
                return@Column
            }

            val selectedPath = ui.musicList.getOrNull(ui.selectedIndex)?.path

            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = ui.browserItems,
                    key = { item ->
                        when (item) {
                            is UpItem -> "up:${item.parentDir}"
                            is FolderItem -> "dir:${item.dir}"
                            is TrackItem -> "track:${item.track.path}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is UpItem -> {
                            ListItem(
                                headlineContent = { Text("..") },
                                supportingContent = { Text("Up") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick(item) }
                            )
                        }

                        is FolderItem -> {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Folder",
                                    )
                                },
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text("Folder") },
                                colors = ListItemDefaults.colors(
                                    // containerColor = MaterialTheme.colorScheme.tertiaryContainer, // 목록 아이템 배경 색
                                    headlineColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    supportingColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    leadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,

                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick(item) }
                            )
                        }

                        is TrackItem -> {
                            val isSelected = selectedPath == item.track.path
                            ListItem(
                                headlineContent = {
                                    Text(item.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(item.track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick(item) }
                            )
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
