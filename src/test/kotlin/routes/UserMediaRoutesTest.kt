package routes

import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.routes.UserMediaRouting
import com.example.security.TestUserIdProvider
import com.example.security.TestUserPrincipal
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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class UserMediaRoutesTest {
    @Test
    fun `POST creates user media`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"

        every { service.create(eq(userId), any<CreateUserMediaRequest>()) } just Runs

        application {
            configureSerialization()
            configureStatusPages()

            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }
            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.post("/user-media") {
            header(HttpHeaders.Authorization, "Bearer anything")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "mediaId": "media-1",
                  "collectionStatus": "PLANNED",
                  "isFavourite": false,
                  "folderIds": []
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        verify(exactly = 1) { service.create(eq(userId), any<CreateUserMediaRequest>()) }
    }

    @Test
    fun `POST api alias creates user media`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"

        every { service.create(eq(userId), any<CreateUserMediaRequest>()) } just Runs

        application {
            configureSerialization()
            configureStatusPages()

            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }
            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.post("/api/user-media") {
            header(HttpHeaders.Authorization, "Bearer anything")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "mediaId": "media-1",
                  "collectionStatus": "PLANNED",
                  "isFavourite": false,
                  "folderIds": []
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        verify(exactly = 1) { service.create(eq(userId), any<CreateUserMediaRequest>()) }
    }

    @Test
    fun `GET by id returns 404 when not found`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"
        val itemId = "abc123"

        every { service.getById(eq(userId), eq(itemId)) } throws
                UserMediaNotFoundException()

        application {
            configureSerialization()
            configureStatusPages()

            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }
            UserMediaRouting(service, TestUserIdProvider())
        }

            val response = client.get("/user-media/$itemId") {
            header(HttpHeaders.Authorization, "Bearer anything")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify(exactly = 1) { service.getById(userId, itemId) }
    }

    @Test
    fun `PATCH returns 400 when request is invalid`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"
        val itemId = "abc123"

        every { service.update(eq(userId), eq(itemId), any()) } throws
                InvalidUserMediaRequestException("bad")

        application {
            configureSerialization()
            configureStatusPages()

            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }

            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.patch("/user-media/$itemId") {
            header(HttpHeaders.Authorization, "Bearer anything")
            contentType(ContentType.Application.Json)
            setBody("""{"userRating":11.0}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 1) { service.update(eq(userId), eq(itemId), any()) }
    }


    @Test
    fun `PATCH returns 400 if json is malformed and service not called`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"
        val itemId = "abc123"

        application {
            configureSerialization()
            configureStatusPages()

            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }

            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.patch("/user-media/$itemId") {
            header(HttpHeaders.Authorization, "Bearer anything")
            contentType(ContentType.Application.Json)
            setBody("""{"userRating":11.0""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.update(any(), any(), any()) }
    }

    @Test
    fun `GET returns 401 if unauthenticated`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)

        application{
            configureSerialization()
            configureStatusPages()
            install(Authentication){
                bearer("auth-jwt"){
                    authenticate { _ -> null }
                }
            }
            UserMediaRouting(service,TestUserIdProvider())
        }

        val response = client.get("/user-media/abc123"){
            header(HttpHeaders.Authorization, "Bearer anything")
        }
        assertEquals(HttpStatusCode.Unauthorized,response.status)
        verify(exactly = 0) {service.getById(any(),any())}
    }

    @Test
    fun `GET list returns 400 for unknown mediaType`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"

        application {
            configureSerialization()
            configureStatusPages()
            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }
            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.get("/user-media?mediaType=unknown") {
            header(HttpHeaders.Authorization, "Bearer anything")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.getAllMediaItemsByUserId(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GET list returns 400 for unsupported mediaType book`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"

        application {
            configureSerialization()
            configureStatusPages()
            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }
            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.get("/user-media?mediaType=BOOK") {
            header(HttpHeaders.Authorization, "Bearer anything")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.getAllMediaItemsByUserId(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GET list returns 400 for unknown sortBy`() = testApplication {
        val service = mockk<UserMediaService>(relaxed = true)
        val userId = "9492"

        application {
            configureSerialization()
            configureStatusPages()
            install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { _ -> TestUserPrincipal(userId) }
                }
            }
            UserMediaRouting(service, TestUserIdProvider())
        }

        val response = client.get("/user-media?sortBy=weird") {
            header(HttpHeaders.Authorization, "Bearer anything")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.getAllMediaItemsByUserId(any(), any(), any(), any(), any(), any(), any()) }
    }




}
