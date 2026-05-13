package com.example.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

class TestUserIdProvider : UserIdProvider {
    override suspend fun getUserId(call: ApplicationCall): String? =
        call.principal<TestUserPrincipal>()?.userId
}
