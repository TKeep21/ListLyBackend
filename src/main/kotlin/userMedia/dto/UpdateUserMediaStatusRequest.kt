package com.example.UserMedia.dto

import com.example.UserMedia.model.UserCollectionStatus
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserMediaStatusRequest(
    val status: UserCollectionStatus
)
