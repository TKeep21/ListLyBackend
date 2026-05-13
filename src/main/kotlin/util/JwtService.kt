package com.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.user.User
import io.ktor.server.application.*
import java.util.*

object JwtService {

    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var audience: String
    private const val VALIDITY_IN_MS = 1000 * 60 * 60 * 24 // 24 часа

    fun init(environment: ApplicationEnvironment) {
        val config = environment.config.config("jwt")
        secret = config.property("secret").getString()
        issuer = config.property("issuer").getString()
        audience = config.property("audience").getString()
    }

    fun generateToken(user: User): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", user._id.toHexString())
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_IN_MS))
            .sign(Algorithm.HMAC256(secret))
    }
}
