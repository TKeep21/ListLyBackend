package routes

import com.example.configureSerialization
import com.example.media.Catalog.dto.model.MediaStatus
import com.example.media.InvalidMediaRequestException
import com.example.media.MediaAlreadyExistsException
import com.example.media.MediaCatalogService
import com.example.media.MediaNotFoundException
import com.example.media.dto.CreateMediaRequest
import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem
import com.example.media.model.MediaType
import com.example.plugins.configureStatusPages
import com.example.routes.GlobalMediaRouting
import com.example.security.TestRoleProvider
import com.example.security.TestUserPrincipal
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class GlobalMediaRoutesTest {
    private fun io.ktor.server.application.Application.installTestAuth(role: String) {
        install(Authentication) {
            bearer("auth-jwt") {
                authenticate { _ -> TestUserPrincipal(userId = "test-user", role = role) }
            }
        }
    }

    @Test
    fun `GET by id returns 404 when service returns null`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.findById("67") } returns null

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/mediaCatalog/67")

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify(exactly = 1) { service.findById("67") }
    }

    @Test
    fun `GET by id returns 200 for media route alias`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.findById("67") } returns mediaItem(id = "67", title = "Interstellar")

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/media/67")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.findById("67") }
    }

    @Test
    fun `GET items by title returns 200`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.findAllByTitle("Interstellar") } returns listOf(mediaItem(title = "Interstellar"))

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/mediaCatalog/items/Interstellar")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.findAllByTitle("Interstellar") }
    }

    @Test
    fun `GET items by title returns 400 when service validation fails`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.findAllByTitle("Interstellar") } throws InvalidMediaRequestException("bad")

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/mediaCatalog/items/Interstellar")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 1) { service.findAllByTitle("Interstellar") }
    }

    @Test
    fun `GET discover returns 200 with default pagination`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.discover(12, 0) } returns listOf(mediaItem(id = "m1", title = "Newest"))

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/media/discover")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.discover(12, 0) }
    }

    @Test
    fun `GET discover returns 200 with provided pagination`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.discover(20, 40) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/media/discover?limit=20&offset=40")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.discover(20, 40) }
    }

    @Test
    fun `GET discover returns 400 when pagination is not integer`() = testApplication {
        val service = mockk<MediaCatalogService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/media/discover?limit=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.discover(any(), any()) }
    }

    @Test
    fun `POST create returns 201`() = testApplication {
        val service = mockk<MediaCatalogService>()
        val request = CreateMediaRequest(
            title = "Interstellar",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED
        )
        val created = mediaItem(id = "67", title = "Interstellar")
        every { service.create(request) } returns created

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Interstellar","mediaType":"MOVIE","mediaStatus":"FINISHED"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        verify(exactly = 1) { service.create(request) }
    }

    @Test
    fun `POST create returns 409 when media already exists`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.create(any()) } throws MediaAlreadyExistsException()

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Interstellar","mediaType":"MOVIE","mediaStatus":"FINISHED"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        verify(exactly = 1) { service.create(any()) }
    }

    @Test
    fun `POST create returns 403 for non-admin`() = testApplication {
        val service = mockk<MediaCatalogService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Interstellar","mediaType":"MOVIE","mediaStatus":"FINISHED"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        verify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `POST create returns 401 without token`() = testApplication {
        val service = mockk<MediaCatalogService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Interstellar","mediaType":"MOVIE","mediaStatus":"FINISHED"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `POST create returns 400 for malformed json and service not called`() = testApplication {
        val service = mockk<MediaCatalogService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Interstellar","mediaType":"MOVIE" """)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `PATCH update returns 200`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.updateByAdmin(eq("67"), any()) } returns Unit

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.patch("/mediaCatalog/admin/67") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.updateByAdmin("67", UpdateMediaRequest(title = "Updated")) }
    }

    @Test
    fun `PATCH update returns 404 when media not found`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.updateByAdmin(eq("67"), any()) } throws MediaNotFoundException()

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.patch("/mediaCatalog/admin/67") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify(exactly = 1) { service.updateByAdmin("67", UpdateMediaRequest(title = "Updated")) }
    }

    @Test
    fun `PATCH update returns 400 for malformed json and service not called`() = testApplication {
        val service = mockk<MediaCatalogService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.patch("/mediaCatalog/admin/67") {
            header(HttpHeaders.Authorization, "Bearer token")
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated" """)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.updateByAdmin(any(), any()) }
    }

    @Test
    fun `POST admin reindex returns 200 and triggers reindex`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.reindexSearchIndex() } returns Unit

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog/admin/reindex") {
            header(HttpHeaders.Authorization, "Bearer token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.reindexSearchIndex() }
    }

    @Test
    fun `POST admin reindex returns 403 for non-admin`() = testApplication {
        val service = mockk<MediaCatalogService>(relaxed = true)
        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.post("/mediaCatalog/admin/reindex") {
            header(HttpHeaders.Authorization, "Bearer token")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        verify(exactly = 0) { service.reindexSearchIndex() }
    }

    @Test
    fun `DELETE returns 200`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.delete("67") } returns Unit

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.delete("/mediaCatalog/67") {
            header(HttpHeaders.Authorization, "Bearer token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.delete("67") }
    }

    @Test
    fun `DELETE returns 404 when media not found`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.delete("67") } throws MediaNotFoundException()

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("ADMIN")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.delete("/mediaCatalog/67") {
            header(HttpHeaders.Authorization, "Bearer token")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify(exactly = 1) { service.delete("67") }
    }

    @Test
    fun `GET by id returns 500 on unexpected exception`() = testApplication {
        val service = mockk<MediaCatalogService>()
        every { service.findById("67") } throws RuntimeException("boom")

        application {
            configureSerialization()
            configureStatusPages()
            installTestAuth("USER")
            GlobalMediaRouting(service, TestRoleProvider())
        }

        val response = client.get("/mediaCatalog/67")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        verify(exactly = 1) { service.findById("67") }
    }

    private fun mediaItem(
        id: String = "id-1",
        title: String = "Title",
        mediaType: MediaType = MediaType.MOVIE,
        mediaStatus: MediaStatus = MediaStatus.FINISHED
    ): MediaItem = MediaItem(
        id = id,
        title = title,
        mediaType = mediaType,
        mediaStatus = mediaStatus
    )
}
