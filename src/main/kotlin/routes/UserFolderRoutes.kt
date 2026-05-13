package com.example.routes

import com.example.UserFolder.UserFolderRepository
import com.example.UserFolder.UserFolderService
import com.example.UserFolder.dto.CreateUserFolderRequest
import com.example.UserFolder.dto.UpdateUserFolderRequest
import com.example.UserFolder.dto.toResponse
import com.example.UserMedia.UserMediaRepository
import com.example.security.JwtUserIdProvider
import com.example.security.UserIdProvider
import com.example.security.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.UserFolderRouting(
    userFolderService: UserFolderService,
    userIdProvider: UserIdProvider = JwtUserIdProvider()
) {
    routing {
        authenticate("auth-jwt") {
            route("/folders") {
                post {
                    val userId = call.requireUserId(userIdProvider) ?: return@post
                    val request = call.receive<CreateUserFolderRequest>()

                    val folder = userFolderService.create(userId, request)
                    call.respond(HttpStatusCode.Created, folder.toResponse())
                }

                get {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val folders = userFolderService.getAllByUserId(userId).map { it.toResponse() }
                    call.respond(folders)
                }

                patch("/{folderId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val folderId = call.parameters["folderId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserFolderRequest>()

                    userFolderService.rename(userId, folderId, request)
                    call.respond(HttpStatusCode.OK)
                }

                delete("/{folderId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@delete
                    val folderId = call.parameters["folderId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)

                    userFolderService.delete(userId, folderId)
                    call.respond(HttpStatusCode.OK)
                }
            }

            route("/api/folders") {
                post {
                    val userId = call.requireUserId(userIdProvider) ?: return@post
                    val request = call.receive<CreateUserFolderRequest>()

                    val folder = userFolderService.create(userId, request)
                    call.respond(HttpStatusCode.Created, folder.toResponse())
                }

                get {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val folders = userFolderService.getAllByUserId(userId).map { it.toResponse() }
                    call.respond(folders)
                }

                patch("/{folderId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val folderId = call.parameters["folderId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserFolderRequest>()

                    userFolderService.rename(userId, folderId, request)
                    call.respond(HttpStatusCode.OK)
                }

                delete("/{folderId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@delete
                    val folderId = call.parameters["folderId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)

                    userFolderService.delete(userId, folderId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

fun Application.UserFolderRouting() {
    val userFolderRepository = UserFolderRepository()
    val userMediaRepository = UserMediaRepository()
    val service = UserFolderService(userFolderRepository, userMediaRepository)
    UserFolderRouting(service, JwtUserIdProvider())
}
