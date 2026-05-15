package com.example.config

import io.ktor.server.application.Application
import io.ktor.server.config.propertyOrNull
import io.ktor.server.application.log
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class MeiliSearchSettings(
    val host: String,
    val apiKey: String?,
    val index: String
)

class MeiliSearchClient(
    private val settings: MeiliSearchSettings
) {
    private val log = LoggerFactory.getLogger(MeiliSearchClient::class.java)
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    fun health(): Boolean {
        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("${settings.host}/health"))
                .timeout(Duration.ofSeconds(3))
                .GET()

            if (!settings.apiKey.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${settings.apiKey}")
            }

            val response = http.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding())
            response.statusCode() in 200..299
        }catch (e: Exception) {
            log.error("Meili health check failed: {}", e)
            false
        }
    }

    fun ensureIndexExists() {
        val indexUrl = "${settings.host}/indexes/${settings.index}"
        val checkRequest = requestBuilder(indexUrl)
            .GET()
            .build()

        val checkResponse = try {
            http.send(checkRequest, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.warn("Failed to check Meili index existence: {}", settings.index, e)
            return
        }

        if (checkResponse.statusCode() in 200..299) {
            return
        }

        if (checkResponse.statusCode() != 404 || !checkResponse.body().contains("index_not_found", ignoreCase = true)) {
            log.warn(
                "Unexpected response while checking Meili index. index={}, status={}, body={}",
                settings.index,
                checkResponse.statusCode(),
                checkResponse.body()
            )
            return
        }

        val payload = """{"uid":"${settings.index}","primaryKey":"id"}"""
        val createRequest = requestBuilder("${settings.host}/indexes")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val createResponse = try {
            http.send(createRequest, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.warn("Failed to create Meili index: {}", settings.index, e)
            return
        }

        if (createResponse.statusCode() !in 200..299) {
            log.warn(
                "Failed to create Meili index. index={}, status={}, body={}",
                settings.index,
                createResponse.statusCode(),
                createResponse.body()
            )
            return
        }

        log.info("Meili index ensured. index={}", settings.index)
    }

    private fun requestBuilder(url: String): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(3))
            .header("Content-Type", "application/json")

        if (!settings.apiKey.isNullOrBlank()) {
            builder.header("Authorization", "Bearer ${settings.apiKey}")
        }

        return builder
    }
}

object MeiliSearchConfig {
    lateinit var settings: MeiliSearchSettings
        private set

    lateinit var client: MeiliSearchClient
        private set

    fun init(application: Application) {
        val config = application.environment.config
        val host = System.getenv("MEILI_HOST")
            ?: config.propertyOrNull("meilisearch.host")?.getString()
            ?: "http://localhost:7700"
        val apiKey = System.getenv("MEILI_API_KEY")
            ?: config.propertyOrNull("meilisearch.apiKey")?.getString()
        val index = System.getenv("MEILI_INDEX")
            ?: config.propertyOrNull("meilisearch.index")?.getString()
            ?: "media_items"

        settings = MeiliSearchSettings(
            host = host.trimEnd('/'),
            apiKey = apiKey,
            index = index
        )
        client = MeiliSearchClient(settings)
    }
}

fun Application.configureSearch() {
    MeiliSearchConfig.init(this)
    MeiliSearchConfig.client.ensureIndexExists()

    if (MeiliSearchConfig.client.health()) {
        log.info("Meilisearch is reachable at ${MeiliSearchConfig.settings.host}, index=${MeiliSearchConfig.settings.index}")
    } else {
        log.warn("Meilisearch is not reachable at ${MeiliSearchConfig.settings.host}. Search endpoints will not work until it is available.")
    }
}
