package com.cinematv.app.data

import android.net.Uri
import android.text.Html
import com.cinematv.app.model.EpisodeInfo
import com.cinematv.app.model.MediaKind
import com.cinematv.app.model.MediaTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class CatalogRepository {
    suspend fun loadSeries(page: Int = 0): List<MediaTitle> = withContext(Dispatchers.IO) {
        val response = requestJson("https://api.tvmaze.com/shows?page=$page") as? JSONArray
            ?: return@withContext emptyList()
        buildList {
            for (index in 0 until response.length()) {
                response.optJSONObject(index)?.let { add(parseTvMazeShow(it)) }
            }
        }.distinctBy { it.id }
    }

    suspend fun searchSeries(query: String): List<MediaTitle> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val response = requestJson("https://api.tvmaze.com/search/shows?q=$encoded") as? JSONArray
            ?: return@withContext emptyList()
        buildList {
            for (index in 0 until response.length()) {
                val show = response.optJSONObject(index)?.optJSONObject("show") ?: continue
                add(parseTvMazeShow(show))
            }
        }.distinctBy { it.id }
    }

    suspend fun loadEpisodes(showId: String): List<EpisodeInfo> = withContext(Dispatchers.IO) {
        val numericId = showId.removePrefix("tvmaze:").toLongOrNull()
            ?: return@withContext emptyList()
        val response = requestJson("https://api.tvmaze.com/shows/$numericId/episodes") as? JSONArray
            ?: return@withContext emptyList()
        buildList {
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                add(
                    EpisodeInfo(
                        id = item.optLong("id"),
                        name = item.optString("name", "Episode"),
                        season = item.optInt("season"),
                        number = item.optInt("number"),
                        airDate = item.optString("airdate"),
                        summary = cleanHtml(item.optString("summary")),
                        imageUrl = item.optJSONObject("image")?.optString("medium")?.takeIf(String::isNotBlank)
                    )
                )
            }
        }
    }

    /**
     * Search Internet Archive movie records, then independently inspect each
     * record's metadata and media files. A result is returned only if its
     * licence is on our explicit allow-list and an MP4 endpoint responds like
     * a video file. This is intentionally conservative and may return no items.
     */
    suspend fun searchLicensedArchiveFilms(query: String = ""): List<MediaTitle> = withContext(Dispatchers.IO) {
        val safeQuery = query.trim().filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,'-" }.take(80)
        // Internet Archive documents wildcard searches such as licenseurl:http*by*.
        // The client still applies the narrower local allow-list below.
        val licenceQuery = "(licenseurl:http*publicdomain* OR licenseurl:http*zero* OR " +
            "licenseurl:http*by/4.0* OR licenseurl:http*by/3.0* OR " +
            "licenseurl:http*by-sa/4.0* OR licenseurl:http*by-sa/3.0*)"
        val archiveQuery = buildString {
            append("mediatype:movies AND ").append(licenceQuery)
            if (safeQuery.isNotBlank()) append(" AND title:\"").append(safeQuery).append("\"")
        }
        val encoded = URLEncoder.encode(archiveQuery, Charsets.UTF_8.name())
        val searchUrl = "https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier,title,year&rows=18&page=1&output=json"
        val search = requestJson(searchUrl) as? JSONObject ?: return@withContext emptyList()
        val docs = search.optJSONObject("response")?.optJSONArray("docs") ?: return@withContext emptyList()
        val results = mutableListOf<MediaTitle>()

        for (index in 0 until docs.length()) {
            val doc = docs.optJSONObject(index) ?: continue
            val identifier = doc.optString("identifier").takeIf { it.isNotBlank() } ?: continue
            val item = inspectArchiveItem(identifier) ?: continue
            results += item.copy(
                title = doc.optString("title").takeIf { it.isNotBlank() } ?: item.title,
                year = doc.optString("year").takeIf { it.isNotBlank() } ?: item.year
            )
        }
        results.distinctBy { it.id }
    }

    private fun inspectArchiveItem(identifier: String): MediaTitle? {
        val metadata = requestJson("https://archive.org/metadata/${Uri.encode(identifier)}") as? JSONObject
            ?: return null
        val meta = metadata.optJSONObject("metadata") ?: return null
        val licenseUrl = meta.optString("licenseurl").takeIf { it.isNotBlank() }
        if (!LicensePolicy.isAllowed(licenseUrl)) return null
        val rightsText = meta.optString("rights").lowercase()
        if (rightsText.contains("all rights reserved") || rightsText.contains("permission required")) return null

        val files = metadata.optJSONArray("files") ?: return null
        var chosenFile: String? = null
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val name = file.optString("name")
            val format = file.optString("format")
            val isMp4 = name.endsWith(".mp4", ignoreCase = true) &&
                (format.contains("mpeg4", ignoreCase = true) || format.contains("mp4", ignoreCase = true))
            val isPrivate = file.optString("private").equals("true", ignoreCase = true)
            val size = file.optString("size").toLongOrNull() ?: 0L
            if (isMp4 && !isPrivate && size >= 250_000L) {
                chosenFile = name
                break
            }
        }
        val fileName = chosenFile ?: return null
        val mediaUrl = "https://archive.org/download/${Uri.encode(identifier)}/${Uri.encode(fileName)}"
        if (!isReachableVideo(mediaUrl)) return null

        val poster = "https://archive.org/services/img/${Uri.encode(identifier)}"
        val description = cleanHtml(meta.optString("description")).ifBlank {
            "A film record from Internet Archive."
        }
        return MediaTitle(
            id = "archive:$identifier",
            title = meta.optString("title").ifBlank { identifier },
            kind = MediaKind.MOVIE,
            posterUrl = poster,
            summary = description,
            year = meta.optString("year"),
            genres = listOf("Archive film"),
            detailsUrl = "https://archive.org/details/${Uri.encode(identifier)}",
            playbackUrl = mediaUrl,
            licenceLabel = LicensePolicy.displayName(licenseUrl),
            licenceUrl = licenseUrl,
            creator = meta.optString("creator").takeIf { it.isNotBlank() }
        )
    }

    private fun parseTvMazeShow(show: JSONObject): MediaTitle {
        val image = show.optJSONObject("image")
        val rating = show.optJSONObject("rating")?.optDouble("average")?.takeIf { it > 0.0 }
        return MediaTitle(
            id = "tvmaze:${show.optLong("id")}",
            title = show.optString("name", "Untitled series"),
            kind = MediaKind.SERIES,
            posterUrl = image?.optString("medium")?.takeIf(String::isNotBlank),
            summary = cleanHtml(show.optString("summary")),
            year = show.optString("premiered").take(4),
            genres = show.optJSONArray("genres").toStringList(),
            rating = rating?.let { String.format(java.util.Locale.US, "%.1f", it) },
            detailsUrl = show.optString("url").takeIf(String::isNotBlank)
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun cleanHtml(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    private fun requestJson(urlString: String): Any? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 7_000
            connection.readTimeout = 9_000
            connection.setRequestProperty("User-Agent", "CINEMA-TV-Android/1.0 (Android)")
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status !in 200..299) return null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val trimmed = body.trimStart()
            when {
                trimmed.startsWith("[") -> JSONArray(body)
                trimmed.startsWith("{") -> JSONObject(body)
                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun verifyPlayableUrl(urlString: String): Boolean = withContext(Dispatchers.IO) {
        isReachableVideo(urlString)
    }

    private fun isReachableVideo(urlString: String): Boolean {
        val source = try {
            URL(urlString)
        } catch (_: Exception) {
            return false
        }
        // Only app-generated Internet Archive download URLs may reach the player.
        if (source.protocol != "https" || !source.host.equals("archive.org", ignoreCase = true) ||
            !source.path.startsWith("/download/")) return false

        fun openArchiveResponse(method: String): HttpURLConnection? {
            var currentUrl = source
            // Follow redirects manually so an Archive record cannot send the app
            // to an unrelated host or downgrade playback to plain HTTP.
            repeat(6) {
                val hostAllowed = currentUrl.host.equals("archive.org", ignoreCase = true) ||
                    currentUrl.host.endsWith(".archive.org", ignoreCase = true)
                if (currentUrl.protocol != "https" || !hostAllowed) return null
                val connection = currentUrl.openConnection() as HttpURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = method
                    connection.connectTimeout = 4_500
                    connection.readTimeout = 4_500
                    connection.setRequestProperty("User-Agent", "CINEMA-TV-Android/1.0 (Android)")
                    if (method == "GET") connection.setRequestProperty("Range", "bytes=0-0")
                    val status = connection.responseCode
                    if (status in 300..399) {
                        val location = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (location.isNullOrBlank()) return null
                        currentUrl = URL(currentUrl, location)
                    } else if (status in 200..299) {
                        return connection
                    } else {
                        connection.disconnect()
                        return null
                    }
                } catch (_: Exception) {
                    connection.disconnect()
                    return null
                }
            }
            return null
        }

        fun hasVideoResponse(method: String): Boolean {
            val connection = openArchiveResponse(method) ?: return false
            return try {
                val contentType = connection.contentType.orEmpty().lowercase()
                val typeLooksLikeVideo = !contentType.contains("text/html") &&
                    (contentType.startsWith("video/") || contentType.contains("octet-stream"))
                val declaredLength = connection.contentLengthLong
                val rangeTotal = connection.getHeaderField("Content-Range")
                    ?.substringAfterLast('/')?.toLongOrNull()
                val lengthIsPlausible = when (method) {
                    "GET" -> rangeTotal == null || rangeTotal >= 250_000L
                    else -> declaredLength < 0L || declaredLength >= 250_000L
                }
                val responseIsValid = typeLooksLikeVideo && lengthIsPlausible
                if (responseIsValid && method == "GET") {
                    // Force the response body to begin, without downloading the whole film.
                    connection.inputStream.use { it.read() }
                }
                responseIsValid
            } catch (_: Exception) {
                false
            } finally {
                connection.disconnect()
            }
        }

        return hasVideoResponse("HEAD") || hasVideoResponse("GET")
    }
}
