package com.example.UserMedia.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserMediaFoldersRequest(
    val folderIds: List<String>
)
