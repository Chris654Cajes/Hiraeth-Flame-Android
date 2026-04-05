package com.hiraeth.flame.ui.editor

import android.graphics.Path

/** One ink stroke for image export (XML / View pipeline; no Compose). */
data class StrokePath(val colorArgb: Int, val path: Path)
