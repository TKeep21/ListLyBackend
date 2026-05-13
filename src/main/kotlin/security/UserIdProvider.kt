package com.example.security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.response.respond


fun interface UserIdProvider{
    suspend fun getUserId(call: ApplicationCall): String?
}

suspend inline fun ApplicationCall.requireUserId(provider: UserIdProvider): String?{
    val userId = provider.getUserId(this)
    if (userId == null){
        respond(HttpStatusCode.Unauthorized)
        return null
    }
    return userId
}


