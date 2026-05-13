package com.example.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

class TestRoleProvider : RoleProvider {
    override suspend fun getRole(call: ApplicationCall): String? =
        call.principal<TestUserPrincipal>()?.role
}
