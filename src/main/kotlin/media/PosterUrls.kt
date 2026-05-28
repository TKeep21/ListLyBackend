package com.example.media

import com.example.media.model.MediaItem
import java.net.URI

object PosterUrls {
    const val TMDB_POSTER_BASE = "https://image.tmdb.org/t/p/w500"

    private val tmdbFilePathPattern = Regex(
        pattern = """^/[A-Za-z0-9_-]+\.(jpg|jpeg|png|webp|svg)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val tmdbImagePathPattern = Regex(
        pattern = """^/t/p/[^/]+(/[A-Za-z0-9_-]+\.(jpg|jpeg|png|webp|svg))$""",
        option = RegexOption.IGNORE_CASE
    )

    fun normalize(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (raw.equals("null", ignoreCase = true)) return null

        val uri = runCatching { URI(raw) }.getOrNull()
        if (uri?.scheme != null) {
            if (uri.host?.equals("image.tmdb.org", ignoreCase = true) == true) {
                if (
                    !uri.scheme.equals("http", ignoreCase = true) &&
                    !uri.scheme.equals("https", ignoreCase = true)
                ) {
                    return null
                }

                val filePath = tmdbImagePathPattern.matchEntire(uri.path)?.groupValues?.get(1)
                    ?: return null

                return "$TMDB_POSTER_BASE$filePath"
            }

            return if (uri.scheme.equals("https", ignoreCase = true)) raw else null
        }

        val posterPath = if (raw.startsWith("/")) raw else "/$raw"
        if (!tmdbFilePathPattern.matches(posterPath)) return null

        return "$TMDB_POSTER_BASE$posterPath"
    }
}

fun MediaItem.withNormalizedPosterUrl(): MediaItem = copy(
    posterUrl = PosterUrls.normalize(posterUrl)
)
