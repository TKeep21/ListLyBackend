package userMedia

import com.example.UserFolder.UserFolderService
import com.example.UserFolder.exceptions.ForbiddenUserFolderAccessException
import com.example.UserMedia.UserMediaRepository
import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaFoldersRequest
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaAlreadyExistsException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.SortDirection
import com.example.UserMedia.model.UserCollectionStatus
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaSortBy
import com.example.media.Catalog.dto.model.MediaStatus
import com.example.media.MediaCatalogService
import com.example.media.model.MediaItem
import com.example.media.model.MediaType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserMediaServiceTest {
    private val repository = mockk<UserMediaRepository>()
    private val mediaCatalogService = mockk<MediaCatalogService>(relaxed = true)
    private val service = UserMediaService(repository, mediaCatalogService)

    @Test
    fun `Get UserMediaItem if it exists`() {
        val testId = ObjectId().toString()
        val testUserId = "9492"
        val item = UserMediaItem(
            id = testId,
            userId = testUserId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED,
            userRating = 0.0,
            note = "test",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        every { repository.findById(testUserId, testId) } returns item

        val result = service.getById(testUserId, testId)

        assertEquals(item, result)
        verify(exactly = 1) { repository.findById(testUserId, testId) }
    }

    @Test
    fun `throw exception if note in update is more than 400`() {
        val testId = ObjectId().toString()
        val testUserId = "9492"

        val item = UserMediaItem(
            id = testId,
            userId = testUserId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED,
            userRating = 0.0,
            note = "old",
            createdAt = 1L,
            updatedAt = 1L
        )

        every { repository.findById(testUserId, testId) } returns item

        val request = UpdateUserMediaRequest(note = "a".repeat(401))

        assertFailsWith<InvalidUserMediaRequestException> {
            service.update(testUserId, testId, request)
        }

        verify(exactly = 1) { repository.findById(testUserId, testId) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `throw exception if userRating in update is not in range`() {
        val testId = ObjectId().toString()
        val testUserId = "9492"

        val item = UserMediaItem(
            id = testId,
            userId = testUserId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED,
            userRating = 0.0,
            note = "note",
            createdAt = 1L,
            updatedAt = 1L
        )

        every { repository.findById(testUserId, testId) } returns item

        val request = UpdateUserMediaRequest(userRating = 11.0)

        assertFailsWith<InvalidUserMediaRequestException> {
            service.update(testUserId, testId, request)
        }

        verify(exactly = 1) { repository.findById(testUserId, testId) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `update successfully updates fields and applies rating delta`() {
        val userId = "9492"
        val id = ObjectId().toString()

        val item = UserMediaItem(
            id = id,
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED,
            userRating = 5.0,
            note = "old note",
            createdAt = 1L,
            updatedAt = 1L
        )

        every { repository.findById(userId, id) } returns item
        every { repository.update(userId, id, any()) } just Runs

        val request = UpdateUserMediaRequest(
            note = "new note",
            userRating = 8.0
        )

        service.update(userId, id, request)

        verify(exactly = 1) { repository.update(userId, id, any()) }
        verify(exactly = 1) { mediaCatalogService.adjustUserRating("media-1", 3.0, 0) }
    }

    @Test
    fun `delete throws exception if not exists`() {
        val testId = ObjectId().toString()
        val testUserId = "9492"

        every { repository.findById(testUserId, testId) } returns null

        assertFailsWith<UserMediaNotFoundException> {
            service.delete(testUserId, testId)
        }

        verify(exactly = 0) { repository.delete(testUserId, testId) }
    }

    @Test
    fun `getById throws if not found`() {
        every { repository.findById("u", "id") } returns null
        assertFailsWith<UserMediaNotFoundException> { service.getById("u", "id") }
    }

    @Test
    fun `create throws if item already exists`() {
        val userId = "u"
        val item = UserMediaItem(userId = userId, mediaId = "media-1", collectionStatus = UserCollectionStatus.PLANNED)
        every { mediaCatalogService.findById("media-1") } returns MediaItem(
            id = "media-1",
            title = "Interstellar",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED
        )
        every { repository.findByMediaIdAndUserId(userId, "media-1") } returns item

        assertFailsWith<UserMediaAlreadyExistsException> { service.create(userId, item) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `delete without rating only removes user item`() {
        val userId = "u"
        val id = "id"
        every { repository.findById(userId, id) } returns UserMediaItem(
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED
        )
        every { repository.delete(userId, id) } just Runs

        service.delete(userId, id)

        verify(exactly = 1) { repository.delete(userId, id) }
        verify(exactly = 0) { mediaCatalogService.adjustUserRating(any(), any(), any()) }
    }

    @Test
    fun `create saves media item if not exists`() {
        val userId = "u"

        val item = UserMediaItem(
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED
        )

        every { repository.findByMediaIdAndUserId(userId, "media-1") } returns null
        every { mediaCatalogService.findById("media-1") } returns MediaItem(
            id = "media-1",
            title = "Interstellar",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED
        )
        every { repository.save(any()) } just Runs

        service.create(userId, item)

        verify(exactly = 1) { repository.findByMediaIdAndUserId(userId, "media-1") }
        verify(exactly = 0) { mediaCatalogService.adjustUserRating(any(), any(), any()) }
    }

    @Test
    fun `create with rating increments global media aggregate`() {
        val userId = "u"
        val item = UserMediaItem(
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED,
            userRating = 8.5
        )

        every { repository.findByMediaIdAndUserId(userId, "media-1") } returns null
        every { mediaCatalogService.findById("media-1") } returns MediaItem(
            id = "media-1",
            title = "Interstellar",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED
        )
        every { repository.save(any()) } just Runs

        service.create(userId, item)

        verify(exactly = 1) { mediaCatalogService.adjustUserRating("media-1", 8.5, 1) }
    }

    @Test
    fun `create request maps to planned status and current user id`() {
        val userId = "u"
        val request = CreateUserMediaRequest(
            mediaId = "media-1",
            userRating = 7.0,
            note = "great"
        )
        val savedSlot = slot<UserMediaItem>()

        every { repository.findByMediaIdAndUserId(userId, "media-1") } returns null
        every { mediaCatalogService.findById("media-1") } returns MediaItem(
            id = "media-1",
            title = "Interstellar",
            mediaType = MediaType.MOVIE,
            mediaStatus = MediaStatus.FINISHED
        )
        every { repository.save(capture(savedSlot)) } just Runs

        service.create(userId, request)

        assertEquals(userId, savedSlot.captured.userId)
        assertEquals(UserCollectionStatus.PLANNED, savedSlot.captured.collectionStatus)
        assertEquals(7.0, savedSlot.captured.userRating)
        assertEquals("great", savedSlot.captured.note)
    }

    @Test
    fun `delete with rating decrements global media aggregate`() {
        val userId = "u"
        val id = "id"
        every { repository.findById(userId, id) } returns UserMediaItem(
            id = id,
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.COMPLETED,
            userRating = 8.5
        )
        every { repository.delete(userId, id) } just Runs

        service.delete(userId, id)

        verify(exactly = 1) { mediaCatalogService.adjustUserRating("media-1", -8.5, -1) }
    }

    @Test
    fun `add folder ids to user media`() {
        val userId = "u1"
        val itemId = "item-1"
        val folderService = mockk<UserFolderService>()
        val serviceWithFolders = UserMediaService(repository, mediaCatalogService, folderService)
        every { repository.findById(userId, itemId) } returns UserMediaItem(
            id = itemId,
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED
        )
        every { folderService.validateFolderOwnership(userId, listOf("folder-1", "folder-2")) } just Runs
        every { repository.updateFolders(userId, itemId, listOf("folder-1", "folder-2")) } just Runs

        serviceWithFolders.updateFolders(
            userId = userId,
            userMediaId = itemId,
            request = UpdateUserMediaFoldersRequest(folderIds = listOf("folder-1", "folder-2"))
        )

        verify(exactly = 1) { folderService.validateFolderOwnership(userId, listOf("folder-1", "folder-2")) }
        verify(exactly = 1) { repository.updateFolders(userId, itemId, listOf("folder-1", "folder-2")) }
    }

    @Test
    fun `forbid foreign folder`() {
        val userId = "u1"
        val itemId = "item-1"
        val folderService = mockk<UserFolderService>()
        val serviceWithFolders = UserMediaService(repository, mediaCatalogService, folderService)
        every { repository.findById(userId, itemId) } returns UserMediaItem(
            id = itemId,
            userId = userId,
            mediaId = "media-1",
            collectionStatus = UserCollectionStatus.PLANNED
        )
        every {
            folderService.validateFolderOwnership(userId, listOf("foreign-folder"))
        } throws ForbiddenUserFolderAccessException("foreign-folder")

        assertFailsWith<ForbiddenUserFolderAccessException> {
            serviceWithFolders.updateFolders(
                userId = userId,
                userMediaId = itemId,
                request = UpdateUserMediaFoldersRequest(folderIds = listOf("foreign-folder"))
            )
        }

        verify(exactly = 0) { repository.updateFolders(any(), any(), any()) }
    }

    @Test
    fun `filter by status`() {
        every {
            repository.findAllByUser("u", UserCollectionStatus.COMPLETED, null, null)
        } returns emptyList()

        service.getAllMediaItemsByUserId("u", status = UserCollectionStatus.COMPLETED)

        verify(exactly = 1) { repository.findAllByUser("u", UserCollectionStatus.COMPLETED, null, null) }
    }

    @Test
    fun `filter by favourite`() {
        every {
            repository.findAllByUser("u", null, true, null)
        } returns emptyList()

        service.getAllMediaItemsByUserId("u", favourite = true)

        verify(exactly = 1) { repository.findAllByUser("u", null, true, null) }
    }

    @Test
    fun `filter by folderId`() {
        every {
            repository.findAllByUser("u", null, null, "folder-1")
        } returns emptyList()

        service.getAllMediaItemsByUserId("u", folderId = "folder-1")

        verify(exactly = 1) { repository.findAllByUser("u", null, null, "folder-1") }
    }

    @Test
    fun `filter by media type uses linked media`() {
        val movieItem = UserMediaItem(id = "um-1", userId = "u", mediaId = "movie-1")
        val bookItem = UserMediaItem(id = "um-2", userId = "u", mediaId = "book-1")
        every { repository.findAllByUser("u", null, null, null) } returns listOf(movieItem, bookItem)
        every { mediaCatalogService.findByIds(listOf("movie-1", "book-1")) } returns listOf(
            mediaItem(id = "movie-1", title = "Arrival", mediaType = MediaType.MOVIE),
            mediaItem(id = "book-1", title = "Dune", mediaType = MediaType.BOOK)
        )

        val result = service.getAllMediaItemsByUserId("u", mediaType = MediaType.MOVIE)

        assertEquals(listOf(movieItem), result)
        verify(exactly = 1) { mediaCatalogService.findByIds(listOf("movie-1", "book-1")) }
    }

    @Test
    fun `sort by title uses linked media title`() {
        val zItem = UserMediaItem(id = "um-1", userId = "u", mediaId = "z-media")
        val aItem = UserMediaItem(id = "um-2", userId = "u", mediaId = "a-media")
        every { repository.findAllByUser("u", null, null, null) } returns listOf(zItem, aItem)
        every { mediaCatalogService.findByIds(listOf("z-media", "a-media")) } returns listOf(
            mediaItem(id = "z-media", title = "Zodiac", mediaType = MediaType.MOVIE),
            mediaItem(id = "a-media", title = "Arrival", mediaType = MediaType.MOVIE)
        )

        val result = service.getAllMediaItemsByUserId(
            userId = "u",
            sortBy = UserMediaSortBy.TITLE,
            sortDirection = SortDirection.ASC
        )

        assertEquals(listOf(aItem, zItem), result)
    }

    @Test
    fun `sort by created date desc returns latest first`() {
        val oldItem = UserMediaItem(id = "old", userId = "u", mediaId = "m1", createdAt = 1L)
        val newItem = UserMediaItem(id = "new", userId = "u", mediaId = "m2", createdAt = 2L)
        every { repository.findAllByUser("u", null, null, null) } returns listOf(oldItem, newItem)

        val result = service.getAllMediaItemsByUserId("u")

        assertEquals(listOf(newItem, oldItem), result)
        verify(exactly = 0) { mediaCatalogService.findByIds(any()) }
    }

    private fun mediaItem(
        id: String,
        title: String,
        mediaType: MediaType
    ) = MediaItem(
        id = id,
        title = title,
        mediaType = mediaType,
        mediaStatus = MediaStatus.FINISHED
    )
}
