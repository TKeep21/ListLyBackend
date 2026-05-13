package com.example.auth.dto

import kotlinx.serialization.Serializable


@Serializable
data class LoginRequest(val login: String,
    val password:String )