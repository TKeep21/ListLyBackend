package search.service

import com.example.media.MediaCatalogService
import com.example.media.model.MediaItem
import com.example.media.model.MediaType
import com.example.media.Catalog.dto.model.MediaStatus
import com.example.search.exceptions.InvalidSearchRequestException
import com.example.search.exceptions.MeiliClientException
import com.example.search.exceptions.SearchUnavailableException
import com.example.search.repository.SearchReadRepository
import com.example.search.service.MeiliMediaSearchServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeiliMediaSearchServiceImplTest {
    private val repository = mockk<SearchReadRepository>()
    private val mediaCatalogService = mockk<MediaCatalogService>()
    private val service = MeiliMediaSearchServiceImpl(repository, mediaCatalogService)

    @Test
    fun `search returns empty list for blank query`() {
        val result = service.search("   ", limit = 10, offset = 0)

        assertEquals(emptyList(), result)
        verify(exactly = 0) { repository.searchIds(any(), any(), any()) }
    }

    @Test
    fun `search throws on invalid limit`() {
        assertFailsWith<InvalidSearchRequestException> {
            service.search("matrix", limit = 0, offset = 0)
        }
    }

    @Test
    fun `search throws on negative offset`() {
        assertFailsWith<InvalidSearchRequestException> {
            service.search("matrix", limit = 10, offset = -1)
        }
    }

    @Test
    fun `search trims and limits query before repository call`() {
        val longQuery = "  " + "a".repeat(200) + "  "
        every { repository.searchIds(any(), 12, 0) } returns listOf("m1")
        every { mediaCatalogService.findByIds(listOf("m1")) } returns listOf(mediaItem("m1"))

        service.search(longQuery, limit = 12, offset = 0)

        verify(exactly = 1) { repository.searchIds("a".repeat(150), 12, 0) }
    }

    @Test
    fun `search maps meili exception to SearchUnavailableException`() {
        every { repository.searchIds("matrix", 10, 0) } throws MeiliClientException("boom")

        assertFailsWith<SearchUnavailableException> {
            service.search("matrix", limit = 10, offset = 0)
        }
    }

    @Test
    fun `search returns media from catalog by ids order`() {
        val expected = listOf(mediaItem("m1"), mediaItem("m2"))
        every { repository.searchIds("matrix", 10, 2) } returns listOf("m1", "m2")
        every { mediaCatalogService.findByIds(listOf("m1", "m2")) } returns expected

        val result = service.search("matrix", limit = 10, offset = 2)

        assertEquals(expected, result)
    }

    private fun mediaItem(id: String): MediaItem = MediaItem(
        id = id,
        title = "Title-$id",
        mediaType = MediaType.MOVIE,
        mediaStatus = MediaStatus.FINISHED
    )
}
