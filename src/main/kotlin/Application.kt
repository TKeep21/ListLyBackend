package com.example

import com.example.auth.dto.LoginRequest
import com.example.config.configureDatabase
import com.example.config.configureHTTP
import com.example.config.configureRouting
import com.example.config.configureSearch
import com.example.config.configureSecurity
import com.example.plugins.configureStatusPages
import com.example.routes.searchRoutes
import com.example.search.service.SearchService
import com.example.security.JwtService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureDatabase()
    configureSearch()
    configureSecurity()
    configureRouting()
    configureStatusPages()

    JwtService.init(environment)
}

