package com.example.UserMedia.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserMediaRequest(
    val userRating : Double? = null,
    val note : String? = null,

)
