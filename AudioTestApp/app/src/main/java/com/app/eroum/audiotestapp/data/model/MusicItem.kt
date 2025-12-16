package com.app.eroum.audiotestapp.data.model

import android.net.Uri

data class MusicItem(
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String = "",
    val path: String,
    val durationMs: Long? = null,
    val volumeName: String? = null,
)

sealed interface BrowserItem { val key: String }

data class UpItem(val parentDir: String) : BrowserItem { override val key = "__UP__" }
data class FolderItem(val name: String, val dir: String) : BrowserItem { override val key = "D:$dir" }
data class TrackItem(val track: MusicItem) : BrowserItem { override val key = "F:${track.path}" }

