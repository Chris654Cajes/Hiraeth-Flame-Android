package com.hiraeth.flame.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hiraeth.flame.data.local.MediaStorage
import com.hiraeth.flame.data.local.VideoTrimmer
import com.hiraeth.flame.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VideoEditorViewModel(
    private val repository: MediaRepository,
    private val storage: MediaStorage,
    private val mediaId: Long,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun clearStatus() {
        _status.value = null
    }

    /**
     * Writes a trimmed copy into [MediaStorage.videosDir] and registers it as new media.
     * Trim uses video track only; see [VideoTrimmer] KDoc for audio limitations.
     */
    fun exportTrim(startFraction: Float, endFraction: Float, onRegistered: (Long) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val entity = repository.getById(mediaId) ?: error("Missing media")
                if (!entity.isVideo) error("Not a video")
                val input = repository.resolveFile(entity)
                val outFile = File(storage.videosDir, "trim_${UUID.randomUUID()}.mp4")
                val retriever = android.media.MediaMetadataRetriever()
                val durationUs = try {
                    retriever.setDataSource(input.absolutePath)
                    val ms = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    ms * 1000L
                } finally {
                    retriever.release()
                }
                val duration = durationUs.toDouble()
                val startUs = (duration * startFraction.toDouble().coerceIn(0.0, 1.0)).toLong()
                val endUs = (duration * endFraction.toDouble().coerceIn(0.0, 1.0)).toLong()
                    .coerceAtLeast(startUs + 50_000L)
                VideoTrimmer.trimVideoTrackToFile(input, outFile, startUs, endUs)
                val newId = repository.registerCapturedVideo(outFile)
                _status.value = "Trim saved as new clip"
                onRegistered(newId)
            }.onFailure {
                _status.value = it.message ?: "Trim failed"
            }
            _busy.value = false
        }
    }

    companion object {
        fun factory(
            repository: MediaRepository,
            storage: MediaStorage,
            mediaId: Long,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(VideoEditorViewModel::class.java))
                    return VideoEditorViewModel(repository, storage, mediaId) as T
                }
            }
    }
}
