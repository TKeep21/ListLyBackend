package com.example.routes

import com.example.auth.AuthService
import com.example.auth.dto.AuthResponse
import com.example.auth.dto.LoginRequest
import com.example.auth.dto.RegisterRequest
import com.example.auth.exceptions.AuthException
import com.example.security.JwtService
import com.example.security.PasswordHasher
import com.example.user.User
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.example.user.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal


fun Application.AuthRouting() {


    val userRepository = UserRepository()
    val authService = AuthService(userRepository)
    AuthRouting(authService)
}

fun Application.AuthRouting(authService: AuthService){
    routing {
        route("/auth") {
            post("/login") {
                val request = call.receive<LoginRequest>()
                val token = authService.login(request.login, request.password)
                call.respond(AuthResponse(token))
            }



            post("/register"){
                val request = call.receive<RegisterRequest>()
                authService.register(request.login,request.password)
                call.respond(HttpStatusCode.Created)

            }
        }

    }

}


