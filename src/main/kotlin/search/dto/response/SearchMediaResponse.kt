package com.example.search.dto.response

import com.example.media.model.MediaItem
import com.example.search.dto.model.SearchHit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchMediaResponse(
    @SerialName("hits")
    val hits: List<SearchHit>
)