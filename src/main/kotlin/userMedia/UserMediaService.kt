package com.example.UserMedia

import com.example.UserFolder.UserFolderService
import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaFavouriteRequest
import com.example.UserMedia.dto.UpdateUserMediaFoldersRequest
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaStatusRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaAlreadyExistsException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.SortDirection
import com.example.UserMedia.model.UserCollectionStatus
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaSortBy
import com.example.media.MediaCatalogService
import com.example.media.MediaNotFoundException
import com.example.media.model.MediaItem
import com.example.media.model.MediaType

class UserMediaService(
    private val userMediaRepository: UserMediaRepository,
    private val mediaCatalogService: MediaCatalogService,
    private val userFolderService: UserFolderService? = null
) {

    fun getAllMediaItemsByUserId(
        userId: String,
        status: UserCollectionStatus? = null,
        favourite: Boolean? = null,
        folderId: String? = null,
        mediaType: MediaType? = null,
        sortBy: UserMediaSortBy = UserMediaSortBy.CREATED_AT,
        sortDirection: SortDirection = SortDirection.DESC
    ): List<UserMediaItem> {
        val items = userMediaRepository.findAllByUser(userId, status, favourite, folderId)
        if (items.isEmpty()) return emptyList()

        val mediaById = loadMediaByIdIfNeeded(items, mediaType, sortBy)
        val filteredItems = if (mediaType == null) {
            items
        } else {
            items.filter { mediaById[it.mediaId]?.mediaType == mediaType }
        }

        return filteredItems.sortedBy(sortBy, sortDirection, mediaById)
    }

    fun getById(userId: String, userMediaId: String): UserMediaItem {
        return userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)
    }

    fun create(userId: String, request: CreateUserMediaRequest) {
        val item = UserMediaItem(
            userId = userId,
            mediaId = request.mediaId,
            collectionStatus = request.collectionStatus ?: UserCollectionStatus.PLANNED,
            isFavourite = request.isFavourite,
            folderIds = request.folderIds.distinct(),
            userRating = request.userRating,
            note = request.note
        )
        create(userId, item)
    }

    fun create(userId: String, item: UserMediaItem) {
        validateRating(item.userRating)
        validateNote(item.note)
        validateFolderIds(userId, item.folderIds)

        val mediaId = item.mediaId
        mediaCatalogService.findById(mediaId) ?: throw MediaNotFoundException()

        val existing = userMediaRepository.findByMediaIdAndUserId(userId, mediaId)
        if (existing != null) throw UserMediaAlreadyExistsException(userId, mediaId)

        val safeItem = UserMediaItem(
            id = item.id,
            userId = userId,
            mediaId = mediaId,
            collectionStatus = item.collectionStatus,
            isFavourite = item.isFavourite,
            folderIds = item.folderIds.distinct(),
            userRating = item.userRating,
            note = item.note,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )

        userMediaRepository.save(safeItem)

        safeItem.userRating?.let { rating ->
            mediaCatalogService.adjustUserRating(mediaId, ratingDelta = rating, countDelta = 1)
        }
    }

    fun update(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaRequest
    ) {
        val existing = userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        validateRating(request.userRating)
        validateNote(request.note)

        userMediaRepository.update(userId, userMediaId, request)

        val newRating = request.userRating ?: return
        val oldRating = existing.userRating
        val mediaId = existing.mediaId

        if (oldRating == null) {
            mediaCatalogService.adjustUserRating(mediaId, ratingDelta = newRating, countDelta = 1)
            return
        }

        val delta = newRating - oldRating
        if (delta != 0.0) {
            mediaCatalogService.adjustUserRating(mediaId, ratingDelta = delta, countDelta = 0)
        }
    }

    fun updateStatus(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaStatusRequest
    ) {
        userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        userMediaRepository.updateStatus(userId, userMediaId, request.status)
    }

    fun updateFavourite(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaFavouriteRequest
    ) {
        userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        userMediaRepository.updateFavourite(userId, userMediaId, request.isFavourite)
    }

    fun updateFolders(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaFoldersRequest
    ) {
        userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        val uniqueFolderIds = request.folderIds.distinct()
        validateFolderIds(userId, uniqueFolderIds)
        userMediaRepository.updateFolders(userId, userMediaId, uniqueFolderIds)
    }

    fun delete(userId: String, userMediaId: String) {
        val existing = userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        userMediaRepository.delete(userId, userMediaId)

        existing.userRating?.let { rating ->
            mediaCatalogService.adjustUserRating(existing.mediaId, ratingDelta = -rating, countDelta = -1)
        }
    }

    private fun validateRating(rating: Double?) {
        if (rating != null && rating !in 0.0..10.0) {
            throw InvalidUserMediaRequestException("Your Rate must be from 0.0 to 10.0, not $rating")
        }
    }

    private fun validateNote(note: String?) {
        if (note != null && note.length > 400) {
            throw InvalidUserMediaRequestException(
                "The note must be less than 400 characters long. ${note.length} is too much"
            )
        }
    }

    private fun validateFolderIds(userId: String, folderIds: List<String>) {
        if (folderIds.isEmpty()) return
        val folderService = userFolderService
            ?: throw InvalidUserMediaRequestException("Folder support is not configured")
        folderService.validateFolderOwnership(userId, folderIds)
    }

    private fun loadMediaByIdIfNeeded(
        items: List<UserMediaItem>,
        mediaType: MediaType?,
        sortBy: UserMediaSortBy
    ): Map<String, MediaItem> {
        if (mediaType == null && sortBy != UserMediaSortBy.TITLE) return emptyMap()

        return mediaCatalogService
            .findByIds(items.map { it.mediaId })
            .associateBy { it.id }
    }

    private fun List<UserMediaItem>.sortedBy(
        sortBy: UserMediaSortBy,
        sortDirection: SortDirection,
        mediaById: Map<String, MediaItem>
    ): List<UserMediaItem> {
        val sorted = when (sortBy) {
            UserMediaSortBy.CREATED_AT -> sortedBy { it.createdAt }
            UserMediaSortBy.TITLE -> sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { item ->
                    mediaById[item.mediaId]?.title.orEmpty()
                }
            )
        }

        return when (sortDirection) {
            SortDirection.ASC -> sorted
            SortDirection.DESC -> sorted.reversed()
        }
    }
}
