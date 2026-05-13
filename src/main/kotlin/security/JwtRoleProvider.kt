package com.example.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

class JwtRoleProvider(
    private val claimName: String = "role"
) : RoleProvider {
    override suspend fun getRole(call: ApplicationCall): String? {
        val principal = call.principal<JWTPrincipal>() ?: return null
        return principal.payload.getClaim(claimName).asString()
    }
}
