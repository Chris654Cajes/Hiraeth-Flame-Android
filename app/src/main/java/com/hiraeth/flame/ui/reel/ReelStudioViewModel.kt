package com.hiraeth.flame.ui.reel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hiraeth.flame.data.db.MediaEntity
import com.hiraeth.flame.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Picks multiple local videos and stages them under [com.hiraeth.flame.data.local.MediaStorage.reelsDir].
 * True frame-accurate merge is delegated to Media3 Transformer in production; this sample copies sources in order.
 */
class ReelStudioViewModel(
    private val repository: MediaRepository,
    private val reelsRoot: File,
) : ViewModel() {

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    val videos: StateFlow<List<MediaEntity>> = repository.observeAll()
        .map { list -> list.filter { it.isVideo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun toggle(id: Long) {
        _selected.value = if (_selected.value.contains(id)) {
            _selected.value - id
        } else {
            _selected.value + id
        }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun stageReelProject() {
        viewModelScope.launch {
            val ids = _selected.value.toList()
            if (ids.size < 2) {
                _status.value = "Select at least two videos."
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val folder = File(reelsRoot, "reel_${System.currentTimeMillis()}").apply { mkdirs() }
                    ids.forEachIndexed { index, mediaId ->
                        val entity = repository.getById(mediaId) ?: return@forEachIndexed
                        val src = repository.resolveFile(entity)
                        val dest = File(folder, "${index.toString().padStart(2, '0')}_${UUID.randomUUID()}.mp4")
                        src.copyTo(dest, overwrite = true)
                    }
                    _status.value = "Staged ${ids.size} clips in ${folder.name}. Merge with Media3 Transformer for a single MP4."
                }
            }.onFailure {
                _status.value = it.message ?: "Could not stage reel"
            }
        }
    }

    companion object {
        fun factory(repository: MediaRepository, reelsRoot: File): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ReelStudioViewModel::class.java))
                    return ReelStudioViewModel(repository, reelsRoot) as T
                }
            }
    }
}
