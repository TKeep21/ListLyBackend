package routes

import com.example.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.routes.HealthRouting
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun `GET health returns 200`() = testApplication {
        application {
            configureSerialization()
            configureStatusPages()
            HealthRouting()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
