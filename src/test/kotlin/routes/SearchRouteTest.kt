package routes

import com.example.configureSerialization
import com.example.media.MediaCatalogService
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
        val mediaCatalogService = mockk<MediaCatalogService>(relaxed = true)
        every { service.search("interstellar", 5, 10) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service, mediaCatalogService)
        }

        val response = client.get("/media/search?query=interstellar&limit=5&offset=10")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.search("interstellar", 5, 10) }
    }

    @Test
    fun `GET media search uses defaults for limit and offset`() = testApplication {
        val service = mockk<SearchService>()
        val mediaCatalogService = mockk<MediaCatalogService>(relaxed = true)
        every { service.search("dune", 12, 0) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service, mediaCatalogService)
        }

        val response = client.get("/media/search?query=dune")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { service.search("dune", 12, 0) }
    }

    @Test
    fun `GET media search returns 400 when service throws InvalidSearchRequestException`() = testApplication {
        val service = mockk<SearchService>()
        val mediaCatalogService = mockk<MediaCatalogService>(relaxed = true)
        every { service.search("dune", 100, 0) } throws InvalidSearchRequestException("Limit must be between 1 and 50")

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service, mediaCatalogService)
        }

        val response = client.get("/media/search?query=dune&limit=100")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET media discover returns 200 and delegates params`() = testApplication {
        val service = mockk<SearchService>(relaxed = true)
        val mediaCatalogService = mockk<MediaCatalogService>()
        every { mediaCatalogService.findPage(6, 3) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service, mediaCatalogService)
        }

        val response = client.get("/media/discover?limit=6&offset=3")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { mediaCatalogService.findPage(6, 3) }
    }

    @Test
    fun `GET media discover uses defaults for limit and offset`() = testApplication {
        val service = mockk<SearchService>(relaxed = true)
        val mediaCatalogService = mockk<MediaCatalogService>()
        every { mediaCatalogService.findPage(12, 0) } returns emptyList()

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service, mediaCatalogService)
        }

        val response = client.get("/media/discover")

        assertEquals(HttpStatusCode.OK, response.status)
        verify(exactly = 1) { mediaCatalogService.findPage(12, 0) }
    }

    @Test
    fun `GET media discover returns 400 when service throws InvalidSearchRequestException`() = testApplication {
        val service = mockk<SearchService>(relaxed = true)
        val mediaCatalogService = mockk<MediaCatalogService>()
        every { mediaCatalogService.findPage(100, 0) } throws InvalidSearchRequestException("Limit must be between 1 and 50")

        application {
            configureSerialization()
            configureStatusPages()
            searchRoute(service, mediaCatalogService)
        }

        val response = client.get("/media/discover?limit=100")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
