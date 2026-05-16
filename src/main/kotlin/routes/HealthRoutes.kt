package com.example.routes

import com.example.config.MeiliSearchConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class HealthServices(
    val api: String,
    val search: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val services: HealthServices
)

fun Application.HealthRouting() {
    routing {
        get("/health") {
            val searchStatus = runCatching {
                if (MeiliSearchConfig.client.health()) "UP" else "DOWN"
            }.getOrElse { "DOWN" }

            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    status = "UP",
                    services = HealthServices(
                        api = "UP",
                        search = searchStatus
                    )
                )
            )
        }
    }
}
