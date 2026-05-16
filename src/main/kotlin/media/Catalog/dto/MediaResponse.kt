package com.example.media.dto

import com.example.media.Catalog.dto.model.MediaStatus
import com.example.media.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class MediaResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val mediaType: MediaType,
    val mediaStatus: MediaStatus,
    val genres: List<String> = emptyList(),
    val posterUrl: String? = null,
    val userRatingAvg: Double? = null,
    val userRatingCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
