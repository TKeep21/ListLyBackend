package userFolder

import com.example.UserFolder.UserFolderRepository
import com.example.UserFolder.UserFolderService
import com.example.UserFolder.dto.CreateUserFolderRequest
import com.example.UserFolder.dto.UpdateUserFolderRequest
import com.example.UserFolder.exceptions.ForbiddenUserFolderAccessException
import com.example.UserFolder.exceptions.InvalidUserFolderRequestException
import com.example.UserFolder.exceptions.UserFolderAlreadyExistsException
import com.example.UserFolder.model.UserFolder
import com.example.UserMedia.UserMediaRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserFolderServiceTest {
    private val folderRepository = mockk<UserFolderRepository>()
    private val userMediaRepository = mockk<UserMediaRepository>()
    private val service = UserFolderService(folderRepository, userMediaRepository)

    @Test
    fun `create folder`() {
        val userId = "u1"
        val folderSlot = slot<UserFolder>()
        every { folderRepository.findAllByUserId(userId) } returns emptyList()
        every { folderRepository.save(capture(folderSlot)) } just Runs

        val result = service.create(userId, CreateUserFolderRequest("  абра  "))

        assertEquals("абра", result.name)
        assertEquals("абра", folderSlot.captured.name)
        assertEquals(userId, folderSlot.captured.userId)
    }

    @Test
    fun `reject empty folder name`() {
        assertFailsWith<InvalidUserFolderRequestException> {
            service.create("u1", CreateUserFolderRequest("   "))
        }
    }

    @Test
    fun `reject duplicate folder names case insensitive`() {
        every { folderRepository.findAllByUserId("u1") } returns listOf(
            UserFolder(id = "f1", userId = "u1", name = "My Folder")
        )

        assertFailsWith<UserFolderAlreadyExistsException> {
            service.create("u1", CreateUserFolderRequest("my folder"))
        }
    }

    @Test
    fun `delete folder`() {
        val userId = "u1"
        val folderId = "f1"
        every { folderRepository.findById(folderId) } returns UserFolder(id = folderId, userId = userId, name = "A")
        every { folderRepository.delete(userId, folderId) } just Runs
        every { userMediaRepository.removeFolderIdFromAllUserMedia(userId, folderId) } just Runs

        service.delete(userId, folderId)

        verify(exactly = 1) { folderRepository.delete(userId, folderId) }
    }

    @Test
    fun `delete folder removes folderId from user media`() {
        val userId = "u1"
        val folderId = "f1"
        every { folderRepository.findById(folderId) } returns UserFolder(id = folderId, userId = userId, name = "A")
        every { folderRepository.delete(userId, folderId) } just Runs
        every { userMediaRepository.removeFolderIdFromAllUserMedia(userId, folderId) } just Runs

        service.delete(userId, folderId)

        verify(exactly = 1) { userMediaRepository.removeFolderIdFromAllUserMedia(userId, folderId) }
    }

    @Test
    fun `cannot delete foreign folder`() {
        every { folderRepository.findById("f1") } returns UserFolder(id = "f1", userId = "other", name = "A")

        assertFailsWith<ForbiddenUserFolderAccessException> {
            service.delete("u1", "f1")
        }
    }

    @Test
    fun `rename folder trims and validates uniqueness`() {
        val userId = "u1"
        val folderId = "f1"
        every { folderRepository.findById(folderId) } returns UserFolder(id = folderId, userId = userId, name = "Old")
        every { folderRepository.findAllByUserId(userId) } returns listOf(
            UserFolder(id = folderId, userId = userId, name = "Old"),
            UserFolder(id = "f2", userId = userId, name = "Anime")
        )
        every { folderRepository.updateName(userId, folderId, "Movies") } just Runs

        service.rename(userId, folderId, UpdateUserFolderRequest("  Movies "))

        verify(exactly = 1) { folderRepository.updateName(userId, folderId, "Movies") }
    }
}
