package com.app.eroum.audiotestapp.ui.audio

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eroum.audiotestapp.data.model.BrowserItem
import com.app.eroum.audiotestapp.data.model.FolderItem
import com.app.eroum.audiotestapp.data.model.MusicItem
import com.app.eroum.audiotestapp.data.model.RepeatMode
import com.app.eroum.audiotestapp.data.model.TrackItem
import com.app.eroum.audiotestapp.data.model.UpItem
import com.app.eroum.audiotestapp.data.model.next
import com.app.eroum.audiotestapp.data.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AudioUiState(
    val isUsbMounted: Boolean = false,
    val usbRootPath: String = "",
    val usbState: String = "UNKNOWN",
    val isScanning: Boolean = false,

    val musicList: List<MusicItem> = emptyList(),
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val selectedIndex: Int = 0,

    // 파일 브라우저
    val currentDir: String = "",                 // relative dir ("", "Artist/Album")
    val browserItems: List<BrowserItem> = emptyList(),
)

private data class DirIndex(
    val childDirs: Map<String, Set<String>>,      // key: dir, value: immediate child dirs
    val tracksInDir: Map<String, List<MusicItem>> // key: dir, value: tracks directly in dir
)

open class AudioViewModel(
    private val repo: AudioRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AudioUiState())
    val ui: StateFlow<AudioUiState> = _ui

    private var monitorJob: Job? = null
    private var lastMounted: Boolean = false

    /** 브라우징 인덱스 */
    private var dirIndex: DirIndex? = null

    @RequiresApi(Build.VERSION_CODES.R)
    fun checkUsbNow() {
        viewModelScope.launch { refreshUsbStatusOnly() }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun onUsbBroadcast(action: String) {
        checkUsbNow()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun onUsbPlugin() {
        viewModelScope.launch {
            if (_ui.value.isScanning) return@launch
            _ui.value = _ui.value.copy(isScanning = true)

            val list = try {
                withContext(Dispatchers.IO) { repo.queryUsbAudioByFileSystem() }
            } catch (e: Exception) {
                Log.d("AudioViewModel", "[onUsbPlugin] scan error: $e")
                emptyList()
            }

            val root = _ui.value.usbRootPath // refreshUsbStatusOnly()에서 미리 채워짐
            dirIndex = buildDirIndex(root, list)

            _ui.value = _ui.value.copy(
                isUsbMounted = true,
                isScanning = false,
                musicList = list,
                selectedIndex = 0,
            )

            // 루트 브라우저 화면 열기
            openDir("")
        }
    }

    fun onUsbPlugOut() {
        dirIndex = null
        _ui.value = _ui.value.copy(
            isUsbMounted = false,
            usbRootPath = "",
            usbState = "NO_VOLUME",
            isScanning = false,
            musicList = emptyList(),
            selectedIndex = 0,
            currentDir = "",
            browserItems = emptyList(),
        )
    }

    fun toggleRepeatMode() {
        _ui.value = _ui.value.copy(repeatMode = _ui.value.repeatMode.next())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startUsbMonitor(intervalMs: Long = 1000L) {
        if (monitorJob != null) return
        monitorJob = viewModelScope.launch {
            while (isActive) {
                refreshUsbStatusOnly()
                delay(intervalMs)
            }
        }
    }

    fun stopUsbMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun refreshUsbStatusOnly() {
        val st = withContext(Dispatchers.IO) { repo.getUsbState() }

        _ui.value = _ui.value.copy(
            isUsbMounted = st.mounted,
            usbRootPath = st.rootPath ?: "",
            usbState = st.state ?: "NO_VOLUME"
        )

        if (!st.mounted) {
            if (lastMounted) onUsbPlugOut()
            lastMounted = false
            return
        }

        if (!lastMounted && st.rootFile != null) {
            onUsbPlugin()
        }
        lastMounted = true
    }

    // =========================
    //  Folder Browser
    // =========================

    fun onBrowserClick(item: BrowserItem) {
        when (item) {
            is UpItem -> openDir(item.parentDir)
            is FolderItem -> openDir(item.dir)
            is TrackItem -> selectTrackByPath(item.track.path)
        }
    }

    private fun selectTrackByPath(path: String) {
        val idx = _ui.value.musicList.indexOfFirst { it.path == path }
        if (idx >= 0) {
            _ui.value = _ui.value.copy(selectedIndex = idx)
            // TODO: 여기서 실제 재생 시작 로직 연결하면 됨
        }
    }

    private fun openDir(dir: String) {
        val idx = dirIndex ?: return

        val folders = idx.childDirs[dir].orEmpty()
            .sorted()
            .map { full ->
                FolderItem(name = full.substringAfterLast("/"), dir = full)
            }

        val tracks = idx.tracksInDir[dir].orEmpty()
            .sortedBy { it.title.lowercase() }
            .map { TrackItem(it) }

        val items = buildList {
            if (dir.isNotEmpty()) add(UpItem(parentDir(dir)))
            addAll(folders)
            addAll(tracks)
        }

        _ui.value = _ui.value.copy(
            currentDir = dir,
            browserItems = items
        )
    }

    private fun parentDir(dir: String): String =
        dir.substringBeforeLast("/", missingDelimiterValue = "")

    private fun buildDirIndex(root: String, tracks: List<MusicItem>): DirIndex {
        val childDirs = mutableMapOf<String, MutableSet<String>>()
        val tracksInDir = mutableMapOf<String, MutableList<MusicItem>>()

        fun normRelDir(path: String): String {
            val rel = if (root.isNotBlank() && path.startsWith(root)) {
                path.removePrefix(root)
            } else path
            val clean = rel.trimStart('/', '\\')
            val parts = clean.split('/', '\\')
            return parts.dropLast(1).joinToString("/") // file의 부모 dir (relative)
        }

        fun addDirChain(dir: String) {
            if (dir.isEmpty()) return
            val parts = dir.split("/")
            var parent = ""
            var cur = ""
            for (p in parts) {
                cur = if (cur.isEmpty()) p else "$cur/$p"
                childDirs.getOrPut(parent) { mutableSetOf() }.add(cur)
                parent = cur
            }
        }

        for (t in tracks) {
            val dir = normRelDir(t.path)
            addDirChain(dir)
            tracksInDir.getOrPut(dir) { mutableListOf() }.add(t)
        }

        return DirIndex(childDirs, tracksInDir)
    }

    // =========================
    // Player controls (TODO)
    // =========================
    fun playPrev() {}
    fun playNext() {}
    fun togglePlayPause() {}
    fun seekTo() {}
    fun selectTrack(index: Int) {}
}
