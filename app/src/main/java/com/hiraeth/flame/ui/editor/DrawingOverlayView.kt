package com.hiraeth.flame.ui.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hiraeth.flame.R
/**
 * Simple freehand overlay for [ImageEditorFragment]. Paths are snapshotted for [ImageEditorViewModel.export].
 */
class DrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class Stroke(val color: Int, val path: Path)

    private val strokes = mutableListOf<Stroke>()
    private var current: Path? = null
    private val neonViolet get() = context.getColor(R.color.neon_violet)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    var drawingEnabled: Boolean = false
        set(value) {
            field = value
            isClickable = value
            if (!value) {
                current = null
            }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (s in strokes) {
            paint.color = s.color
            canvas.drawPath(s.path, paint)
        }
        current?.let {
            paint.color = neonViolet
            canvas.drawPath(it, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                current = Path().apply { moveTo(event.x, event.y) }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                current?.lineTo(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current?.let { p ->
                    if (pathLength(p) > 8f) {
                        strokes.add(Stroke(neonViolet, Path(p)))
                    }
                }
                current = null
                invalidate()
            }
        }
        return true
    }

    fun clearStrokes() {
        strokes.clear()
        current = null
        invalidate()
    }

    /** Deep copies for the exporter (thread-safe handoff after snapshot on main thread). */
    fun snapshotStrokes(): List<StrokePath> =
        strokes.map { s ->
            StrokePath(s.color, Path().apply { addPath(s.path) })
        }

    private fun pathLength(p: Path): Float {
        val pm = PathMeasure(p, false)
        return pm.length
    }
}
