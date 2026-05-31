package com.example.routes

import com.example.media.MediaCatalogRepository
import com.example.media.MediaCatalogService
import com.example.search.repository.MeiliMediaSearchRepository
import com.example.search.service.MeiliMediaSearchServiceImpl
import com.example.search.service.SearchIndexServiceImpl
import com.example.search.service.SearchService
import io.ktor.server.plugins.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.text.orEmpty

fun Application.searchRoute(searchService: SearchService){
    routing {
        route("/media/search") {
            get {
                val query = call.request.queryParameters["query"].orEmpty()
                val limit = call.request.queryParameters.parseIntParam("limit", 12)
                val offset = call.request.queryParameters.parseIntParam("offset", 0)


                val result = searchService.search(query, limit, offset)
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}

fun Application.searchRoutes(){
    val searchRepo = MeiliMediaSearchRepository()
    val mediaCatalogRepo = MediaCatalogRepository()
    val searchIndexService = SearchIndexServiceImpl(mediaCatalogRepo, searchRepo)
    val mediaCatalogService = MediaCatalogService(mediaCatalogRepo, searchIndexService)
    val service = MeiliMediaSearchServiceImpl(searchRepo,mediaCatalogService)
    searchRoute(service)

}

private fun io.ktor.http.Parameters.parseIntParam(name: String, default: Int): Int {
    val raw = this[name] ?: return default
    return raw.toIntOrNull() ?: throw BadRequestException("$name must be an integer")
}
