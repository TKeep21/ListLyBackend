package com.example.search.service

import com.example.media.MediaCatalogService
import com.example.media.model.MediaItem
import com.example.search.exceptions.InvalidSearchRequestException
import com.example.search.exceptions.MeiliClientException
import com.example.search.exceptions.SearchRequestFailedException
import com.example.search.exceptions.SearchUnavailableException
import com.example.search.repository.SearchReadRepository
import org.slf4j.LoggerFactory

class MeiliMediaSearchServiceImpl(
    private val repository: SearchReadRepository, private val mediaCatalogService: MediaCatalogService
) : SearchService {
    private val log = LoggerFactory.getLogger(MeiliMediaSearchServiceImpl::class.java)

    override fun search(
        query: String,
        limit: Int,
        offset: Int
    ): List<MediaItem> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        if (limit !in 1..50) {
            throw InvalidSearchRequestException("Limit must be between 1 and 50")
        }

        if (offset < 0) {
            throw InvalidSearchRequestException("Offset must not be negative")
        }

        val safeQuery = normalizedQuery.take(150)

        return try {
            val ids = repository.searchIds(safeQuery,limit,offset)
            val items = mediaCatalogService.findByIds(ids)
            if (items.isEmpty()) return emptyList()
            items
        } catch (e: SearchRequestFailedException) {
            if (e.statusCode == 404 && e.responseBody.contains("index_not_found", ignoreCase = true)) {
                log.info("Search index not found for query='{}'. Returning empty result.", safeQuery)
                emptyList()
            } else {
                throw SearchUnavailableException("Search is unavailable", e)
            }
        } catch (e: MeiliClientException) {
            throw SearchUnavailableException("Search is unavailable", e)
        }
    }
}
