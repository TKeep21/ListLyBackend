package com.example.UserFolder.dto

import com.example.UserFolder.model.UserFolder
import kotlinx.serialization.Serializable

@Serializable
data class UserFolderResponse(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun UserFolder.toResponse() = UserFolderResponse(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt
)
