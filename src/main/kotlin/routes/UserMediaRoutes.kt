package com.example.routes

import com.example.UserFolder.UserFolderRepository
import com.example.UserFolder.UserFolderService
import com.example.UserMedia.UserMediaRepository
import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaFavouriteRequest
import com.example.UserMedia.dto.UpdateUserMediaFoldersRequest
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaStatusRequest
import com.example.UserMedia.dto.toResponse
import com.example.UserMedia.model.SortDirection
import com.example.UserMedia.model.UserCollectionStatus
import com.example.UserMedia.model.UserMediaSortBy
import com.example.media.MediaCatalogRepository
import com.example.media.MediaCatalogService
import com.example.media.model.MediaType
import com.example.search.repository.MeiliMediaSearchRepository
import com.example.search.service.SearchIndexServiceImpl
import com.example.security.JwtUserIdProvider
import com.example.security.UserIdProvider
import com.example.security.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.UserMediaRouting(
    userMediaService: UserMediaService,
    userIdProvider: UserIdProvider = JwtUserIdProvider()
) {
    routing {
        authenticate("auth-jwt") {
            route("/user-media") {

                get("/{userMediaId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val item = userMediaService.getById(userId, userMediaId)
                    call.respond(item.toResponse())
                }

                get {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val query = call.parseUserMediaQuery()
                    val items = userMediaService
                        .getAllMediaItemsByUserId(
                            userId = userId,
                            status = query.status,
                            favourite = query.favourite,
                            folderId = query.folderId,
                            mediaType = query.mediaType,
                            sortBy = query.sortBy,
                            sortDirection = query.sortDirection
                        )
                        .map { it.toResponse() }

                    call.respond(items)
                }

                patch("/{userMediaId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)

                    val request = call.receive<UpdateUserMediaRequest>()
                    userMediaService.update(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }

                post {
                    val userId = call.requireUserId(userIdProvider) ?: return@post

                    val request = call.receive<CreateUserMediaRequest>()
                    userMediaService.create(userId, request)
                    call.respond(HttpStatusCode.Created)
                }

                delete("/{userMediaId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@delete
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)

                    userMediaService.delete(userId, userMediaId)
                    call.respond(HttpStatusCode.OK)
                }

                patch("/{userMediaId}/status") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserMediaStatusRequest>()

                    userMediaService.updateStatus(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }

                patch("/{userMediaId}/favourite") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserMediaFavouriteRequest>()

                    userMediaService.updateFavourite(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }

                patch("/{userMediaId}/folders") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserMediaFoldersRequest>()

                    userMediaService.updateFolders(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }
            }

            route("/api/user-media") {
                get {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val query = call.parseUserMediaQuery()
                    val items = userMediaService
                        .getAllMediaItemsByUserId(
                            userId = userId,
                            status = query.status,
                            favourite = query.favourite,
                            folderId = query.folderId,
                            mediaType = query.mediaType,
                            sortBy = query.sortBy,
                            sortDirection = query.sortDirection
                        )
                        .map { it.toResponse() }

                    call.respond(items)
                }

                patch("/{userMediaId}/status") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserMediaStatusRequest>()

                    userMediaService.updateStatus(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }

                patch("/{userMediaId}/favourite") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserMediaFavouriteRequest>()

                    userMediaService.updateFavourite(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }

                patch("/{userMediaId}/folders") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    val request = call.receive<UpdateUserMediaFoldersRequest>()

                    userMediaService.updateFolders(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

// Prod wiring отдельно
fun Application.UserMediaRouting() {
    val userMediaRepo = UserMediaRepository()
    val userFolderRepository = UserFolderRepository()
    val mediaCatalogRepo = MediaCatalogRepository()
    val searchRepo = MeiliMediaSearchRepository()
    val searchIndexService = SearchIndexServiceImpl(mediaCatalogRepo, searchRepo)
    val mediaCatalogService = MediaCatalogService(mediaCatalogRepo, searchIndexService)
    val userFolderService = UserFolderService(userFolderRepository, userMediaRepo)
    val service = UserMediaService(userMediaRepo, mediaCatalogService, userFolderService)
    UserMediaRouting(service, JwtUserIdProvider())
}

private fun parseCollectionStatus(value: String): UserCollectionStatus {
    return try {
        UserCollectionStatus.valueOf(value.trim().uppercase())
    } catch (_: IllegalArgumentException) {
        throw BadRequestException("Unknown status: $value")
    }
}

private data class UserMediaQuery(
    val status: UserCollectionStatus?,
    val favourite: Boolean?,
    val folderId: String?,
    val mediaType: MediaType?,
    val sortBy: UserMediaSortBy,
    val sortDirection: SortDirection
)

private fun ApplicationCall.parseUserMediaQuery(): UserMediaQuery {
    return UserMediaQuery(
        status = parameters["status"]?.let { parseCollectionStatus(it) },
        favourite = parameters["favourite"]?.let { parseBoolean("favourite", it) },
        folderId = parameters["folderId"]?.trim()?.takeIf { it.isNotEmpty() },
        mediaType = parameters["mediaType"]?.let { parseMediaType(it) },
        sortBy = parameters["sortBy"]?.let { parseUserMediaSortBy(it) } ?: UserMediaSortBy.CREATED_AT,
        sortDirection = parameters["sortDirection"]?.let { parseSortDirection(it) } ?: SortDirection.DESC
    )
}

private fun parseBoolean(parameterName: String, value: String): Boolean {
    return value.trim().lowercase().toBooleanStrictOrNull()
        ?: throw BadRequestException("$parameterName must be true or false")
}

private fun parseMediaType(value: String): MediaType {
    return try {
        MediaType.valueOf(value.trim().uppercase())
    } catch (_: IllegalArgumentException) {
        throw BadRequestException("Unknown mediaType: $value")
    }
}

private fun parseUserMediaSortBy(value: String): UserMediaSortBy {
    return when (value.trim().lowercase()) {
        "createdat", "created_at", "addedat", "added_at", "dateadded", "date_added" -> UserMediaSortBy.CREATED_AT
        "title", "alphabet", "alphabetical", "name" -> UserMediaSortBy.TITLE
        else -> throw BadRequestException("Unknown sortBy: $value")
    }
}

private fun parseSortDirection(value: String): SortDirection {
    return when (value.trim().lowercase()) {
        "asc", "ascending" -> SortDirection.ASC
        "desc", "descending" -> SortDirection.DESC
        else -> throw BadRequestException("Unknown sortDirection: $value")
    }
}
