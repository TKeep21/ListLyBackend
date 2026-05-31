package routes

import com.example.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.search.exceptions.InvalidSearchRequestException
import com.example.search.service.SearchService
import com.example.routes.searchRoute
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchRouteTest {
    @Test
    fun `GET media search returns 200 and delegates params`() = testApplication {
        val service = mockk<SearchService>()
        every { service.search("interstellar", 5, 10) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service)
        }

        val response = client.get("/media/search?query=interstellar&limit=5&offset=10")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.search("interstellar", 5, 10) }
    }

    @Test
    fun `GET media search uses defaults for limit and offset`() = testApplication {
        val service = mockk<SearchService>()
        every { service.search("dune", 12, 0) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service)
        }

        val response = client.get("/media/search?query=dune")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.search("dune", 12, 0) }
    }

    @Test
    fun `GET media search returns 400 when service throws InvalidSearchRequestException`() = testApplication {
        val service = mockk<SearchService>()
        every { service.search("dune", 100, 0) } throws InvalidSearchRequestException("Limit must be between 1 and 50")

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service)
        }

        val response = client.get("/media/search?query=dune&limit=100")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET media search returns 400 for non-integer limit`() = testApplication {
        val service = mockk<SearchService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service)
        }

        val response = client.get("/media/search?query=dune&limit=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.search(any(), any(), any()) }
    }

    @Test
    fun `GET media search returns 400 for non-integer offset`() = testApplication {
        val service = mockk<SearchService>(relaxed = true)

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service)
        }

        val response = client.get("/media/search?query=dune&offset=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        verify(exactly = 0) { service.search(any(), any(), any()) }
    }
}
