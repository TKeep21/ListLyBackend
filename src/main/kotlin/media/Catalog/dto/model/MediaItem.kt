package com.example.media.model

import com.example.media.Catalog.dto.model.ExternalRef
import com.example.media.Catalog.dto.model.MediaStatus
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MediaItem(
    val id: String = ObjectId().toString(),
    val title: String,
    val description: String? = null,
    val mediaType: MediaType,
    val mediaStatus: MediaStatus,
    val genres: List<String> = emptyList(),
    val posterUrl: String? = null,
    val externalRef: ExternalRef? = null,
    val userRatingSum: Double = 0.0,
    val userRatingCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
