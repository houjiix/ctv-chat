package com.cinematv.app.data

import com.cinematv.app.model.MediaKind
import com.cinematv.app.model.MediaTitle

fun MediaTitle.toFavoriteEntity() = FavoriteEntity(
    id = id,
    title = title,
    kind = kind.name,
    posterUrl = posterUrl,
    summary = summary,
    year = year,
    genres = genres.joinToString("|"),
    rating = rating,
    detailsUrl = detailsUrl,
    playbackUrl = playbackUrl,
    licenceLabel = licenceLabel,
    licenceUrl = licenceUrl,
    creator = creator
)

fun FavoriteEntity.toMediaTitle() = MediaTitle(
    id = id,
    title = title,
    kind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.MOVIE),
    posterUrl = posterUrl,
    summary = summary,
    year = year,
    genres = genres.split('|').filter(String::isNotBlank),
    rating = rating,
    detailsUrl = detailsUrl,
    playbackUrl = playbackUrl,
    licenceLabel = licenceLabel,
    licenceUrl = licenceUrl,
    creator = creator
)
