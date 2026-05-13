package com.example.security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

fun interface RoleProvider {
    suspend fun getRole(call: ApplicationCall): String?
}

suspend fun ApplicationCall.requireAdmin(provider: RoleProvider): Boolean {
    val role = provider.getRole(this)
    if (role == null) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
        return false
    }

    if (!role.equals("ADMIN", ignoreCase = true)) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin role required"))
        return false
    }

    return true
}
