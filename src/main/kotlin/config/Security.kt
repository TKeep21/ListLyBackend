package com.example.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {

    val jwtConfig = environment.config.config("jwt")

    val jwtAudience = jwtConfig.property("audience").getString()
    val jwtIssuer = jwtConfig.property("issuer").getString()
    val jwtRealm = jwtConfig.property("realm").getString()
    val jwtSecret = jwtConfig.property("secret").getString()

    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}

