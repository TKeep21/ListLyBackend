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
                    val status = call.parameters["status"]?.let {
                        parseCollectionStatus(it)
                    }
                    val favourite = call.parameters["favourite"]?.let {
                        it.toBooleanStrictOrNull()
                            ?: throw BadRequestException("favourite must be true or false")
                    }
                    val folderId = call.parameters["folderId"]
                    val mediaType = call.parameters["mediaType"]?.let { parseMediaType(it) }
                    val sortBy = call.parameters["sortBy"]?.let { parseSortBy(it) } ?: UserMediaSortBy.ADDED_DATE
                    val sortDirection = call.parameters["sortDir"]?.let { parseSortDirection(it) } ?: SortDirection.DESC
                    val items = userMediaService
                        .getAllMediaItemsByUserId(
                            userId = userId,
                            status = status,
                            favourite = favourite,
                            folderId = folderId,
                            mediaType = mediaType,
                            sortBy = sortBy,
                            sortDirection = sortDirection
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
                    val status = call.parameters["status"]?.let {
                        parseCollectionStatus(it)
                    }
                    val favourite = call.parameters["favourite"]?.let {
                        it.toBooleanStrictOrNull()
                            ?: throw BadRequestException("favourite must be true or false")
                    }
                    val folderId = call.parameters["folderId"]
                    val mediaType = call.parameters["mediaType"]?.let { parseMediaType(it) }
                    val sortBy = call.parameters["sortBy"]?.let { parseSortBy(it) } ?: UserMediaSortBy.ADDED_DATE
                    val sortDirection = call.parameters["sortDir"]?.let { parseSortDirection(it) } ?: SortDirection.DESC
                    val items = userMediaService
                        .getAllMediaItemsByUserId(
                            userId = userId,
                            status = status,
                            favourite = favourite,
                            folderId = folderId,
                            mediaType = mediaType,
                            sortBy = sortBy,
                            sortDirection = sortDirection
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
        UserCollectionStatus.valueOf(value)
    } catch (_: IllegalArgumentException) {
        throw BadRequestException("Unknown status: $value")
    }
}

private fun parseMediaType(value: String): MediaType {
    val parsed = try {
        MediaType.valueOf(value.uppercase())
    } catch (_: IllegalArgumentException) {
        throw BadRequestException("Unknown mediaType: $value")
    }

    if (parsed == MediaType.BOOK) {
        throw BadRequestException("mediaType BOOK is not supported")
    }

    return parsed
}

private fun parseSortBy(value: String): UserMediaSortBy {
    val normalized = value.trim().lowercase()
    return when (normalized) {
        "added_date", "addeddate", "byaddeddate", "created_at", "createdat", "date", "date_added", "bydate" ->
            UserMediaSortBy.ADDED_DATE
        "title", "alphabet", "alphabetical", "byalphabet", "name" ->
            UserMediaSortBy.TITLE
        else -> throw BadRequestException("Unknown sortBy: $value")
    }
}

private fun parseSortDirection(value: String): SortDirection {
    return when (value.trim().lowercase()) {
        "asc", "ascending" -> SortDirection.ASC
        "desc", "descending" -> SortDirection.DESC
        else -> throw BadRequestException("Unknown sortDir: $value")
    }
}
