package routes

import com.example.auth.AuthService
import com.example.auth.dto.AuthResponse

import com.example.auth.exceptions.EmptyFieldException
import com.example.auth.exceptions.TooManyCharactersInPasswordException
import com.example.auth.exceptions.TooShortLoginException
import com.example.auth.exceptions.UserAlreadyExistsException
import com.example.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.routes.AuthRouting

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType

import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk

import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals


class AuthRoutesTest {

    @Test
    fun `register throws 409 if user already exists`() = testApplication {
        val authService = mockk<AuthService>()
        every {
            authService.register("login1", "password1")
        } throws UserAlreadyExistsException()

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"login1","password":"password1"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        verify(exactly = 1) { authService.register("login1", "password1") }
    }

    @Test
    fun `registration successful 201`() = testApplication {
        val authService = mockk<AuthService>()
        every {
            authService.register("login1", "password1")
        } just Runs
        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"login1","password":"password1"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        verify(exactly = 1) { authService.register("login1", "password1") }
    }

    @Test
    fun `empty field throws 400`() = testApplication {
        val authService = mockk<AuthService>()
        every {
            authService.register("", "password1")
        } throws EmptyFieldException()
        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"","password":"password1"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 1) { authService.register("", "password1") }

    }

    @Test
    fun `password too long throws 400`() = testApplication {
        val authService = mockk<AuthService>()
        val password = "a".repeat(27)
        every {
            authService.register("login1", password)
        } throws TooManyCharactersInPasswordException()

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/register"){
            contentType(ContentType.Application.Json)
            setBody("""{"login":"login1","password":"$password"}""")
        }

        assertEquals(HttpStatusCode.BadRequest,response.status)

        verify(exactly = 1) { authService.register("login1", "a".repeat(27)) }
    }

    @Test
    fun `if login is too short throws 400`() = testApplication {
        val authService = mockk<AuthService>()

        every{
            authService.register("dc", "password1")
        } throws TooShortLoginException()

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/register"){
            contentType(ContentType.Application.Json)
            setBody("""{"login":"dc","password":"password1"}""")
        }
        assertEquals(HttpStatusCode.BadRequest,response.status)

        verify(exactly = 1) { authService.register("dc","password1") }


    }

    @Test
    fun `login returns 200 and token if successful`() = testApplication {
        val authService = mockk<AuthService>()
        val login = "login1"
        val password = "password1"
        val expectedToken = "jwt-token-123"

        every {
            authService.login(login, password)
        } returns expectedToken

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"$login","password":"$password"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(expectedToken, body.token)



        verify(exactly = 1) { authService.login(login, password) }
    }


    @Test
    fun `login returns 400 if json is malformed`() = testApplication {
        val authService = mockk<AuthService>()

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"abc" """)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        verify(exactly = 0) { authService.login(any(), any()) }
    }

    @Test
    fun `login returns 400 if field is missing`() = testApplication {
        val authService = mockk<AuthService>()

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/login"){
            contentType(ContentType.Application.Json)
            setBody("""{"login":"abcd"} """)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        verify(exactly = 0) { authService.login(any(), any()) }

    }

    @Test
    fun `login returns 500 on unexpected exception`() = testApplication {
        val authService = mockk<AuthService>()
        every { authService.login(any(), any()) } throws RuntimeException("boom")

        application {
            configureSerialization()
            configureStatusPages()
            AuthRouting(authService)
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"login1","password":"password1"}""")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        verify(exactly = 1) { authService.login("login1", "password1") }
    }










}