package com.example.search.repository

interface SearchReadRepository {
    fun searchIds(query: String, limit: Int = 12, offset: Int = 0): List<String>
}
