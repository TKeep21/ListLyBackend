package routes

import com.example.UserMedia.UserMediaService
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.SortDirection
import com.example.UserMedia.model.UserCollectionStatus
import com.example.UserMedia.model.UserMediaSortBy
import com.example.configureSerialization
import com.example.media.model.MediaType
import com.example.plugins.configureStatusPages
import com.example.routes.UserMediaRouting
import com.example.security.TestUserIdProvider
import com.example.security.TestUserPrincipal
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
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

class UserMediaRoutesTest {

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
    fun `GET collection passes filter and sort query params to service`() = testApplication {
        val service = mockk<UserMediaService>()
        val userId = "9492"

        every {
            service.getAllMediaItemsByUserId(
                userId = userId,
                status = UserCollectionStatus.COMPLETED,
                favourite = true,
                folderId = "folder-1",
                mediaType = MediaType.MOVIE,
                sortBy = UserMediaSortBy.TITLE,
                sortDirection = SortDirection.ASC
            )
        } returns emptyList()

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

        val response = client.get(
            "/user-media?status=completed&favourite=true&folderId=folder-1&mediaType=movie&sortBy=title&sortDirection=asc"
        ) {
            header(HttpHeaders.Authorization, "Bearer anything")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) {
            service.getAllMediaItemsByUserId(
                userId,
                UserCollectionStatus.COMPLETED,
                true,
                "folder-1",
                MediaType.MOVIE,
                UserMediaSortBy.TITLE,
                SortDirection.ASC
            )
        }
    }

    @Test
    fun `GET collection returns 400 for unknown media type`() = testApplication {
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
        verify(exactly = 0) {
            service.getAllMediaItemsByUserId(any(), any(), any(), any(), any(), any(), any())
        }
    }



}
