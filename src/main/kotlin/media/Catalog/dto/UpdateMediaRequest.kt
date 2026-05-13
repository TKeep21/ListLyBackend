package com.example.media.dto

import com.example.media.Catalog.dto.model.ExternalRef
import com.example.media.Catalog.dto.model.MediaStatus
import kotlinx.serialization.Serializable

@Serializable
data class UpdateMediaRequest(
    val title: String? = null,
    val description: String? = null,
    val mediaStatus: MediaStatus? = null,
    val genres: List<String>? = null,
    val posterUrl: String? = null,
    val externalRef: ExternalRef? = null
)