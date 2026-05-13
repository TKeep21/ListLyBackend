package com.example.search.repository

import com.example.config.MeiliSearchConfig
import com.example.search.dto.model.SearchDocument
import com.example.search.dto.request.SearchMediaRequest
import com.example.search.dto.response.SearchMediaResponse
import com.example.search.exceptions.MeiliClientException
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class MeiliMediaSearchRepository(
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SearchReadRepository, SearchIndexRepository {
    private val settings = MeiliSearchConfig.settings
    private val log = LoggerFactory.getLogger(MeiliMediaSearchRepository::class.java)

    override fun searchIds(query: String, limit: Int, offset: Int): List<String> {

        val url = "${settings.host}/indexes/${settings.index}/search"

        val body = json.encodeToString(
            SearchMediaRequest(
                q = query.trim(),
                limit = limit,
                offset = offset
            )
        )

        val request = requestBuilder(url, settings.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = send(request,"search documents")

        return try {
            val parsed = json.decodeFromString(SearchMediaResponse.serializer(), response.body())
            parsed.hits.map {it.id}
        } catch (e: Exception) {
            log.error("Failed to parse Meilisearch response body: {}", response.body(), e)
            throw MeiliClientException("Failed to parse MeiliSearch response", e)
        }
    }

    private fun send(request: HttpRequest, action: String): HttpResponse<String> {
        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.error("Failed to {} in MeiliSearch. uri={}", action, request.uri(), e)
            throw MeiliClientException("Failed to $action in MeiliSearch", e)
        }

        if (response.statusCode() !in 200..299) {
            log.error(
                "MeiliSearch {} failed. status={}, body={}",
                action,
                response.statusCode(),
                response.body()
            )
            throw MeiliClientException(
                "MeiliSearch $action failed with status=${response.statusCode()}, body=${response.body()}"
            )
        }

        return response
    }


    override fun upsertDocuments(documents: List<SearchDocument>) {
        if (documents.isEmpty()) return

        val url = "${settings.host}/indexes/${settings.index}/documents"

        val body = json.encodeToString(documents)

        val request = requestBuilder(url, settings.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        send(request, "upsert documents")


    }

    override fun deleteDocument(documentId: String) {
        val normalizedId = documentId.trim()
        if (normalizedId.isEmpty()) return

        val url = "${settings.host}/indexes/${settings.index}/documents/$normalizedId"

        val request = requestBuilder(url, settings.apiKey)
            .DELETE()
            .build()

        send(request, "delete document with id=$normalizedId")
    }

    private fun requestBuilder(url: String, apiKey: String?): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")

        if (!apiKey.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        return builder
    }


}
