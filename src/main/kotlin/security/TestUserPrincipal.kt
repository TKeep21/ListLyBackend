package com.example.security

import io.ktor.server.auth.Principal

data class TestUserPrincipal(
    val userId: String,
    val role: String = "USER"
) : Principal
