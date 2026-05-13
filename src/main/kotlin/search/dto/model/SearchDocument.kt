package com.example.search.dto.model


import kotlinx.serialization.Serializable

@Serializable
data class SearchDocument (
    val id: String,
    val title:String,
)