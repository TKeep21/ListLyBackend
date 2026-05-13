package com.example.search.service

import com.example.media.model.MediaItem

interface SearchIndexService {
    fun reindex()

    fun indexMediaItem(mediaItem: MediaItem)

    fun indexMediaItems(mediaItems: List<MediaItem>)

    fun deleteFromIndex(mediaId: String)
}
