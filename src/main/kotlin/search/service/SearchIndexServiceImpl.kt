package com.example.search.service

import com.example.media.MediaCatalogRepository
import com.example.media.model.MediaItem
import com.example.search.mapper.toSearchDocument
import com.example.search.repository.SearchIndexRepository
import org.slf4j.LoggerFactory

class SearchIndexServiceImpl(
    private val mediaCatalogRepository: MediaCatalogRepository,
    private val searchIndexRepository: SearchIndexRepository,
) : SearchIndexService {
    private val log = LoggerFactory.getLogger(SearchIndexServiceImpl::class.java)

    override fun reindex() {
        val batchSize = 500
        var offset = 0

        while (true) {
            val batch = mediaCatalogRepository.findPage(limit = batchSize, offset = offset)
            if (batch.isEmpty()) break
            indexMediaItems(batch)
            offset += batch.size
        }

        log.info("Reindex completed. indexedItems={}", offset)
    }

    override fun indexMediaItem(mediaItem: MediaItem) {
        searchIndexRepository.upsertDocument(mediaItem.toSearchDocument())
    }

    override fun indexMediaItems(mediaItems: List<MediaItem>) {
        if (mediaItems.isEmpty()) return
        val documents = mediaItems.map { it.toSearchDocument() }
        searchIndexRepository.upsertDocuments(documents)
    }

    override fun deleteFromIndex(mediaId: String) {
        val normalizedId = mediaId.trim()
        if (normalizedId.isEmpty()) return
        searchIndexRepository.deleteDocument(normalizedId)
    }
    
}
