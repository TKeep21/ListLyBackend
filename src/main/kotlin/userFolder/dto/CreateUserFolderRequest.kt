package com.example.UserFolder.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserFolderRequest(
    val name: String
)
