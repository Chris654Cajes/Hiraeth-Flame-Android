package com.hiraeth.flame.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hiraeth.flame.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class ImageEditorViewModel(
    private val repository: MediaRepository,
    private val mediaId: Long,
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun clearMessage() {
        _message.value = null
    }

    /**
     * Applies color matrix + rotation, optional normalized crop (0..1), and vector strokes, then overwrites the file.
     */
    fun export(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        rotationQuarterTurns: Int,
        cropLeft: Float,
        cropTop: Float,
        cropRight: Float,
        cropBottom: Float,
        strokes: List<StrokePath>,
        onDone: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            _exporting.value = true
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    val entity = repository.getById(mediaId) ?: return@withContext false
                    if (entity.isVideo) return@withContext false
                    val file = repository.resolveFile(entity)
                    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
                    val rotated = rotateBitmap(original, rotationQuarterTurns)
                    if (rotated != original) original.recycle()

                    val l = (cropLeft * rotated.width).toInt().coerceIn(0, rotated.width)
                    val t = (cropTop * rotated.height).toInt().coerceIn(0, rotated.height)
                    val r = (cropRight * rotated.width).toInt().coerceIn(0, rotated.width)
                    val b = (cropBottom * rotated.height).toInt().coerceIn(0, rotated.height)
                    val left = min(l, r)
                    val right = max(l, r)
                    val top = min(t, b)
                    val bottom = max(t, b)
                    val cropped = if (right - left > 1 && bottom - top > 1) {
                        Bitmap.createBitmap(rotated, left, top, right - left, bottom - top)
                    } else {
                        rotated
                    }
                    if (cropped != rotated) rotated.recycle()

                    val adjusted = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(adjusted)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        colorFilter = ColorMatrixColorFilter(colorMatrix(brightness, contrast, saturation))
                    }
                    canvas.drawBitmap(cropped, 0f, 0f, paint)
                    cropped.recycle()

                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 8f
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                    }
                    for (s in strokes) {
                        strokePaint.color = s.colorArgb
                        canvas.drawPath(s.path, strokePaint)
                    }

                    val outW = adjusted.width
                    val outH = adjusted.height
                    FileOutputStream(file).use { out ->
                        adjusted.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    adjusted.recycle()
                    val latest = repository.getById(mediaId)
                    if (latest != null) {
                        val refreshed = repository.resolveFile(latest)
                        repository.update(
                            latest.copy(
                                sizeBytes = refreshed.length(),
                                width = outW,
                                height = outH,
                            ),
                        )
                    }
                    true
                }
            }.getOrDefault(false)
            _exporting.value = false
            _message.value = if (ok) "Saved" else "Could not save image"
            onDone(ok)
        }
    }

    private fun rotateBitmap(src: Bitmap, quarterTurns: Int): Bitmap {
        val turns = ((quarterTurns % 4) + 4) % 4
        if (turns == 0) return src
        val m = Matrix().apply { postRotate(90f * turns) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun colorMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
        val b = ColorMatrix().apply { setScale(brightness, brightness, brightness, 1f) }
        val c = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val sat = ColorMatrix()
        sat.setSaturation(saturation)
        val out = ColorMatrix()
        out.postConcat(b)
        out.postConcat(c)
        out.postConcat(sat)
        return out
    }

    companion object {
        fun factory(repository: MediaRepository, mediaId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ImageEditorViewModel::class.java))
                    return ImageEditorViewModel(repository, mediaId) as T
                }
            }
    }
}
