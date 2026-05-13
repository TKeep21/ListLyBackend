package com.example.config


import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.IgnoreTrailingSlash


fun Application.configureHTTP() {
    install(CallLogging)
    install(DefaultHeaders)
    install(IgnoreTrailingSlash)
    install(CORS) {
        anyHost()
        allowCredentials = true
    }
}
