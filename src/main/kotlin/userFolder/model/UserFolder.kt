package com.example.UserFolder.model

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class UserFolder(
    val id: String = ObjectId().toString(),
    val userId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
