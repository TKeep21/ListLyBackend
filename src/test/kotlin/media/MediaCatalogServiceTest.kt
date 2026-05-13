package media

import com.example.media.Catalog.dto.model.ExternalRef
import com.example.media.Catalog.dto.model.MediaStatus
import com.example.media.InvalidMediaRequestException
import com.example.media.MediaAlreadyExistsException
import com.example.media.MediaCatalogRepository
import com.example.media.MediaCatalogService
import com.example.media.MediaNotFoundException
import com.example.media.dto.CreateMediaRequest
import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem
import com.example.media.model.MediaType
import com.example.search.service.SearchIndexService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MediaCatalogServiceTest {
    private val repository = mockk<MediaCatalogRepository>()
    private val searchIndexService = mockk<SearchIndexService>(relaxed = true)
    private val service = MediaCatalogService(repository, searchIndexService)

    @Test
    fun `findAllByTitle should trim title and return repository result`() {
        val media = mediaItem(title = "Interstellar")
        every { repository.findAllByTitle("Interstellar") } returns listOf(media)

        val result = service.findAllByTitle("  Interstellar  ")

        assertEquals(listOf(media), result)
        verify(exactly = 1) { repository.findAllByTitle("Interstellar") }
    }

    @Test
    fun `findAllByTitle should throw when title is blank`() {
        assertFailsWith<IllegalArgumentException> {
            service.findAllByTitle("   ")
        }

        verify(exactly = 0) { repository.findAllByTitle(any()) }
    }

    @Test
    fun `findById should return media from repository`() {
        val media = mediaItem(id = "m1")
        every { repository.findById("m1") } returns media

        val result = service.findById("m1")

        assertEquals(media, result)
        verify(exactly = 1) { repository.findById("m1") }
    }

    @Test
    fun `findById should return null when media does not exist`() {
        every { repository.findById("missing") } returns null

        val result = service.findById("missing")

        assertNull(result)
        verify(exactly = 1) { repository.findById("missing") }
    }

    @Test
    fun `findById should throw when media id is blank`() {
        assertFailsWith<IllegalArgumentException> {
            service.findById(" ")
        }

        verify(exactly = 0) { repository.findById(any()) }
    }

    @Test
    fun `create should save media successfully`() {
        val request = CreateMediaRequest(
            title = "testGlobal",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.ANNOUNCED,
        )

        val savedItemsSlot = slot<MediaItem>()
        every { repository.save(capture(savedItemsSlot)) } returns Unit
        every { repository.findById(any()) } returns null

        service.create(request)

        verify(exactly = 1) { repository.save(any()) }
        assertEquals("testGlobal", savedItemsSlot.captured.title)
        assertEquals(MediaType.MOVIE, savedItemsSlot.captured.mediaType)
        assertEquals(MediaStatus.ANNOUNCED, savedItemsSlot.captured.mediaStatus)
    }

    @Test
    fun `create should throw error if media already exists`() {
        val request = CreateMediaRequest(
            title = "TestGlobal",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.ANNOUNCED,
            externalRef = ExternalRef(
                provider = "imdb",
                id = "tt0816692",
                url = "https://www.imdb.com/title/tt0816692/"
            )
        )

        every {
            repository.findByExternalRef("imdb", "tt0816692")
        } returns MediaItem(
            id = "67",
            title = "Interstellar",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED,
            externalRef = ExternalRef(
                provider = "imdb",
                id = "tt0816692",
                url = "https://www.imdb.com/title/tt0816692/"
            )
        )

        assertFailsWith<MediaAlreadyExistsException> {
            service.create(request)
        }

        verify(exactly = 1) { repository.findByExternalRef("imdb", "tt0816692") }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `create should normalize fields before save`() {
        val request = CreateMediaRequest(
            title = "  Interstellar  ",
            description = "  epic sci fi  ",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED,
            genres = listOf("  Sci-Fi ", " ", "Drama"),
            posterUrl = "  https://img.test/poster.jpg  "
        )
        val savedItemsSlot = slot<MediaItem>()
        every { repository.save(capture(savedItemsSlot)) } returns Unit
        every { repository.findById(any()) } returns null

        service.create(request)

        assertEquals("Interstellar", savedItemsSlot.captured.title)
        assertEquals("epic sci fi", savedItemsSlot.captured.description)
        assertEquals(listOf("Sci-Fi", "Drama"), savedItemsSlot.captured.genres)
        assertEquals("https://img.test/poster.jpg", savedItemsSlot.captured.posterUrl)
    }

    @Test
    fun `create should throw for blank title`() {
        val request = CreateMediaRequest(
            title = "   ",
            mediaType = MediaType.SERIES,
            mediaStatus = MediaStatus.ONGOING
        )

        assertFailsWith<InvalidMediaRequestException> {
            service.create(request)
        }

        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `create should throw for blank external provider`() {
        val request = CreateMediaRequest(
            title = "Dark",
            mediaType = MediaType.SERIES,
            mediaStatus = MediaStatus.FINISHED,
            externalRef = ExternalRef(provider = " ", id = "123", url = null)
        )

        assertFailsWith<InvalidMediaRequestException> {
            service.create(request)
        }

        verify(exactly = 0) { repository.findByExternalRef(any(), any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `updateByAdmin should validate and call repository update when media exists`() {
        val mediaId = "m1"
        val request = UpdateMediaRequest(
            title = " Updated title ",
            description = " Updated desc "
        )
        every { repository.findById(mediaId) } returns mediaItem(id = mediaId)
        every { repository.update(mediaId, request) } returns Unit

        service.updateByAdmin(mediaId, request)

        verify(exactly = 2) { repository.findById(mediaId) }
        verify(exactly = 1) { repository.update(mediaId, request) }
    }

    @Test
    fun `updateByAdmin should throw when id is blank`() {
        val request = UpdateMediaRequest(title = "Updated")

        assertFailsWith<IllegalArgumentException> {
            service.updateByAdmin(" ", request)
        }

        verify(exactly = 0) { repository.findById(any()) }
        verify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `updateByAdmin should throw when media does not exist`() {
        val mediaId = "missing"
        every { repository.findById(mediaId) } returns null

        assertFailsWith<MediaNotFoundException> {
            service.updateByAdmin(mediaId, UpdateMediaRequest(title = "Updated"))
        }

        verify(exactly = 1) { repository.findById(mediaId) }
        verify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `updateByAdmin should throw for blank title in request`() {
        val mediaId = "m1"
        every { repository.findById(mediaId) } returns mediaItem(id = mediaId)

        assertFailsWith<InvalidMediaRequestException> {
            service.updateByAdmin(mediaId, UpdateMediaRequest(title = "   "))
        }

        verify(exactly = 1) { repository.findById(mediaId) }
        verify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `delete should call repository delete when media exists`() {
        val mediaId = "m1"
        every { repository.findById(mediaId) } returns mediaItem(id = mediaId)
        every { repository.delete(mediaId) } returns Unit

        service.delete(mediaId)

        verify(exactly = 1) { repository.findById(mediaId) }
        verify(exactly = 1) { repository.delete(mediaId) }
    }

    @Test
    fun `delete should throw when media does not exist`() {
        val mediaId = "missing"
        every { repository.findById(mediaId) } returns null

        assertFailsWith<MediaNotFoundException> {
            service.delete(mediaId)
        }

        verify(exactly = 1) { repository.findById(mediaId) }
        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `adjustUserRating should pass deltas to repository`() {
        every { repository.adjustUserRating("m1", 2.5, 1) } returns Unit

        service.adjustUserRating("m1", 2.5, 1)

        verify(exactly = 1) { repository.adjustUserRating("m1", 2.5, 1) }
    }

    @Test
    fun `adjustUserRating should throw when media id is blank`() {
        assertFailsWith<IllegalArgumentException> {
            service.adjustUserRating(" ", 1.0, 1)
        }

        verify(exactly = 0) { repository.adjustUserRating(any(), any(), any()) }
    }

    private fun mediaItem(
        id: String = "id-1",
        title: String = "Title",
        mediaType: MediaType = MediaType.MOVIE,
        mediaStatus: MediaStatus = MediaStatus.ANNOUNCED
    ): MediaItem = MediaItem(
        id = id,
        title = title,
        mediaType = mediaType,
        mediaStatus = mediaStatus
    )

}
