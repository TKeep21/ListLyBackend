package com.example.search.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class SearchMediaRequest(val q: String, val limit: Int = 20, val offset: Int = 0)
