package com.example.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

class JwtUserIdProvider(private val claimName:String = "userId") : UserIdProvider{
    override suspend fun getUserId(call: ApplicationCall): String? {
        val principal = call.principal<JWTPrincipal>() ?: return null
        return principal.payload.getClaim(claimName).asString()
    }
}