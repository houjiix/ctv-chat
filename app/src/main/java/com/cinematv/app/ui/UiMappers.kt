package com.cinematv.app.ui

import com.cinematv.app.data.WatchProgressEntity
import com.cinematv.app.model.MediaKind
import com.cinematv.app.model.MediaTitle

fun WatchProgressEntity.toMediaTitle() = MediaTitle(
    id = id,
    title = title,
    kind = MediaKind.MOVIE,
    posterUrl = posterUrl,
    playbackUrl = playbackUrl,
    licenceUrl = licenceUrl,
    licenceLabel = licenceLabel,
    resumePositionMs = positionMs,
    resumeProgress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else null
)
