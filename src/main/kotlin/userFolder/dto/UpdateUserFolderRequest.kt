package com.example.UserFolder.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserFolderRequest(
    val name: String
)
