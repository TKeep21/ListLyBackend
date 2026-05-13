package com.example.search.repository

import com.example.search.dto.model.SearchDocument

interface SearchIndexRepository {
    fun upsertDocuments(documents: List<SearchDocument>)

    fun upsertDocument(document: SearchDocument) {
        upsertDocuments(listOf(document))
    }

    fun deleteDocument(documentId: String)
}
