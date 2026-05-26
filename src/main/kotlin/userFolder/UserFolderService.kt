package com.example.UserFolder

import com.example.UserFolder.dto.CreateUserFolderRequest
import com.example.UserFolder.dto.UpdateUserFolderRequest
import com.example.UserFolder.exceptions.ForbiddenUserFolderAccessException
import com.example.UserFolder.exceptions.InvalidUserFolderRequestException
import com.example.UserFolder.exceptions.UserFolderAlreadyExistsException
import com.example.UserFolder.exceptions.UserFolderNotFoundException
import com.example.UserFolder.model.UserFolder
import com.example.UserMedia.UserMediaRepository

class UserFolderService(
    private val userFolderRepository: UserFolderRepository,
    private val userMediaRepository: UserMediaRepository
) {
    private val defaultFolderNames = listOf(
        "watched",
        "watching",
        "planned",
        "dropped"
    )

    fun create(userId: String, request: CreateUserFolderRequest): UserFolder {
        val normalizedName = normalizeAndValidateName(request.name)
        ensureUniqueName(userId, normalizedName)

        val folder = UserFolder(
            userId = userId,
            name = normalizedName
        )
        userFolderRepository.save(folder)
        return folder
    }

    fun getAllByUserId(userId: String): List<UserFolder> {
        ensureDefaultFolders(userId)
        return userFolderRepository.findAllByUserId(userId)
    }

    fun rename(userId: String, folderId: String, request: UpdateUserFolderRequest) {
        val folder = userFolderRepository.findById(folderId)
            ?: throw UserFolderNotFoundException(folderId)
        if (folder.userId != userId) throw ForbiddenUserFolderAccessException(folderId)

        val normalizedName = normalizeAndValidateName(request.name)
        ensureUniqueName(userId, normalizedName, ignoredFolderId = folderId)

        userFolderRepository.updateName(userId, folderId, normalizedName)
    }

    fun delete(userId: String, folderId: String) {
        val folder = userFolderRepository.findById(folderId)
            ?: throw UserFolderNotFoundException(folderId)
        if (folder.userId != userId) throw ForbiddenUserFolderAccessException(folderId)

        userFolderRepository.delete(userId, folderId)
        userMediaRepository.removeFolderIdFromAllUserMedia(userId, folderId)
    }

    fun validateFolderOwnership(userId: String, folderIds: List<String>) {
        val uniqueFolderIds = folderIds.distinct()
        val ownedFolderIds = userFolderRepository.findByIdsAndUserId(userId, uniqueFolderIds)
            .map { it.id }
            .toSet()

        val notOwned = uniqueFolderIds.filterNot { it in ownedFolderIds }
        if (notOwned.isNotEmpty()) {
            throw ForbiddenUserFolderAccessException(notOwned.first())
        }
    }

    private fun normalizeAndValidateName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw InvalidUserFolderRequestException("Folder name must not be empty")
        }
        if (trimmed.length !in 1..40) {
            throw InvalidUserFolderRequestException("Folder name length must be from 1 to 40 characters")
        }
        return trimmed
    }

    private fun ensureUniqueName(userId: String, name: String, ignoredFolderId: String? = null) {
        val duplicateExists = userFolderRepository.findAllByUserId(userId)
            .any { existing ->
                existing.id != ignoredFolderId && existing.name.equals(name, ignoreCase = true)
            }

        if (duplicateExists) {
            throw UserFolderAlreadyExistsException(name)
        }
    }

    private fun ensureDefaultFolders(userId: String) {
        val existingNames = userFolderRepository.findAllByUserId(userId)
            .map { it.name.lowercase() }
            .toSet()

        defaultFolderNames
            .filterNot { it.lowercase() in existingNames }
            .forEach { name ->
                userFolderRepository.save(
                    UserFolder(
                        userId = userId,
                        name = name
                    )
                )
            }
    }
}
