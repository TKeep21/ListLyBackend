package com.example.media

import com.example.media.dto.CreateMediaRequest
import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem
import com.example.search.exceptions.MeiliClientException
import com.example.search.service.SearchIndexService
import org.slf4j.LoggerFactory

class MediaCatalogService(
    private val mediaCatalogRepository: MediaCatalogRepository,
    private val searchIndexService: SearchIndexService
) {
    private val log = LoggerFactory.getLogger(MediaCatalogService::class.java);

    fun findAllByTitle(title: String): List<MediaItem> {
        require(title.isNotBlank()) { "title must not be blank" }
        return mediaCatalogRepository.findAllByTitle(title.trim())
    }

    fun findById(mediaId: String): MediaItem? {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        return mediaCatalogRepository.findById(mediaId)
    }

    fun create(request: CreateMediaRequest): MediaItem {
        validateCreateRequest(request)

        val mediaItem = MediaItem(
            title = request.title.trim(),
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            mediaType = request.mediaType,
            mediaStatus = request.mediaStatus,
            genres = request.genres.map { it.trim() }.filter { it.isNotBlank() },
            posterUrl = request.posterUrl?.trim()?.takeIf { it.isNotBlank() }
        )


        mediaCatalogRepository.save(mediaItem)

        val saved = findById(mediaItem.id)

        saved?.let {
            try {
                searchIndexService.indexMediaItem(it)
            } catch(e: MeiliClientException){
                log.warn("Media created in database but failed to index in search. mediaId={}", saved.id,e)
            }
        }



        return mediaItem
    }

    fun updateByAdmin(id: String, request: UpdateMediaRequest) {
        require(id.isNotBlank()) { "id must not be blank" }

        val mediaId = id.trim()
        mediaCatalogRepository.findById(mediaId) ?: throw MediaNotFoundException()

        validateUpdateRequest(request)
        mediaCatalogRepository.update(id, request)

        val updated = mediaCatalogRepository.findById(mediaId)
        if (updated == null){
            log.error("Media disappeared after update. mediaId={}", mediaId)
            throw MediaNotFoundException()
        }

        try {
            searchIndexService.indexMediaItem(updated)
            } catch (e: MeiliClientException){
                log.warn("Media Updated in mongo, but failed to update in MeiliIndex. mediaId = {}", mediaId,e )
            }
    }

    fun delete(mediaId: String) {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }

        val normalizedId = mediaId.trim()

        mediaCatalogRepository.findById(normalizedId) ?: throw MediaNotFoundException()
        mediaCatalogRepository.delete(normalizedId)

        try{
            searchIndexService.deleteFromIndex(normalizedId)
        } catch (e: MeiliClientException){
            log.warn("Media deleted from DB but failed to delete from index mediaId = {}",normalizedId,e)
        }
    }

    fun reindexSearchIndex() {
        try {
            searchIndexService.reindex()
        } catch (e: MeiliClientException) {
            log.warn("Reindex failed due to Meili client error", e)
            throw e
        }
    }

    private fun validateCreateRequest(request: CreateMediaRequest) {
        if (request.title.isBlank()) {
            throw InvalidMediaRequestException("Title must not be blank")
        }
    }

    private fun validateUpdateRequest(request: UpdateMediaRequest) {
        request.title?.let {
            if (it.isBlank()) {
                throw InvalidMediaRequestException("Title must not be blank")
            }
        }
    }

    fun adjustUserRating(mediaId: String, ratingDelta: Double, countDelta: Int) {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        mediaCatalogRepository.adjustUserRating(mediaId, ratingDelta, countDelta)
    }

    fun findByIds(ids:List<String>):List<MediaItem>{

        if (ids.isEmpty()) return emptyList()

        val normalizedIds = ids.map{it.trim()}.filter { it.isNotBlank() }

        if (normalizedIds.isEmpty()) return emptyList()


        val items = mediaCatalogRepository.findByIds(normalizedIds)

        if (items.size != normalizedIds.distinct().size){
            log.warn(
                "Search index may be out of sync with DB. requestedIds = {}, foundItems = {}",
                normalizedIds.distinct().size
                ,items.size
            )
            reindexSearchIndex();
        }


        return items

    }
}
