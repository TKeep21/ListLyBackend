package com.example.auth.dto

import kotlinx.serialization.Serializable


@Serializable
data class AuthResponse (
    val token: String
    )