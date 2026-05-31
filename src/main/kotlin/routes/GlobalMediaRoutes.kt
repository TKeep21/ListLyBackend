package com.example.routes

import com.example.media.MediaCatalogRepository
import com.example.media.MediaCatalogService
import com.example.media.dto.CreateMediaRequest
import com.example.media.dto.UpdateMediaRequest
import com.example.search.repository.MeiliMediaSearchRepository
import com.example.search.service.SearchIndexServiceImpl
import com.example.security.JwtRoleProvider
import com.example.security.RoleProvider
import com.example.security.requireAdmin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.GlobalMediaRouting(
    mediaService: MediaCatalogService,
    roleProvider: RoleProvider = JwtRoleProvider()
) {

    fun Route.registerCatalogEndpoints() {
        get("/{mediaId}") {
            val mediaId = call.parameters["mediaId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val item = mediaService.findById(mediaId) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(item)
        }

        get("/items/{title}") {
            val title = call.parameters["title"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val items = mediaService.findAllByTitle(title)
            call.respond(items)
        }

        get("/discover") {
            val limit = call.request.queryParameters.parseIntParam("limit", 12)
            val offset = call.request.queryParameters.parseIntParam("offset", 0)
            val items = mediaService.discover(limit = limit, offset = offset)
            call.respond(items)
        }

        authenticate("auth-jwt") {
            post {
                if (!call.requireAdmin(roleProvider)) return@post
                val request = call.receive<CreateMediaRequest>()
                val created = mediaService.create(request)
                call.respond(HttpStatusCode.Created, created)
            }

            patch("/admin/{mediaId}") {
                if (!call.requireAdmin(roleProvider)) return@patch
                val mediaId = call.parameters["mediaId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateMediaRequest>()
                mediaService.updateByAdmin(mediaId, request)
                call.respond(HttpStatusCode.OK)
            }

            post("/admin/reindex") {
                if (!call.requireAdmin(roleProvider)) return@post
                mediaService.reindexSearchIndex()
                call.respond(HttpStatusCode.OK)
            }



            delete("/{mediaId}") {
                if (!call.requireAdmin(roleProvider)) return@delete
                val mediaId = call.parameters["mediaId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                mediaService.delete(mediaId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }


    routing {
        route("/mediaCatalog") {
            registerCatalogEndpoints()
        }
        route("/media") {
            registerCatalogEndpoints()
        }
    }
}

private fun io.ktor.http.Parameters.parseIntParam(name: String, default: Int): Int {
    val raw = this[name] ?: return default
    return raw.toIntOrNull() ?: throw BadRequestException("$name must be an integer")
}

//prod-wiring для global

fun Application.GlobalMediaRoutes() {
    val repo = MediaCatalogRepository()
    val searchRepo = MeiliMediaSearchRepository()
    val searchIndexService = SearchIndexServiceImpl(repo, searchRepo)
    val service = MediaCatalogService(repo, searchIndexService)
    GlobalMediaRouting(service)
}
