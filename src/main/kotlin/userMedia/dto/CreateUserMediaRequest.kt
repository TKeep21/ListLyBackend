package com.example.UserMedia.dto

import com.example.UserMedia.model.UserCollectionStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserMediaRequest(
    val mediaId: String,
    val collectionStatus: UserCollectionStatus? = null,
    val isFavourite: Boolean = false,
    val folderIds: List<String> = emptyList(),
    val userRating: Double? = null,
    val note : String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
