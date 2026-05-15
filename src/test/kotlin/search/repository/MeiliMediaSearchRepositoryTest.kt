package search.repository

import com.example.config.MeiliSearchConfig
import com.example.config.MeiliSearchSettings
import com.example.search.dto.model.SearchDocument
import com.example.search.exceptions.SearchRequestFailedException
import com.example.search.repository.MeiliMediaSearchRepository
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MeiliMediaSearchRepositoryTest {
    private lateinit var server: HttpServer
    private lateinit var repository: MeiliMediaSearchRepository
    private var lastMethod: String = ""
    private var lastPath: String = ""
    private var lastBody: String = ""
    private var responseCode: Int = 200
    private var responseBody: String = """{"hits":[]}"""

    @BeforeTest
    fun setup() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            lastMethod = exchange.requestMethod
            lastPath = exchange.requestURI.path
            lastBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)

            val bytes = responseBody.toByteArray()
            exchange.sendResponseHeaders(responseCode, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        val host = "http://localhost:${server.address.port}"
        setMeiliSettings(host)
        repository = MeiliMediaSearchRepository()
    }

    @AfterTest
    fun teardown() {
        server.stop(0)
    }

    @Test
    fun `searchIds parses ids from meili response`() {
        responseBody = """{"hits":[{"id":"m1"},{"id":"m2"}]}"""

        val result = repository.searchIds("matrix", 3, 1)

        assertEquals(listOf("m1", "m2"), result)
        assertEquals("POST", lastMethod)
        assertEquals("/indexes/media_items/search", lastPath)
        assertTrue(lastBody.contains("\"q\":\"matrix\""))
        assertTrue(lastBody.contains("\"limit\":3"))
        assertTrue(lastBody.contains("\"offset\":1"))
    }

    @Test
    fun `upsertDocuments sends request to meili documents endpoint`() {
        responseBody = """{"taskUid":1}"""

        repository.upsertDocuments(listOf(SearchDocument(id = "m1", title = "Matrix")))

        assertEquals("POST", lastMethod)
        assertEquals("/indexes/media_items/documents", lastPath)
        assertTrue(lastBody.contains("\"id\":\"m1\""))
        assertTrue(lastBody.contains("\"title\":\"Matrix\""))
    }

    @Test
    fun `deleteDocument trims id and sends delete request`() {
        responseBody = """{"taskUid":2}"""

        repository.deleteDocument("  m42  ")

        assertEquals("DELETE", lastMethod)
        assertEquals("/indexes/media_items/documents/m42", lastPath)
    }

    @Test
    fun `searchIds throws SearchRequestFailedException on non-2xx response`() {
        responseCode = 500
        responseBody = """{"message":"error"}"""

        assertFailsWith<SearchRequestFailedException> {
            repository.searchIds("boom", 10, 0)
        }
    }

    private fun setMeiliSettings(host: String) {
        val settingsField = MeiliSearchConfig::class.java.getDeclaredField("settings")
        settingsField.isAccessible = true
        settingsField.set(
            MeiliSearchConfig,
            MeiliSearchSettings(
                host = host,
                apiKey = null,
                index = "media_items"
            )
        )
    }
}
