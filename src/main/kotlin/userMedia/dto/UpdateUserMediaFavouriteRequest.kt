package com.example.UserMedia.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserMediaFavouriteRequest(
    val isFavourite: Boolean
)
