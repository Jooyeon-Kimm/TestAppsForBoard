package com.app.eroum.audiotestapp.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.app.eroum.audiotestapp.data.model.MusicItem
import java.io.File
import kotlin.io.path.fileVisitor

open class AudioRepository(
    private val context: Context,
) {
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager


    data class UsbStorageState(
        val mounted: Boolean,
        val state: String?,
        val uuid: String?,
        val rootFile: File?,
        val rootPath: String?,
    )

    @RequiresApi(Build.VERSION_CODES.R)
    fun getUsbState(): UsbStorageState {
        val vol = storageManager.storageVolumes
            .firstOrNull { it.isRemovable && !it.isEmulated }

        val state = vol?.state
        val mounted = state.equals(Environment.MEDIA_MOUNTED, ignoreCase = true)
        val dir = if (mounted) vol?.directory else null

        return UsbStorageState(
            mounted = mounted,
            state = state,
            uuid = vol?.uuid,
            rootFile = dir,
            rootPath = dir?.absolutePath
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun findUsbRoot(): File? {
        val mediaRw = File("/mnt/media_rw")
        if (!mediaRw.exists() || !mediaRw.isDirectory) return null

        return mediaRw.listFiles()
            ?.firstOrNull { it.isDirectory && it.canRead() }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun queryUsbAudioByFileSystem(): List<MusicItem> {
        val usbRoot = getUsbState().rootFile ?: findUsbRoot() ?: return emptyList()
        val list = mutableListOf<MusicItem>()
        val files = usbRoot.listFiles()
        Log.e("USB", "listFiles = ${files?.size}")

        files?.forEach {
            Log.e("USB", "file=${it.absolutePath}, canRead=${it.canRead()}")
        }

        usbRoot.walkTopDown()
            .filter { it.isFile }
            .filter {
                val ext = it.extension.lowercase()
                ext == "mp3" || ext == "wav" || ext == "flac" || ext == "aac"
            }
            .forEach { file ->
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(file.absolutePath)

                    val title =
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.nameWithoutExtension

                    val artist =
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: "Unknown"

                    val album =
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?: ""

                    val duration =
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L

                    list.add(
                        MusicItem(
                            uri = file.toUri(),
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            volumeName = "USB",
                            path = file.absolutePath,
                        )
                    )
                } catch (_: Exception) {
                } finally {
                    mmr.release()
                }
            }

        return list.sortedBy { it.title.lowercase() }
    }
}