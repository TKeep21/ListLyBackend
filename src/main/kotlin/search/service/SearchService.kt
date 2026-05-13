package com.example.search.service

import com.example.media.model.MediaItem


interface SearchService {
    fun search (query: String, limit: Int = 12,offset:Int = 0) : List<MediaItem>

}