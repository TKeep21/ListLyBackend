package com.example.search.mapper

import com.example.media.model.MediaItem
import com.example.search.dto.model.SearchDocument


fun MediaItem.toSearchDocument() : SearchDocument {
    return SearchDocument(
        id = this.id,
        title = this.title
    )
}
