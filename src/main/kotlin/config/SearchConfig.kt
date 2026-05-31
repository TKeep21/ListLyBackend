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
import kotlin.math.min

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

    fun ensureIndexExists(
        maxAttempts: Int = 10,
        retryDelay: Duration = Duration.ofMillis(400)
    ): Boolean {
        if (maxAttempts < 1) return false

        repeat(maxAttempts) { attempt ->
            val attemptNumber = attempt + 1

            if (!health()) {
                log.warn(
                    "Meili is not healthy yet. attempt={}/{} host={}",
                    attemptNumber,
                    maxAttempts,
                    settings.host
                )
                waitBeforeRetry(attempt, retryDelay)
                return@repeat
            }

            val indexResponse = send("${settings.host}/indexes/${settings.index}", "check index", "GET")
            if (indexResponse == null) {
                waitBeforeRetry(attempt, retryDelay)
                return@repeat
            }

            if (indexResponse.statusCode() in 200..299) {
                return true
            }

            val body = indexResponse.body()
            if (indexResponse.statusCode() == 404 && body.contains("index_not_found", ignoreCase = true)) {
                val createPayload = """{"uid":"${settings.index}","primaryKey":"id"}"""
                val createResponse = send(
                    "${settings.host}/indexes",
                    "create index",
                    "POST",
                    createPayload
                )

                if (createResponse == null) {
                    waitBeforeRetry(attempt, retryDelay)
                    return@repeat
                }

                if (createResponse.statusCode() in 200..299) {
                    log.info(
                        "Meili index creation accepted. index={} status={} attempt={}/{}",
                        settings.index,
                        createResponse.statusCode(),
                        attemptNumber,
                        maxAttempts
                    )
                } else if (
                    createResponse.statusCode() == 409 ||
                    createResponse.body().contains("index_already_exists", ignoreCase = true)
                ) {
                    log.info("Meili index already exists. index={}", settings.index)
                    return true
                } else {
                    log.warn(
                        "Failed to create Meili index. index={} status={} body={} attempt={}/{}",
                        settings.index,
                        createResponse.statusCode(),
                        createResponse.body(),
                        attemptNumber,
                        maxAttempts
                    )
                }

                waitBeforeRetry(attempt, retryDelay)
                return@repeat
            }

            log.warn(
                "Unexpected response while checking Meili index. index={} status={} body={} attempt={}/{}",
                settings.index,
                indexResponse.statusCode(),
                body,
                attemptNumber,
                maxAttempts
            )
            waitBeforeRetry(attempt, retryDelay)
        }

        log.warn(
            "Failed to ensure Meili index after retries. index={} attempts={}",
            settings.index,
            maxAttempts
        )
        return false
    }

    private fun send(
        url: String,
        action: String,
        method: String,
        body: String? = null
    ): HttpResponse<String>? {
        val requestBuilder = requestBuilder(url)
        val request = when (method) {
            "GET" -> requestBuilder.GET().build()
            "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body.orEmpty())).build()
            else -> error("Unsupported method: $method")
        }

        return try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.warn("Failed to {} in MeiliSearch. url={}", action, url, e)
            null
        }
    }

    private fun waitBeforeRetry(attempt: Int, baseDelay: Duration) {
        if (attempt <= 0) {
            Thread.sleep(baseDelay.toMillis())
            return
        }

        val multiplier = min(attempt + 1, 5)
        Thread.sleep(baseDelay.toMillis() * multiplier)
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
    val indexEnsured = MeiliSearchConfig.client.ensureIndexExists()

    if (MeiliSearchConfig.client.health()) {
        if (indexEnsured) {
            log.info("Meilisearch is reachable at ${MeiliSearchConfig.settings.host}, index=${MeiliSearchConfig.settings.index} is ready")
        } else {
            log.warn("Meilisearch is reachable at ${MeiliSearchConfig.settings.host}, but index=${MeiliSearchConfig.settings.index} was not confirmed during startup")
        }
    } else {
        log.warn("Meilisearch is not reachable at ${MeiliSearchConfig.settings.host}. Search endpoints will not work until it is available.")
    }
}
