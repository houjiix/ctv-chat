package com.cinematv.app.model

enum class MediaKind { MOVIE, SERIES }

data class MediaTitle(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val posterUrl: String? = null,
    val summary: String = "",
    val year: String = "",
    val genres: List<String> = emptyList(),
    val rating: String? = null,
    val detailsUrl: String? = null,
    val playbackUrl: String? = null,
    val licenceLabel: String? = null,
    val licenceUrl: String? = null,
    val creator: String? = null,
    val resumePositionMs: Long? = null,
    val resumeProgress: Float? = null
)

data class EpisodeInfo(
    val id: Long,
    val name: String,
    val season: Int,
    val number: Int,
    val airDate: String,
    val summary: String,
    val imageUrl: String?
)
