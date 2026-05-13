package com.example.media.Catalog.dto.model

import kotlinx.serialization.Serializable

@Serializable
data class ExternalRef(
    val provider: String,
    val id: String,
    val url: String? = null
)